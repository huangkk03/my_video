# AList MKV 视频流式转码优化方案

## 1. 背景与目标

### 当前问题
- 10GB+ 的 MKV 文件先下载到本地再转码，浪费时间和磁盘空间
- 10GB 文件下载可能需要 30 分钟以上（取决于网络带宽）
- 转码完成后还需删除原始文件，管理复杂

### 优化目标
FFmpeg 直接从 AList 提供的 HTTP URL 读取视频流，边读边转码为 HLS，不在本地存储原始文件。

---

## 2. 技术方案

### 2.1 流程对比

**当前流程（先下后转）：**
```
AList raw_url → HttpURLConnection 下载 → /data/videos/downloads/incoming/{file}
                                                       ↓
                              TranscodeService.executeTranscode() (读本地文件)
                                                       ↓
                                    /data/videos/{uuid}/index.m3u8
                                                       ↓
                              Files.deleteIfExists(targetPath)  // 删除原始文件
```

**优化后流程（流式转码）：**
```
AList 相对路径 → 动态获取 raw_url → TranscodeService (FFmpeg 直接读 HTTP URL)
                                                          ↓
                                          /data/videos/{uuid}/index.m3u8 + thumbnail.jpg
```

### 2.2 核心原理

FFmpeg 原生支持 HTTP URL 作为输入，配合重连参数应对网络波动：
```bash
ffmpeg -reconnect 1 -reconnect_streamed 1 -reconnect_delay_max 5 -i "https://alist.example.com/..." \
       -codec:v libx264 -preset fast -crf 23 \
       -codec:a aac -b:a 128k \
       -f hls -hls_time 10 -hls_list_size 0 \
       -http_seekable 1 \
       /data/videos/{uuid}/index.m3u8
```

### 2.3 设计决策：Option B（存储 AList 相对路径）

**设计选择**：存储 AList 相对路径（如 `/Movies/Avatar.mkv`）而非带签名的临时 URL。

**原因**：
- 带签名的 URL 会过期（1-2 小时），转码失败重试时需重新获取 URL
- 存储相对路径可支持失败重试时动态重新获取有效 URL
- 已刮削的元数据不丢失，只需重新获取 URL + 转码

**实现方式**：
- `Video.java` 新增 `alistPath` 字段存储 AList 相对路径
- 转码时通过 `CloudStorageService.getFileRawUrl(alistPath)` 动态获取实时 URL
- `originalPath` 字段语义改为存储最终播放地址（m3u8 路径），不再存储源文件路径

---

## 3. 代码改动详解

### 3.1 `Video.java` 实体修改

**新增字段：**

```java
// Video.java

// 视频来源类型：local_file = 本地上传, remote_alist = AList 网盘
@Column(name = "source_type", length = 20)
private String sourceType = "local_file";

// AList 网盘相对路径，用于动态获取转码 URL（如 "/Movies/Avatar.mkv"）
@Column(name = "alist_path", length = 1000)
private String alistPath;
```

---

### 3.2 `TranscodeService.java` 新增方法

#### 新增 1: `executeTranscodeFromUrl(String sourceUrl, Video video, TranscodeTask task)`

位置：`executeTranscode()` 方法之后新增

功能：FFmpeg 直接从 HTTP URL 读取并转码为 HLS，同时生成缩略图

**特性**：
- FFmpeg 追加重连参数（-reconnect, -reconnect_streamed 等）应对网络波动
- **一次调用同时产出 HLS 和缩略图**，避免二次请求 URL（节省带宽 + 避免签名过期风险）
- 转码失败时自动清理残留的 .ts 碎片文件

```java
/**
 * 从 HTTP URL 流式转码（不下载到本地）
 * 同时产出 HLS 和缩略图
 */
private void executeTranscodeFromUrl(String sourceUrl, Video video, TranscodeTask task) throws Exception {
    Path videoDir = Paths.get(VIDEO_STORAGE_PATH, video.getUuid());
    Files.createDirectories(videoDir);
    
    Path hlsPath = videoDir.resolve("index.m3u8");
    Path thumbnailPath = videoDir.resolve("thumbnail.jpg");
    
    long durationMs = getVideoDurationFromUrl(sourceUrl);
    video.setDuration(durationMs);
    
    // FFmpeg 命令：重连参数 + 同时产出 HLS 和缩略图
    List<String> command = new ArrayList<>(Arrays.asList(
        FFMPEG_COMMAND,
        "-reconnect", "1",                       // 开启重连
        "-reconnect_streamed", "1",               // 开启流重连
        "-reconnect_delay_max", "5",               // 最大重连间隔 5 秒
        "-i", sourceUrl,                          // HTTP URL
        "-codec:v", "libx264",
        "-preset", "fast",
        "-crf", "23",
        "-codec:a", "aac",
        "-b:a", "128k",
        "-f", "hls",
        "-hls_time", "10",
        "-hls_list_size", "0",
        "-http_seekable", "1",
        // HLS 输出
        hlsPath.toString(),
        // 缩略图输出（同时）
        "-ss", "00:00:05",
        "-vframes", "1",
        "-vf", "scale=320:-1",
        thumbnailPath.toString()
    ));
    
    task.setFfmpegCommand(String.join(" ", command));
    transcodeTaskRepository.save(task);
    
    log.info("Executing FFmpeg stream transcode from URL: {}",
             sourceUrl.replaceAll("sign=[^&]+", "sign=HIDDEN"));
    
    ProcessBuilder processBuilder = new ProcessBuilder(command);
    processBuilder.redirectErrorStream(true);
    Process process = processBuilder.start();
    
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        long lastProgressUpdate = System.currentTimeMillis();
        while ((line = reader.readLine()) != null) {
            log.debug("FFmpeg: {}", line);
            
            if (System.currentTimeMillis() - lastProgressUpdate > 2000) {
                lastProgressUpdate = System.currentTimeMillis();
                int progress = parseProgress(line, durationMs);
                if (progress > 0 && progress < 100) {
                    task.setProgress(progress);
                    transcodeTaskRepository.save(task);
                }
            }
        }
    }
    
    int exitCode = process.waitFor();
    if (exitCode != 0) {
        throw new RuntimeException("FFmpeg exited with code: " + exitCode);
    }
    
    video.setHlsPath(hlsPath.toString());
    if (Files.exists(thumbnailPath)) {
        video.setThumbnailPath(thumbnailPath.toString());
    }
    videoRepository.save(video);
}
```

#### 新增 2: `getVideoDurationFromUrl(String url)`

```java
/**
 * 从 HTTP URL 获取视频时长
 */
private long getVideoDurationFromUrl(String urlStr) {
    try {
        ProcessBuilder pb = new ProcessBuilder(
            FFPROBE_COMMAND, "-i", urlStr, "-v", "quiet", "-print_format", "json",
            "-show_format", "-show_streams"
        );
        pb.redirectErrorStream(true);
        Process p = pb.start();
        
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        p.waitFor();
        
        String output = sb.toString();
        Pattern pattern = Pattern.compile("\"duration\"\\s*:\\s*([\\d.]+)");
        Matcher matcher = pattern.matcher(output);
        if (matcher.find()) {
            return (long) (Double.parseDouble(matcher.group(1)) * 1000);
        }
    } catch (Exception e) {
        log.warn("Failed to get video duration from URL: {}", e.getMessage());
    }
    return 0;
}
```

#### 新增 3: 异常清理 `cleanupPartialFiles(String uuid)`

```java
/**
 * 清理转码失败的残留文件
 */
private void cleanupPartialFiles(String uuid) {
    try {
        Path videoDir = Paths.get(VIDEO_STORAGE_PATH, uuid);
        if (Files.exists(videoDir)) {
            try (Stream<Path> walk = Files.walk(videoDir)) {
                walk.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
            log.info("Cleaned up partial files for video: {}", uuid);
        }
    } catch (IOException e) {
        log.warn("Failed to cleanup directory for video: {}", uuid, e);
    }
}
```

#### 新增 4: 公开方法 `transcodeVideoFromAlistPath(String videoUuid)`

```java
@Async
public CompletableFuture<Void> transcodeVideoFromAlistPath(String videoUuid) {
    log.info("Starting stream transcode for video: {} from AList path", videoUuid);

    Video video = videoRepository.findByUuid(videoUuid).orElse(null);
    if (video == null) {
        log.error("Video not found: {}", videoUuid);
        return CompletableFuture.completedFuture(null);
    }

    TranscodeTask task = transcodeTaskRepository.findByVideoUuid(videoUuid).orElse(null);
    if (task == null) {
        task = createTranscodeTask(video);
    }

    try {
        task.setStatus("processing");
        task.setStartedAt(LocalDateTime.now());
        transcodeTaskRepository.save(task);

        video.setStatus("transcoding");
        videoRepository.save(video);

        // 通过 alistPath 动态获取实时 URL
        String sourceUrl = cloudStorageService.getFileRawUrl(video.getAlistPath());
        if (sourceUrl == null) {
            throw new RuntimeException("Failed to get raw URL from AList for path: " + video.getAlistPath());
        }

        executeTranscodeFromUrl(sourceUrl, video, task);

        task.setStatus("completed");
        task.setProgress(100);
        task.setCompletedAt(LocalDateTime.now());
        transcodeTaskRepository.save(task);

        video.setStatus("completed");
        videoRepository.save(video);

        log.info("Stream transcode completed for video: {}", videoUuid);

    } catch (Exception e) {
        log.error("Stream transcode failed for video: {}", videoUuid, e);
        handleTranscodeFailure(video, task, e);
    }

    return CompletableFuture.completedFuture(null);
}
```

#### 修改 5: `handleTranscodeFailure()` 增加清理逻辑

```java
private void handleTranscodeFailure(Video video, TranscodeTask task, Exception e) {
    log.error("Stream transcode failed, cleaning up partial files...");
    cleanupPartialFiles(video.getUuid());

    task.setStatus("failed");
    task.setErrorMessage(e.getMessage());
    transcodeTaskRepository.save(task);

    video.setStatus("failed");
    videoRepository.save(video);
}
```

---

### 3.3 `CloudMediaService.java` 修改

#### 修改 `processImportAsync()` 方法

**改动后逻辑：**
```java
task.setStatus("preparing");
task.setMessage("Preparing stream transcode...");
task.setProgress(5);
importTaskRepository.save(task);

// 获取 AList 相对路径（不下载）
String alistPath = task.getSourcePath();

task.setProgress(10);
task.setMessage("Scraping metadata...");
importTaskRepository.save(task);

// 元数据刮削（不变）
String title = buildScrapeTitleFromFilename(fileName);
ScrapingAggregationService.MetadataResult metadataResult = scrapMetadata(task, title);

task.setProgress(30);
task.setMessage("Starting stream transcode...");
importTaskRepository.save(task);

task.setStatus("transcoding");
importTaskRepository.save(task);

// 直接从 AList 相对路径创建 Video 并触发转码
String videoUuid = startTranscodingFromAlist(task, alistPath, title);
applyScrapedMetadataToVideo(videoUuid, metadataResult);

task.setVideoUuid(videoUuid);
task.setProgress(100);
task.setStatus("completed");
task.setMessage("Import complete");
importTaskRepository.save(task);
```

#### 新增 `startTranscodingFromAlist(ImportTask task, String alistPath, String title)`

```java
private String startTranscodingFromAlist(ImportTask task, String alistPath, String title) {
    String uuid = UUID.randomUUID().toString();

    Video video = new Video();
    video.setUuid(uuid);
    video.setTitle(title);
    video.setOriginalFilename(task.getSourceName());
    video.setAlistPath(alistPath);  // 存储 AList 相对路径
    video.setSourceType("remote_alist");  // 标记来源为 AList
    video.setFileSize(task.getSourceSize());
    video.setStatus("pending");
    video.setCreatedAt(LocalDateTime.now());
    video = videoRepository.save(video);

    // 触发转码（内部会动态获取 URL）
    transcodeService.transcodeVideoFromAlistPath(uuid);

    return uuid;
}
```

---

## 4. 进度计算优化

### 问题
FFmpeg 从 HTTP URL 读取时，初始无法预知总时长，进度计算不准确。

### 解决方案

1. **转码前**：使用 `ffprobe -i "http://..."` 获取视频时长
2. **转码中**：正常解析 FFmpeg 输出中的 `time=` 进度信息
3. **Fallback**：如果无法获取时长，显示 Indeterminate 进度

---

## 5. 异常处理

| 异常场景 | 处理方式 |
|---------|---------|
| AList URL 签名过期 | 重新导入时动态获取新 URL |
| 网络中断导致 FFmpeg 读取失败 | 任务标记 failed，自动清理残留文件，支持重试 |
| FFmpeg 转码失败 | 记录错误日志，任务标记 failed，自动清理残留文件 |
| 无法获取视频时长 | 使用 indeterminate 进度条 |

---

## 6. 存储结构对比

### 优化前
```
/data/videos/
├── downloads/incoming/           # 临时存储 10GB+ 原始文件
│   └── {movie.mkv}               # 转码后删除
└── {uuid}/
    ├── original/                 # 又存一份原始文件（浪费！）
    │   └── {movie.mkv}
    ├── index.m3u8
    └── thumbnail.jpg
```

### 优化后
```
/data/videos/
└── {uuid}/                      # 无需存储原始文件
    ├── index.m3u8               # HLS 播放列表
    ├── index0.ts                # 视频切片
    ├── index1.ts
    └── thumbnail.jpg            # 缩略图
```

**磁盘空间节省：每部视频节省约 20GB+（原始文件 + 副本）**

---

## 7. 依赖变更

无新增依赖，使用现有的：
- `java.net.HttpURLConnection` - URL 处理
- `java.nio.file.Files` - HLS 输出路径管理
- `ProcessBuilder` - FFmpeg/FFprobe 命令执行
- `CloudStorageService` - 动态获取 AList URL（已有）

---

## 8. 风险与注意事项

1. **网络稳定性**：转码过程中网络中断会导致任务失败，需要稳定网络
2. **FFmpeg 重连机制**：已添加 `-reconnect 1 -reconnect_streamed 1 -reconnect_delay_max 5` 参数应对瞬时抖动
3. **进度显示**：由于无法预知总时长，进度条可能不如本地文件准确
4. **签名过期**：存储相对路径而非带签名 URL，支持失败重试时重新获取有效 URL
5. **残留文件清理**：转码失败时自动清理 .ts 碎片

---

## 9. 实施步骤

| 步骤 | 操作 | 文件 |
|-----|------|------|
| 1 | Video.java 新增 `sourceType` 和 `alistPath` 字段 | `Video.java` |
| 2 | TranscodeService.java 新增 `executeTranscodeFromUrl()` | `TranscodeService.java` |
| 3 | TranscodeService.java 新增 `getVideoDurationFromUrl()` | `TranscodeService.java` |
| 4 | TranscodeService.java 新增 `cleanupPartialFiles()` | `TranscodeService.java` |
| 5 | TranscodeService.java 新增 `transcodeVideoFromAlistPath()` | `TranscodeService.java` |
| 6 | TranscodeService.java 修改 `handleTranscodeFailure()` 增加清理 | `TranscodeService.java` |
| 7 | CloudMediaService.java 修改 `processImportAsync()` 跳过下载 | `CloudMediaService.java` |
| 8 | CloudMediaService.java 新增 `startTranscodingFromAlist()` | `CloudMediaService.java` |
| 9 | 测试验证 | - |

---

## 10. 后续可扩展

1. **断点续传**：如果网络中断，支持从断点继续转码
2. **多清晰度并行**：同时生成 1080p/720p/480p 多档 HLS
3. **转码加速**：利用 GPU 加速（NVIDIA NVENC）
