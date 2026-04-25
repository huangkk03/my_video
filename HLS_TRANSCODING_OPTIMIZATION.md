# HLS 转码优化方案

## 问题分析

| 错误类型 | 原因 |
|---------|------|
| `bufferAppendError` (audio) | 音频编码/时间戳问题导致 buffer 追加失败 |
| `MediaSource readyState: closed` | 播放器生命周期管理问题 |

---

## 一、后端 FFmpeg 转码命令优化

### 1.1 修改文件
- `backend/src/main/java/com/video/service/TranscodeService.java`

### 1.2 修改位置
- `executeTranscode()` 方法 - 本地文件转码 (第 120-135 行)
- `executeTranscodeFromUrl()` 方法 - 远程 URL 转码 (第 223-242 行)

### 1.3 新 FFmpeg 命令参数

#### 本地文件转码 (executeTranscode)

**当前命令**:
```java
"-codec:v", "libx264",
"-pix_fmt", "yuv420p",
"-profile:v", "high",
"-level", "4.1",
"-preset", "veryfast",
"-crf", "23",
"-codec:a", "aac",
"-b:a", "128k",
"-f", "hls",
"-hls_time", "10",
"-hls_list_size", "0",
```

**优化后命令**:
```java
"-fflags", "+genpts",
"-async", "1",
"-codec:v", "libx264",
"-pix_fmt", "yuv420p",
"-profile:v", "high",
"-level", "4.1",
"-preset", "veryfast",
"-crf", "23",
"-c:a", "aac",
"-b:a", "192k",
"-ar", "44100",
"-movflags", "+faststart",
"-vsync", "cfr",
"-f", "hls",
"-hls_time", "6",
"-hls_list_size", "0",
"-hls_flags", "delete_segments+independent_segments",
```

#### 远程 URL 转码 (executeTranscodeFromUrl)

**当前命令**:
```java
"-reconnect", "1",
"-reconnect_streamed", "1",
"-reconnect_delay_max", "5",
"-i", sourceUrl,
"-codec:v", "libx264",
"-pix_fmt", "yuv420p",
"-profile:v", "high",
"-level", "4.1",
"-preset", "veryfast",
"-crf", "23",
"-codec:a", "aac",
"-b:a", "128k",
"-f", "hls",
"-hls_time", "10",
"-hls_list_size", "0",
"-http_seekable", "1",
```

**优化后命令**:
```java
"-reconnect", "1",
"-reconnect_streamed", "1",
"-reconnect_delay_max", "5",
"-fflags", "+genpts",
"-async", "1",
"-i", sourceUrl,
"-codec:v", "libx264",
"-pix_fmt", "yuv420p",
"-profile:v", "high",
"-level", "4.1",
"-preset", "veryfast",
"-crf", "23",
"-c:a", "aac",
"-b:a", "192k",
"-ar", "44100",
"-movflags", "+faststart",
"-vsync", "cfr",
"-f", "hls",
"-hls_time", "6",
"-hls_list_size", "0",
"-hls_flags", "delete_segments+independent_segments",
"-http_seekable", "1",
```

### 1.4 参数说明

| 参数 | 作用 |
|------|------|
| `-fflags +genpts` | 强制生成 PTS (Presentation Time Stamp)，解决时间戳错乱问题 |
| `-async 1` | 音频同步修正，确保音频与视频同步 |
| `-c:a aac -b:a 192k -ar 44100` | 强制转换音频为标准 AAC 192kbps 44.1kHz |
| `-movflags +faststart` | 优化文件封装，对流媒体播放更友好 |
| `-vsync cfr` | 强制固定帧率 (Constant Frame Rate)，防止掉帧导致 buffer 报错 |
| `-hls_time 6` | 切片长度 6 秒（减少单次加载数据量） |
| `-hls_flags delete_segments+independent_segments` | 确保切片独立且可删除，减少内存占用 |

---

## 二、前端 HLS 错误恢复逻辑优化

### 2.1 修改文件
- `frontend/src/views/PlayerView.vue`

### 2.2 修改位置
- HLS 实例创建部分
- Hls.Events.ERROR 事件处理

### 2.3 新增变量

```javascript
let hlsInstance: Hls | null = null
let recoverAttemptCount = 0
const MAX_RECOVER_ATTEMPTS = 3
```

### 2.4 HLS 错误处理逻辑

```javascript
hls.on(Hls.Events.ERROR, (event, data) => {
  if (data.fatal) {
    console.error('HLS fatal error:', data)

    // bufferAppendError 恢复逻辑
    if (data.details === Hls.ErrorDetails.BUFFER_APPEND_ERROR) {
      if (recoverAttemptCount < MAX_RECOVER_ATTEMPTS) {
        recoverAttemptCount++
        console.log(`Attempting to recover media error (${recoverAttemptCount}/${MAX_RECOVER_ATTEMPTS})`)
        if (hlsInstance) {
          hlsInstance.recoverMediaError()
        }
      } else {
        console.error('Max recover attempts reached, reloading player...')
        showNotification('播放器出错，正在重新加载...')
        // 强制重载页面
        setTimeout(() => {
          window.location.reload()
        }, 2000)
      }
      return
    }

    // BUFFER_ADD_CODEC_ERROR - 触发重新转码
    if (data.details === Hls.ErrorDetails.BUFFER_ADD_CODEC_ERROR) {
      showNotification('当前视频编码格式暂不支持，正在尝试兼容性转码...')
      if (video.value) {
        triggerRetranscode(video.value.uuid)
      }
    }
  }
})
```

### 2.5 关键改动说明

| 项目 | 说明 |
|------|------|
| `recoverAttemptCount` | 记录恢复尝试次数 |
| `MAX_RECOVER_ATTEMPTS = 3` | 最多尝试3次恢复 |
| `hlsInstance.recoverMediaError()` | 调用 HLS.js 内置恢复方法 |
| 超过3次后 `window.location.reload()` | 强制重载页面，彻底重置播放器状态 |

### 2.6 保存 hlsInstance 引用

在创建 Hls 实例后保存引用:
```javascript
if (Hls.isSupported()) {
  const hls = new Hls({...})
  hlsInstance = hls  // 保存引用
  hls.loadSource(url)
  hls.attachMedia(videoEl)
}
```

### 2.7 播放器销毁时清理

```javascript
if (art) {
  art.destroy()
  art = null
}
if (hlsInstance) {
  hlsInstance.destroy()
  hlsInstance = null
}
```

---

## 三、测试验证计划

### 3.1 转码命令验证

1. **重新触发转码**
   - 媒体库页面点击"重新转码"按钮

2. **检查 FFmpeg 命令**
   ```sql
   SELECT ffmpeg_command FROM transcode_tasks WHERE video_uuid = 'xxx';
   ```
   - 确认包含 `-fflags +genpts`
   - 确认包含 `-async 1`
   - 确认包含 `-c:a aac -b:a 192k -ar 44100`
   - 确认包含 `-vsync cfr`
   - 确认包含 `-hls_time 6`

3. **检查输出文件**
   - 切片数量增加（因为时间缩短到 6 秒）
   - 每个切片更小

### 3.2 前端错误恢复验证

1. 播放测试视频
2. 打开浏览器控制台
3. 模拟网络波动或触发错误
4. 观察:
   - recoverAttemptCount 是否递增
   - 是否调用 `recoverMediaError()`
   - 超过3次后是否自动重载页面

---

## 四、影响范围

### 4.1 需要重新转码的视频

所有已转码视频都需要重新转码才能使用新参数：
- 手动点击"重新转码"按钮
- 或等待下次导入新视频时自动使用新参数

### 4.2 兼容性说明

| 参数 | 兼容性影响 |
|------|-----------|
| `-pix_fmt yuv420p` | 提升兼容性（8-bit） |
| `-profile:v high -level 4.1` | 提升兼容性（不再过高） |
| `-vsync cfr` | 提升兼容性（固定帧率） |
| `-hls_time 6` | 提升兼容性（更小的切片） |

---

## 五、回滚方案

如需回滚，修改 FFmpeg 命令移除以下参数：
- `-fflags +genpts`
- `-async 1`
- `-c:a aac -b:a 192k -ar 44100` (改回 `-codec:a aac -b:a 128k`)
- `-movflags +faststart`
- `-vsync cfr`
- `-hls_flags delete_segments+independent_segments`
- `-hls_time 6` (改回 `10`)
