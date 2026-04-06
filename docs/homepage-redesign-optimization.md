# 首页 UI 改版与缩略图修复优化方案

## 1. 问题与目标

### 问题 1：缩略图丢失
- 转码完成后封面缩略图丢失

### 问题 2：首页 UI 改版
- 当前首页视觉风格不符合 Jellyfin
- 需要深色主题 (#101010)、搜索栏、库切换标签
- 海报墙要求：2:3 比例、圆角、阴影、悬停浮动 + 播放图标

---

## 2. 代码改动详解

### 2.1 修复缩略图丢失 - `TranscodeService.java`

#### 问题根因

`executeTranscodeFromUrl` 在单次 FFmpeg 调用中同时输出 HLS 和缩略图，这是**无效语法**。

HLS muxer 关闭输出文件后，后续的 `-ss`, `-vframes`, `-vf` 参数被忽略。

#### 修复方案

**分两步执行**：
1. 第一步：执行 HLS 转码
2. 第二步：从 HLS 文件单独提取缩略图

#### 改动文件

`D:\video\my_video\backend\src\main\java\com\video\service\TranscodeService.java`

#### 改动方法

`executeTranscodeFromUrl(String sourceUrl, Video video, TranscodeTask task)`

#### 改动前后对比

**改动前（错误）：**
```java
// 单次调用同时指定 HLS 输出和缩略图输出 - 无效语法
List<String> command = new ArrayList<>(Arrays.asList(
    FFMPEG_COMMAND,
    "-reconnect", "1",
    "-reconnect_streamed", "1",
    "-reconnect_delay_max", "5",
    "-i", sourceUrl,
    "-codec:v", "libx264",
    "-preset", "fast",
    "-crf", "23",
    "-codec:a", "aac",
    "-b:a", "128k",
    "-f", "hls",
    "-hls_time", "10",
    "-hls_list_size", "0",
    "-http_seekable", "1",
    hlsPath.toString(),        // HLS 输出
    "-ss", "00:00:05",        // 缩略图参数（被忽略！）
    "-vframes", "1",
    "-vf", "scale=320:-1",
    thumbnailPath.toString()   // 缩略图输出（被忽略！）
));
```

**改动后（正确）：**
```java
// ========== 步骤 1：HLS 转码 ==========
List<String> hlsCommand = new ArrayList<>(Arrays.asList(
    FFMPEG_COMMAND,
    "-reconnect", "1",
    "-reconnect_streamed", "1",
    "-reconnect_delay_max", "5",
    "-i", sourceUrl,
    "-codec:v", "libx264",
    "-preset", "fast",
    "-crf", "23",
    "-codec:a", "aac",
    "-b:a", "128k",
    "-f", "hls",
    "-hls_time", "10",
    "-hls_list_size", "0",
    "-http_seekable", "1",
    hlsPath.toString()
));

task.setFfmpegCommand(String.join(" ", hlsCommand));
transcodeTaskRepository.save(task);

log.info("Executing FFmpeg stream transcode from URL: {}",
         sourceUrl.replaceAll("sign=[^&]+", "sign=HIDDEN"));

ProcessBuilder pb = new ProcessBuilder(hlsCommand);
pb.redirectErrorStream(true);
Process process = pb.start();

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

// ========== 步骤 2：单独生成缩略图 ==========
Path thumbnailPath = videoDir.resolve("thumbnail.jpg");
String[] thumbCmd = {
    FFMPEG_COMMAND,
    "-i", hlsPath.toString(),
    "-ss", "00:00:05",
    "-vframes", "1",
    "-vf", "scale=320:-1",
    thumbnailPath.toString()
};
ProcessBuilder thumbPb = new ProcessBuilder(thumbCmd);
thumbPb.redirectErrorStream(true);
Process thumbProcess = thumbPb.start();
thumbProcess.waitFor();

// ========== 保存结果 ==========
video.setHlsPath(hlsPath.toString());
if (Files.exists(thumbnailPath)) {
    video.setThumbnailPath(thumbnailPath.toString());
}
videoRepository.save(video);
```

---

### 2.2 首页 UI 改版 - `HomeView.vue`

#### 改动文件

`D:\video\my_video\frontend\src\views\HomeView.vue`

#### 布局结构

```
┌──────────────────────────────────────────────────────────────┐
│  [Logo]  [搜索框........................]  [库切换]  [管理]  │
├──────────────────────────────────────────────────────────────┤
│  最近更新                                                     │
│  ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐ ┌────┐         │
│  │ ▢  │ │ ▢  │ │ ▢  │ │ ▢  │ │ ▢  │ │ ▢  │ │ ▢  │  ...   │
│  │ ▷  │ │ ▷  │ │ ▷  │ │ ▷  │ │ ▷  │ │ ▷  │ │ ▷  │         │
│  └────┘ └────┘ └────┘ └────┘ └────┘ └────┘ └────┘         │
└──────────────────────────────────────────────────────────────┘
```

#### 样式要点

| 元素 | 样式 |
|------|------|
| 背景色 | `#101010` |
| 字体 | 系统非衬线字体 |
| 搜索框 | 深灰背景 `#1a1a1a`，白色文字，圆角 |
| 标签切换 | 选中白色 + 底部指示条 |
| 海报卡片 | 2:3 比例，圆角 12px，阴影 |
| 悬停效果 | translateY(-8px) 向上浮动，显示播放图标 |
| 响应式 | 移动端 2-3 列，平板 3-4 列，桌面 5-7 列 |

#### 完整组件代码

```vue
<template>
  <div class="min-h-screen" style="background-color: #101010;">
    <!-- 顶部导航栏 -->
    <header class="sticky top-0 z-40 px-6 py-4" style="background-color: rgba(16,16,16,0.95); backdrop-filter: blur(10px);">
      <div class="flex items-center gap-6 max-w-screen-2xl mx-auto">
        <!-- Logo -->
        <router-link to="/" class="flex items-center gap-2 shrink-0">
          <div class="w-8 h-8 rounded-lg bg-primary flex items-center justify-center">
            <svg class="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"/>
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
          </div>
          <span class="text-white font-semibold text-lg hidden sm:block">我的媒体库</span>
        </router-link>

        <!-- 搜索框 -->
        <div class="flex-1 max-w-xl">
          <div class="relative">
            <input
              v-model="searchQuery"
              type="text"
              placeholder="搜索电影、电视剧、动漫..."
              class="w-full px-4 py-2.5 pl-10 rounded-lg text-white text-sm"
              style="background-color: #1a1a1a; border: 1px solid #333;"
            />
            <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
            </svg>
          </div>
        </div>

        <!-- 标签切换 -->
        <nav class="flex items-center gap-1">
          <button
            v-for="tab in tabs"
            :key="tab.id"
            @click="activeTab = tab.id"
            class="px-4 py-2 rounded-lg text-sm font-medium transition-colors relative"
            :class="activeTab === tab.id ? 'text-white' : 'text-gray-400 hover:text-white'"
          >
            {{ tab.name }}
            <div
              v-if="activeTab === tab.id"
              class="absolute bottom-0 left-3 right-3 h-0.5 rounded-full bg-primary"
            />
          </button>
        </nav>

        <!-- 管理入口 -->
        <router-link
          to="/admin"
          class="px-4 py-2 rounded-lg text-sm font-medium text-gray-400 hover:text-white transition-colors"
        >
          管理
        </router-link>
      </div>
    </header>

    <!-- 主内容区 -->
    <main class="px-6 py-6 max-w-screen-2xl mx-auto">
      <!-- 最近更新 -->
      <section class="mb-10">
        <h2 class="text-white text-xl font-semibold mb-4">最近更新</h2>
        <div v-if="loading" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-7 gap-4">
          <div v-for="i in 14" :key="i" class="aspect-[2/3] rounded-xl bg-gray-800 animate-pulse" />
        </div>
        <div v-else-if="recentVideos.length === 0" class="text-gray-400 py-12 text-center">
          暂无内容
        </div>
        <div v-else class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-7 gap-4">
          <VideoCard
            v-for="video in filteredVideos"
            :key="video.uuid"
            :video="video"
            @click="goToPlayer(video.uuid)"
          />
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import VideoCard from '../components/VideoCard.vue'
import { videoApi, type Video } from '../api/video'

const router = useRouter()
const searchQuery = ref('')
const activeTab = ref('home')
const recentVideos = ref<Video[]>([])
const loading = ref(true)

const tabs = [
  { id: 'home', name: '首页' },
  { id: 'movies', name: '电影' },
  { id: 'tvshows', name: '电视剧' },
  { id: 'anime', name: '动漫' },
]

const filteredVideos = computed(() => {
  let result = recentVideos.value
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(v => v.title.toLowerCase().includes(q))
  }
  return result
})

onMounted(async () => {
  await fetchRecentVideos()
})

async function fetchRecentVideos() {
  try {
    loading.value = true
    const res = await videoApi.list({ page: 0, size: 21 })
    recentVideos.value = res.content || []
  } catch (e) {
    console.error('Failed to fetch videos:', e)
  } finally {
    loading.value = false
  }
}

function goToPlayer(uuid: string) {
  router.push(`/player/${uuid}`)
}
</script>
```

---

### 2.3 海报卡片组件微调 - `VideoCard.vue`

#### 改动文件

`D:\video\my_video\frontend\src\components\VideoCard.vue`

#### 改动说明

微调悬停效果：向上浮动 8px，显示播放图标

#### 完整组件代码

```vue
<template>
  <div class="group relative cursor-pointer transition-transform duration-300 hover:-translate-y-2">
    <div class="relative aspect-[2/3] rounded-xl overflow-hidden shadow-lg">
      <!-- 海报图片 -->
      <img
        v-if="posterUrl"
        :src="posterUrl"
        :alt="video.title"
        class="w-full h-full object-cover"
      />
      <div v-else class="w-full h-full bg-gray-800 flex items-center justify-center">
        <svg class="w-12 h-12 text-gray-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 4v16M17 4v16M3 8h4m10 0h4M3 12h18M3 16h4m10 0h4M4 20h16a1 1 0 001-1V5a1 1 0 00-1-1H4a1 1 0 00-1 1v14a1 1 0 001 1z"/>
        </svg>
      </div>

      <!-- 悬停遮罩 -->
      <div class="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity duration-300 flex items-center justify-center">
        <!-- 播放图标 -->
        <div class="w-14 h-14 rounded-full bg-white/90 flex items-center justify-center">
          <svg class="w-7 h-7 text-gray-900 ml-0.5" fill="currentColor" viewBox="0 0 24 24">
            <path d="M8 5v14l11-7z"/>
          </svg>
        </div>
      </div>

      <!-- 底部信息 -->
      <div class="absolute bottom-0 left-0 right-0 p-2 bg-gradient-to-t from-black/80 to-transparent">
        <h3 class="text-white font-medium text-sm truncate">{{ video.title }}</h3>
        <div class="flex items-center gap-2 text-gray-300 text-xs mt-0.5">
          <span v-if="video.releaseYear">{{ video.releaseYear }}</span>
          <span v-if="video.rating" class="text-yellow-400">★ {{ video.rating.toFixed(1) }}</span>
        </div>
      </div>

      <!-- 播放进度条 -->
      <div
        v-if="progressPercent > 0"
        class="absolute bottom-0 left-0 h-1 bg-gray-600"
      >
        <div class="h-full bg-primary" :style="{ width: progressPercent + '%' }" />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Video {
  uuid: string
  title: string
  thumbnailPath?: string
  posterPath?: string
  duration?: number
  releaseYear?: number
  rating?: number
  currentPosition?: number
  status?: string
}

const props = defineProps<{
  video: Video
}>()

const posterUrl = computed(() => {
  return props.video.posterPath || props.video.thumbnailPath || ''
})

const progressPercent = computed(() => {
  if (!props.video.currentPosition || !props.video.duration || props.video.duration === 0) {
    return 0
  }
  return Math.min(100, (props.video.currentPosition / props.video.duration) * 100)
})
</script>
```

---

## 3. 实施步骤

| 步骤 | 操作 | 文件 |
|-----|------|------|
| 1 | 修复 `executeTranscodeFromUrl` 缩略图生成（分两步执行） | `TranscodeService.java` |
| 2 | 替换 `HomeView.vue` 为 Jellyfin 风格组件 | `HomeView.vue` |
| 3 | 微调 `VideoCard.vue` 悬停效果 | `VideoCard.vue` |
| 4 | 测试验证 | - |

---

## 4. 风险与注意事项

1. **缩略图生成**：从 HLS 提取缩略图可能不如从原始视频提取准确（HLS 已切片），但这是避免二次请求的折中方案
2. **响应式断点**：需根据实际效果调整 grid 列数
3. **搜索功能**：当前代码中搜索框仅有 UI，搜索逻辑通过 `computed` 客户端过滤实现

---

## 5. 后续可扩展

1. **完整搜索功能**：后端搜索 API 支持
2. **分类筛选**：按电影/电视剧/动漫分类展示
3. **无限滚动**：替代分页加载
4. **海报懒加载**：优化大数据量性能
5. **记住播放进度**：跨设备同步
