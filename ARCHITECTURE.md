# MKV Video Platform - 架构设计文档

## 1. 系统架构图

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Vue3 Frontend │────▶│  Spring Boot    │────▶│  FFmpeg Transcode│
│   (ArtPlayer)   │     │   REST API      │     │   Service        │
└─────────────────┘     └─────────────────┘     └─────────────────┘
        │                       │                       │
        │                       ▼                       ▼
        │               ┌──────────────┐       ┌──────────────┐
        │               │   MySQL DB   │       │  HLS Storage │
        │               │              │       │  /data/videos│
        │               └──────────────┘       └──────────────┘
        │
        ▼
┌─────────────────┐
│   HLS Playback  │
│   (.m3u8 + .ts) │
└─────────────────┘
```

## 2. 核心约束

### 2.1 视频处理约束
- **禁止**: 直接在前端播放 MKV（浏览器不支持）
- **必须**: 后端采用 FFmpeg 进行 `MKV -> HLS (.m3u8 + .ts)` 异步转码
- **存储**: 切片必须存储在结构化目录 `/data/videos/{uuid}/index.m3u8`
- **协议**: 后端控制器必须支持 `HTTP 206 Partial Content`

### 2.2 目录结构
```
/data/videos/
├── {uuid}/
│   ├── original/          # 原始视频存储
│   │   └── video.mkv
│   ├── index.m3u8          # HLS 播放列表
│   ├── index0.ts           # 视频切片
│   ├── index1.ts
│   └── ...
│   └── thumbnail.jpg       # 缩略图
```

## 3. 后端架构设计

### 3.1 技术栈
- Spring Boot 3.x
- Java 25
- Maven/Gradle

### 3.2 核心模块

#### 3.2.1 VideoController (REST API)
- `POST /api/videos/upload` - 上传视频
- `GET /api/videos` - 获取视频列表
- `GET /api/videos/{uuid}` - 获取视频详情
- `GET /api/videos/{uuid}/stream` - 流式播放（支持 206）
- `PUT /api/videos/{uuid}/progress` - 更新播放进度
- `DELETE /api/videos/{uuid}` - 删除视频

#### 3.2.2 TranscodeService (转码服务)
- 使用 `@Async` 注解异步执行
- FFmpeg 命令模板:
  ```
  ffmpeg -i {input} -codec:v libx264 -codec:a aac -f hls -hls_time 10 -hls_list_size 0 {output}/index.m3u8
  ```
- 硬件加速（可选）: ARM/RK3588 使用 `h264_v4l2m2m` 编码器

#### 3.2.3 VideoService (业务逻辑)
- 视频元数据管理
- 播放进度同步
- 状态流转管理

### 3.3 数据库设计
- 参见 `DB_SCHEMA.sql`
- 核心表: video_metadata, transcode_tasks, playback_progress

## 4. 前端架构设计

### 4.1 技术栈
- Vue 3.5+
- Vite
- ArtPlayer + Hls.js
- TailwindCSS

### 4.2 页面结构

```
src/
├── views/
│   ├── HomeView.vue        # 视频列表页
│   ├── UploadView.vue      # 上传页
│   └── PlayerView.vue      # 播放页
├── components/
│   ├── VideoCard.vue       # 视频卡片
│   ├── VideoPlayer.vue     # 播放器组件
│   ├── UploadZone.vue      # 上传区域
│   └── SkeletonCard.vue    # 骨架屏
├── composables/
│   ├── useVideoList.ts     # 视频列表逻辑
│   ├── useUpload.ts        # 上传逻辑
│   └── usePlayer.ts        # 播放器逻辑
└── api/
    └── video.ts            # API 调用
```

### 4.3 播放器实现要点
- 必须在 `onUnmounted` 中调用 `art.destroy()` 和 `hls.destroy()`
- 实现 `timeupdate` 监听，每 5 秒同步一次进度到后端
- 支持 HTTP 206 Partial Content 流式播放

## 5. API 接口设计

### 5.1 视频上传
```
POST /api/videos/upload
Content-Type: multipart/form-data

Request: 
  - file: Binary (视频文件)
  - title: String (可选)

Response:
  {
    "uuid": "xxx",
    "status": "pending",
    "message": "上传成功，转码任务已创建"
  }
```

### 5.2 视频列表
```
GET /api/videos?page=1&size=20

Response:
  {
    "content": [
      {
        "uuid": "xxx",
        "title": "视频标题",
        "thumbnailPath": "/api/videos/xxx/thumbnail",
        "duration": 3600000,
        "status": "completed",
        "currentPosition": 120000
      }
    ],
    "total": 100,
    "page": 1
  }
```

### 5.3 视频播放
```
GET /api/videos/{uuid}/stream
Range: bytes=0-

Response: 
  Content-Type: application/vnd.apple.mpegurl
  支持 206 分片传输
```

### 5.4 播放进度
```
PUT /api/videos/{uuid}/progress
Content-Type: application/json

Request:
  {
    "position": 120000  // 毫秒
  }

Response:
  {
    "success": true
  }
```

## 6. 部署架构

### 6.1 Nginx 配置
```nginx
server {
    # 前端静态资源
    location / {
        root /var/www/html;
        try_files $uri $uri/ /index.html;
    }
    
    # HLS 视频流
    location /data/videos/ {
        alias /data/videos/;
        add_header Access-Control-Allow-Origin *;
        
        # CORS for HLS
        add_header Access-Control-Allow-Headers "Range";
        add_header Access-Control-Allow-Headers "Content-Type";
    }
    
    # API 代理
    location /api/ {
        proxy_pass http://localhost:8080/api/;
    }
}
```

### 6.2 Docker 部署
- 多阶段构建，包含 FFmpeg 运行库
- 参见阶段 4 的 Dockerfile

## 7. 安全考虑

- 文件上传校验（大小、类型）
- 路径遍历防护
- 转码进程隔离
- 存储配额限制