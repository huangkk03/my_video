# 阿里云盘批量下载功能优化计划

## 1. 需求描述

### 1.1 用户期望行为

用户在阿里云盘页面浏览文件时，可以：
- 勾选多个视频文件（支持全选）
- 点击"批量下载并转码"按钮
- 在弹窗中选择目标文件夹
- 确认后，后台自动排队逐个下载并转码

### 1.2 当前实现 vs 期望实现

| 维度 | 当前 | 期望 |
|------|------|------|
| 文件选择 | 单个文件操作 | 多选/全选 |
| 下载触发 | 逐个点击"下载并转码" | 一键批量下载 |
| 文件夹指定 | 不可选，默认刮削 | 下载时指定保存文件夹 |
| 后台处理 | 直接下载转码 | 进入队列，轮循处理 |

---

## 2. 现状分析

### 2.1 后端已具备能力

| 组件 | 状态 | 说明 |
|------|------|------|
| `download_queue` 表 | ✅ 已创建 | 存储下载任务 |
| `DownloadQueueService` | ✅ 已开发 | 单线程顺序下载，下载完成自动触发转码 |
| `DownloadQueueController` | ✅ 已开发 | `/api/download-queue/add` 接收单个任务 |
| `TranscodeService` | ✅ 已优化 | 使用 `transcodeExecutor` (core=1, max=1) 轮循转码 |

### 2.2 前端现状

| 文件 | 现状 |
|------|------|
| `AliyunDrive.vue` | 只有单个文件"下载并转码"按钮，无多选功能 |
| `MediaLibrary.vue` | 已有完整的多选+批量操作模式可参考 |

---

## 3. 优化方案

### 3.1 前端改造 (AliyunDrive.vue)

#### 3.1.1 数据结构新增

```typescript
// 新增选择状态
const selectedFiles = ref<Set<string>>(new Set())
const showBatchModal = ref(false)  // 批量下载弹窗
const targetFolderId = ref<number | null>(null)  // 目标文件夹

// 选中文件列表（用于显示）
const selectedFileList = computed(() => {
  return files.value.filter(f => selectedFiles.value.has(f.path))
})
```

#### 3.1.2 表格Checkbox改造

在文件列表表格第一列增加Checkbox：

```html
<table>
  <thead>
    <tr>
      <th class="w-12">
        <input type="checkbox" :checked="allSelected" @change="toggleSelectAll" />
      </th>
      <th>文件名</th>
      <th>大小</th>
      <th>类型</th>
      <th>操作</th>
    </tr>
  </thead>
  <tbody>
    <tr v-for="file in files" :key="file.path">
      <td>
        <input 
          type="checkbox" 
          :checked="selectedFiles.has(file.path)"
          :disabled="file.isFolder"
          @change="toggleFileSelection(file.path, $event)" 
        />
      </td>
      <td>{{ file.name }}</td>
      <td>{{ formatSize(file.size) }}</td>
      <td>{{ file.isFolder ? '文件夹' : '视频' }}</td>
      <td>
        <button v-if="!file.isFolder" @click="downloadFile(file)">下载并转码</button>
      </td>
    </tr>
  </tbody>
</table>
```

#### 3.1.3 批量操作栏

在文件列表上方添加：

```html
<!-- 批量操作栏 -->
<div v-if="selectedFiles.size > 0" class="batch-action-bar">
  <span>已选 {{ selectedFiles.size }} 项</span>
  <button @click="showBatchModal = true">批量下载并转码</button>
  <button @click="clearSelection">清空选择</button>
</div>
```

#### 3.1.4 文件夹选择弹窗

```html
<!-- 批量下载弹窗 -->
<div v-if="showBatchModal" class="modal">
  <div class="modal-content">
    <h3>批量下载设置</h3>
    <p>已选择 {{ selectedFiles.size }} 个文件</p>
    
    <label>保存到文件夹：</label>
    <select v-model="targetFolderId">
      <option :value="null">-- 不指定（使用根目录）--</option>
      <option v-for="folder in folders" :key="folder.id" :value="folder.id">
        {{ folder.name }}
      </option>
    </select>
    
    <div class="modal-actions">
      <button @click="showBatchModal = false">取消</button>
      <button @click="confirmBatchDownload">确认下载</button>
    </div>
  </div>
</div>
```

#### 3.1.5 核心方法实现

```typescript
// 切换单个文件选择
function toggleFileSelection(path: string, event: Event) {
  const checked = (event.target as HTMLInputElement).checked
  if (checked) {
    selectedFiles.value.add(path)
  } else {
    selectedFiles.value.delete(path)
  }
}

// 全选/取消全选
function toggleSelectAll(event: Event) {
  const checked = (event.target as HTMLInputElement).checked
  if (checked) {
    files.value
      .filter(f => !f.isFolder)
      .forEach(f => selectedFiles.value.add(f.path))
  } else {
    selectedFiles.value.clear()
  }
}

// 清空选择
function clearSelection() {
  selectedFiles.value.clear()
}

// 计算属性：是否全部选中
const allSelected = computed(() => {
  const videoFiles = files.value.filter(f => !f.isFolder)
  return videoFiles.length > 0 && 
         videoFiles.every(f => selectedFiles.value.has(f.path))
})

// 批量下载确认
async function confirmBatchDownload() {
  const filesToDownload = selectedFileList.value
  if (filesToDownload.length === 0) return
  
  showBatchModal.value = false
  
  // 循环调用批量添加API
  for (const file of filesToDownload) {
    await addToDownloadQueue(file, targetFolderId.value)
  }
  
  clearSelection()
  alert(`已添加 ${filesToDownload.length} 个文件到下载队列`)
}

// 调用后端API添加下载任务
async function addToDownloadQueue(file: CloudFile, folderId: number | null) {
  const params = new URLSearchParams()
  params.append('sourceUrl', file.path)
  params.append('sourceName', file.name)
  params.append('sourceSize', String(file.size || 0))
  params.append('folderId', String(folderId || ''))
  
  await fetch('/api/download-queue/add', {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: params.toString()
  })
}
```

---

### 3.2 后端改造

#### 3.2.1 DownloadQueue Entity 新增字段

```java
// 新增字段：目标文件夹ID
@Column(name = "folder_id")
private Long folderId;
```

#### 3.2.2 DownloadQueueController 改造

```java
@PostMapping("/add")
public ResponseEntity<?> addToQueue(
    @RequestParam String sourceUrl,
    @RequestParam String sourceName,
    @RequestParam(required = false, defaultValue = "0") Long sourceSize,
    @RequestParam(required = false, defaultValue = "0") int priority,
    @RequestParam(required = false) Long folderId) {  // 新增参数
    
    DownloadQueue task = downloadQueueService.addToQueue(
        sourceUrl, sourceName, sourceSize, priority, folderId);
    
    Map<String, Object> result = new HashMap<>();
    result.put("success", true);
    result.put("taskId", task.getTaskId());
    return ResponseEntity.ok(result);
}
```

#### 3.2.3 DownloadQueueService 改造

```java
public DownloadQueue addToQueue(String sourceUrl, String sourceName, 
                                Long sourceSize, int priority, Long folderId) {
    DownloadQueue queue = new DownloadQueue();
    queue.setTaskId(UUID.randomUUID().toString());
    queue.setSourceUrl(sourceUrl);
    queue.setSourceName(sourceName);
    queue.setSourceSize(sourceSize);
    queue.setPriority(priority);
    queue.setFolderId(folderId);  // 设置文件夹ID
    queue.setStatus("pending");
    return downloadQueueRepository.save(queue);
}
```

#### 3.2.4 下载完成后关联视频文件夹

在 `DownloadQueueService.processTask()` 中，下载完成触发转码时：

```java
// 下载完成，准备触发转码
task.setStatus("transcoding");
downloadQueueRepository.save(task);

// 触发转码，传递 folderId
transcodeService.startTranscodeWithFolder(targetPath.toString(), task.getFolderId());
```

---

## 4. 数据库变更

### 4.1 download_queue 表新增字段

```sql
ALTER TABLE download_queue ADD COLUMN folder_id BIGINT COMMENT '目标文件夹ID';
```

### 4.2 video_metadata 表新增字段（可选）

如果需要记录下载来源：

```sql
ALTER TABLE video_metadata ADD COLUMN download_queue_id BIGINT COMMENT '关联下载队列ID';
```

---

## 5. API 设计

### 5.1 批量添加下载任务

**接口**：`POST /api/download-queue/add`

**参数**：
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sourceUrl | String | ✅ | 源文件路径（AList路径） |
| sourceName | String | ✅ | 文件名 |
| sourceSize | Long | ❌ | 文件大小 |
| priority | int | ❌ | 优先级（默认0） |
| folderId | Long | ❌ | 目标文件夹ID |

**响应**：
```json
{
  "success": true,
  "taskId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 5.2 获取文件夹列表

**接口**：`GET /api/folders`

**响应**：
```json
[
  {"id": 1, "name": "电影"},
  {"id": 2, "name": "电视剧"}
]
```

---

## 6. 交互流程

```
┌─────────────────────────────────────────────────────────────┐
│                    用户在阿里云盘页面                          │
│              浏览文件，勾选 3 个视频文件                        │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                 点击"批量下载并转码"                          │
│            弹出选择文件夹 Modal                               │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              用户选择"电影"文件夹，点击确认                      │
│         前端循环调用 3 次 POST /api/download-queue/add        │
│              后端返回 3 个 taskId                             │
└─────────────────────────┬───────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              提示"已添加 3 个文件到下载队列"                    │
│         用户可关闭页面，后台自动轮循处理                        │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                    DownloadQueueService                       │
│  轮循取出 pending 任务 → 下载 → 转码 → 完成                     │
│                                                             │
│  [任务1] pending → downloading → transcoding → completed     │
│  [任务2] pending (等待)                                      │
│  [任务3] pending (等待)                                      │
└─────────────────────────────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────┐
│              转码完成后，视频保存到指定文件夹                    │
│           可在"媒体库管理"页面查看和管理                        │
└─────────────────────────────────────────────────────────────┘
```

---

## 7. 实施步骤

### 阶段 1：后端增强
- [ ] `download_queue` 表新增 `folder_id` 字段
- [ ] `DownloadQueue` Entity 新增 `folderId` 属性
- [ ] `DownloadQueueController.addToQueue()` 新增 `folderId` 参数
- [ ] `DownloadQueueService.addToQueue()` 新增 `folderId` 参数
- [ ] `TranscodeService.startTranscode()` 支持传入 `folderId`

### 阶段 2：前端改造
- [ ] `AliyunDrive.vue` 新增 `selectedFiles` 状态
- [ ] 表格第一列增加 Checkbox 列
- [ ] 实现 `toggleFileSelection()` 方法
- [ ] 实现 `toggleSelectAll()` 方法
- [ ] 添加批量操作栏 UI
- [ ] 添加文件夹选择 Modal
- [ ] 实现 `confirmBatchDownload()` 方法
- [ ] 获取文件夹列表 API 调用

### 阶段 3：测试验证
- [ ] 单文件下载测试
- [ ] 多文件批量下载测试
- [ ] 文件夹指定验证
- [ ] 队列轮循验证

---

## 8. 注意事项

1. **文件大小限制**：建议前端单次批量不超过 50 个文件，避免请求超时
2. **AList 限流**：后台单线程下载，不会触发 AList 限流
3. **磁盘空间**：批量下载前检查 `/data/downloads/incoming` 剩余空间
4. **异常处理**：某个文件下载失败不影响其他文件，继续处理队列

---

## 9. 参考文件

- 前端参考：`/data/video/my_video/frontend/src/views/admin/MediaLibrary.vue`（多选模式）
- 前端改造：`/data/video/my_video/frontend/src/views/admin/AliyunDrive.vue`
- 后端实体：`/data/video/my_video/backend/src/main/java/com/video/entity/DownloadQueue.java`
- 后端服务：`/data/video/my_video/backend/src/main/java/com/video/service/DownloadQueueService.java`
- 后端控制器：`/data/video/my_video/backend/src/main/java/com/video/controller/DownloadQueueController.java`
