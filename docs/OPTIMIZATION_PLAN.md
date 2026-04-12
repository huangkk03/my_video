# 批量下载 + 轮循转码优化计划

## 1. 现状分析

### 1.1 当前架构问题

| 问题 | 当前实现 | 影响 |
|------|---------|------|
| 无下载队列 | `@Async downloadAndProcess()` 直接下载 | 多文件同时下载占满带宽，AList 连接超时 |
| 无转码队列 | `@Async transcodeVideo()` 直接转码 | 多视频同时转码导致 CPU/内存爆满 |
| 无并发控制 | 使用 Spring 默认 `SimpleAsyncTaskExecutor`（无限制） | 系统不稳定，FFmpeg 进程过多 |
| 缺乏持久化 | 队列任务在内存中，重启后丢失 | 任务进度无法恢复 |

### 1.2 当前转码流程

```
用户上传/云端导入
      │
      ▼
┌─────────────────┐
│  TranscodeService.transcodeVideo()
│  @Async (无任何限制)
└─────────────────┘
      │
      ▼
  直接执行 FFmpeg，无队列控制
```

### 1.3 技能文档约束

根据 `mkv-video-expert` 技能文档：
- **阶段 11.2**：`transcode_thread_count` 配置限制并行转码线程数
- **阶段 11.3**：HLS 切片策略优化
- **阶段 21**：强制构建与验证协议

---

## 2. 优化目标

1. **批量下载**：支持多文件选择，后台顺序下载，不阻塞前端
2. **轮循转码**：同一时间只允许 1 个转码任务，排队执行
3. **资源可控**：CPU/内存占用稳定，系统不死机
4. **进度可查**：前端可查看下载队列和转码队列状态
5. **故障恢复**：服务重启后任务自动恢复

---

## 3. 优化架构

### 3.1 整体流程

```
┌─────────────────────────────────────────────────────────────┐
│                      用户批量选择文件                          │
│                 (阿里云盘 / AList 文件列表)                    │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                   DownloadQueueService                         │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           download_queue 表 (持久化)                    │   │
│  │  id, source_url, source_name, status, priority,       │   │
│  │  progress, created_at, started_at, completed_at       │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                               │
│  • 单线程顺序下载 (避免 AList 连接超时)                        │
│  • 下载完成 → 自动触发转码任务 → 进入转码队列                    │
│  • 服务重启后自动恢复 pending 状态的任务                        │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                   TranscodeQueueService                       │
│  ┌─────────────────────────────────────────────────────┐   │
│  │     ThreadPoolTaskExecutor (core=1, max=1)            │   │
│  │     同一时间只允许 1 个转码任务，队列等待               │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                               │
│  • 按创建时间 FIFO 顺序执行                                   │
│  • 转码完成 → 更新 video_metadata 状态                       │
│  • 失败自动重试 3 次                                         │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                      前端轮询状态                             │
│   GET /api/download-queue → 查看下载进度                       │
│   GET /api/transcode-queue/stats → 转码队列位置               │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 核心组件

| 组件 | 职责 |
|------|------|
| `DownloadQueueService` | 管理下载队列，单线程顺序下载，下载完成触发转码 |
| `TranscodeQueueService` | 管理转码线程池（大小=1），任务排队执行 |
| `DownloadQueue` (Entity) | 下载队列实体，对应 `download_queue` 表 |
| `TranscodeTask` (Entity) | 已有，复用 `transcode_tasks` 表 |
| `DownloadQueueController` | 下载队列 REST API |
| `TranscodeQueueController` | 转码队列统计 API |

---

## 4. 数据库设计

### 4.1 新增表：download_queue

```sql
CREATE TABLE IF NOT EXISTS download_queue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL UNIQUE COMMENT '任务唯一标识',
    source_url VARCHAR(2000) NOT NULL COMMENT '源文件 URL (AList raw URL)',
    source_name VARCHAR(500) NOT NULL COMMENT '源文件名',
    source_size BIGINT DEFAULT 0 COMMENT '源文件大小',
    save_path VARCHAR(1000) COMMENT '本地保存路径',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending, downloading, transcoding, completed, failed',
    progress INT DEFAULT 0 COMMENT '下载进度 0-100',
    error_message TEXT COMMENT '错误信息',
    priority INT DEFAULT 0 COMMENT '优先级 (数字越小优先级越高)',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    INDEX idx_status (status),
    INDEX idx_priority (priority),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 4.2 修改表：transcode_tasks

建议新增字段：
```sql
ALTER TABLE transcode_tasks ADD COLUMN download_queue_id BIGINT COMMENT '关联下载队列 ID';
```

---

## 5. API 设计

### 5.1 下载队列 API

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/download-queue` | 获取下载队列列表 |
| GET | `/api/download-queue/{taskId}` | 获取单个任务详情 |
| POST | `/api/download-queue/add` | 批量添加下载任务 |
| DELETE | `/api/download-queue/{taskId}` | 取消下载任务 |
| POST | `/api/download-queue/{taskId}/retry` | 重试失败任务 |

### 5.2 转码队列 API

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/transcode-queue/stats` | 获取转码队列统计（等待数量、当前任务） |
| GET | `/api/transcode-queue/pending` | 获取等待中的转码任务列表 |

---

## 6. 配置参数

### 6.1 application.yml 新增配置

```yaml
video:
  download:
    thread-count: 1           # 下载线程数（建议单线程避免 AList 超时）
    retry-max: 3             # 下载失败最大重试次数
  transcode:
    thread-count: 1          # 转码线程数（核心参数，建议为 1）
    queue-enabled: true      # 启用队列模式
    retry-max: 3             # 转码失败最大重试次数
```

### 6.2 系统配置表新增项

```sql
INSERT INTO system_config (config_key, config_value, description) VALUES
('transcode_thread_count', '1', '转码并发线程数'),
('transcode_queue_enabled', 'true', '是否启用转码队列模式'),
('download_thread_count', '1', '下载并发线程数');
```

---

## 7. 实施步骤

### 阶段 1：数据库扩展
- [ ] 新增 `download_queue` 表
- [ ] 修改 `transcode_tasks` 表（可选）
- [ ] 更新 `system_config` 表

### 阶段 2：下载队列服务
- [ ] 创建 `DownloadQueue` Entity
- [ ] 创建 `DownloadQueueRepository`
- [ ] 创建 `DownloadQueueService`
  - 单线程顺序下载
  - 下载完成自动触发转码
  - 启动时恢复 pending 任务

### 阶段 3：转码队列控制
- [ ] 创建 `TranscodeConfig` 配置类
- [ ] 配置 `ThreadPoolTaskExecutor` Bean (core=1, max=1)
- [ ] 修改 `TranscodeService.transcodeVideo()` 使用自定义 Executor
- [ ] 添加 `@Async("transcodeExecutor")` 注解

### 阶段 4：API 端点
- [ ] 创建 `DownloadQueueController`
- [ ] 创建 `TranscodeQueueController`

### 阶段 5：前端集成
- [ ] 管理页面显示下载队列
- [ ] 显示转码队列状态和位置

### 阶段 6：测试验证
- [ ] 验证多文件批量下载顺序执行
- [ ] 验证转码队列同一时间只有 1 个任务
- [ ] 验证服务重启后任务恢复

---

## 8. 关键代码片段

### 8.1 转码线程池配置

```java
@Bean(name = "transcodeExecutor")
public ThreadPoolTaskExecutor transcodeExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(1);
    executor.setMaxPoolSize(1);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("transcode-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    executor.initialize();
    return executor;
}
```

### 8.2 下载队列服务伪代码

```java
@Async("downloadExecutor")
public void processDownloadQueue() {
    while (true) {
        DownloadQueue task = downloadQueueRepository
            .findTopByStatusOrderByPriorityAscCreatedAtAsc("pending");
        
        if (task == null) {
            Thread.sleep(5000); // 无任务时休眠
            continue;
        }
        
        task.setStatus("downloading");
        task.setStartedAt(LocalDateTime.now());
        downloadQueueRepository.save(task);
        
        try {
            // 执行下载
            downloadFile(task);
            
            task.setStatus("transcoding");
            task.setProgress(100);
            downloadQueueRepository.save(task);
            
            // 触发转码
            transcodeService.startTranscode(task.getSavePath());
            
            task.setStatus("completed");
            task.setCompletedAt(LocalDateTime.now());
        } catch (Exception e) {
            task.setStatus("failed");
            task.setErrorMessage(e.getMessage());
        }
        
        downloadQueueRepository.save(task);
    }
}
```

---

## 9. 注意事项

1. **AList 限流**：单线程下载避免触发 AList 请求频率限制
2. **FFmpeg 内存**：HLS 转码建议限制 `-threads 2`，避免单任务占用过多 CPU
3. **磁盘空间**：下载前检查磁盘空间，避免满载
4. **大文件处理**：超过 10GB 的文件建议分段下载或使用 aria2

---

## 10. 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| 下载速度慢 | 单线程下载大文件耗时长 | 用户预期管理，可后台执行 |
| 转码队列堆积 | 大量视频排队等待 | 监控告警，动态调整线程数 |
| 服务重启丢任务 | pending 任务丢失 | 启动时自动恢复 pending 状态任务 |
| AList Token 过期 | 下载中断 | Token 自动刷新机制 |

---

## 11. 参考文档

- `mkv-video-expert` 技能文档 - 阶段 11（转码优化）、阶段 21（构建验证）
- Spring Boot `@Async` + `ThreadPoolTaskExecutor` 配置
- FFmpeg HLS 转码参数优化
