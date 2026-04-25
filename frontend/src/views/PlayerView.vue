<template>
  <div class="max-w-5xl mx-auto">
    <div class="mb-4">
      <router-link to="/" class="text-primary hover:underline flex items-center gap-2">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
        </svg>
        返回视频列表
      </router-link>
    </div>

    <div v-if="loading" class="aspect-video skeleton rounded-xl"></div>

    <div v-else-if="!video" class="text-center py-16">
      <p class="text-gray-500">视频不存在</p>
    </div>

    <div v-else>
      <h1 class="text-2xl font-semibold text-gray-800 mb-4">{{ video.title }}</h1>

      <div 
        class="relative bg-black rounded-xl overflow-hidden group" 
        style="aspect-ratio: 16/9;"
        @mousemove="handleMouseMove"
        @mouseleave="hideControls"
      >
        <div ref="playerRef" class="w-full h-full"></div>
        
        <div 
          ref="controlsRef"
          class="absolute bottom-0 left-0 right-0 p-4 bg-white/10 backdrop-blur-md transition-opacity duration-300"
          :class="controlsVisible ? 'opacity-100' : 'opacity-0'"
        >
          <div class="flex items-center justify-between text-white text-sm">
            <div class="flex items-center gap-4">
              <span class="font-medium">状态:</span>
              <span :class="statusClass">{{ statusText }}</span>
            </div>
            <div class="text-gray-300">
              {{ formatSize(video.fileSize) }} | {{ formatDate(video.createdAt) }}
            </div>
          </div>
        </div>
      </div>

      <div v-if="video.currentPosition > 0" class="mt-4 p-4 bg-gray-50 rounded-lg">
        <div class="flex items-center justify-between text-sm">
          <div class="text-gray-600">
            <span class="font-medium">播放进度:</span>
            <span class="ml-2 text-primary">{{ formatDuration(video.currentPosition) }} / {{ formatDuration(video.duration) }}</span>
          </div>
          <div class="text-gray-500">
            已观看 {{ progressPercent }}%
          </div>
        </div>
        <div class="mt-2 h-1 bg-gray-200 rounded-full overflow-hidden">
          <div
            class="h-full bg-primary transition-all"
            :style="{ width: progressPercent + '%' }"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import ArtPlayer from 'artplayer'
import Hls from 'hls.js'
import { videoApi, type Video } from '../api/video'

const route = useRoute()
const playerRef = ref<HTMLElement>()
const controlsRef = ref<HTMLElement>()
const video = ref<Video | null>(null)
const loading = ref(true)
const controlsVisible = ref(true)
const isRetranscoding = ref(false)
const retranscodeProgress = ref(0)
let art: ArtPlayer | null = null
let hlsInstance: Hls | null = null
let hideTimer: ReturnType<typeof setTimeout> | null = null
let pendingPlayPromise: Promise<void> | null = null
let isPlayerInitializing = false
let retranscodePollingTimer: ReturnType<typeof setInterval> | null = null
let recoverAttemptCount = 0
const MAX_RECOVER_ATTEMPTS = 3

const statusClass = computed(() => {
  switch (video.value?.status) {
    case 'transcoding': return 'text-yellow-600'
    case 'completed': return 'text-green-600'
    case 'failed': return 'text-red-600'
    default: return 'text-gray-600'
  }
})

const statusText = computed(() => {
  if (isRetranscoding.value) {
    return `重新转码中 ${retranscodeProgress.value}%`
  }
  switch (video.value?.status) {
    case 'transcoding': return '转码中...'
    case 'completed': return '已完成'
    case 'failed': return '转码失败'
    default: return '待处理'
  }
})

const progressPercent = computed(() => {
  if (!video.value || video.value.duration === 0) return 0
  return Math.round((video.value.currentPosition / video.value.duration) * 100)
})

function formatSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

function formatDuration(ms: number): string {
  const seconds = Math.floor(ms / 1000)
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
  return `${m}:${s.toString().padStart(2, '0')}`
}

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

function handleMouseMove() {
  controlsVisible.value = true
  if (hideTimer) clearTimeout(hideTimer)
  hideTimer = setTimeout(() => {
    controlsVisible.value = false
  }, 3000)
}

function hideControls() {
  controlsVisible.value = false
  if (hideTimer) clearTimeout(hideTimer)
}

async function fetchVideo() {
  loading.value = true
  try {
    const uuid = route.params.uuid as string
    video.value = await videoApi.getByUuid(uuid)
  } catch (e) {
    console.error('Failed to fetch video:', e)
  } finally {
    loading.value = false
  }
}

async function initPlayer() {
  if (isPlayerInitializing) {
    console.log('Player is initializing, skipping...')
    return
  }
  isPlayerInitializing = true
  recoverAttemptCount = 0

  try {
    await nextTick()

    if (!playerRef.value || !video.value || video.value.status !== 'completed') {
      return
    }

    const uuid = video.value.uuid
    const streamUrl = videoApi.getStreamUrl(uuid)

    if (pendingPlayPromise) {
      try {
        await pendingPlayPromise.catch(() => {})
      } catch (e) {}
      pendingPlayPromise = null
    }

    if (art) {
      art.destroy()
      art = null
    }

    art = new ArtPlayer({
      container: playerRef.value,
      autoplay: false,
      muted: false,
      theme: '#00AEEC',
      url: streamUrl,
      type: 'm3u8',
      fullscreen: true,
      autoOrientation: true,
      customType: {
        m3u8: function(videoEl, url) {
          if (Hls.isSupported()) {
            const hls = new Hls({
              enableWorker: true,
              lowLatencyMode: false,
              preloadNext: true,
              maxBufferLength: 30,
              maxMaxBufferLength: 60,
              maxBufferSize: 50 * 1000 * 1000,
              maxBufferHole: 0.5,
              fragLoadingTimeOut: 20000,
              manifestLoadingTimeOut: 10000,
            })
            hlsInstance = hls
            hls.loadSource(url)
            hls.attachMedia(videoEl)
            hls.on(Hls.Events.ERROR, (event, data) => {
              if (data.fatal) {
                console.error('HLS fatal error:', data)

                if (data.details === Hls.ErrorDetails.BUFFER_APPEND_ERROR) {
                  if (data.sourceBufferName === 'audio') {
                    console.log('Audio buffer append error, attempting recovery...')
                    if (recoverAttemptCount < MAX_RECOVER_ATTEMPTS) {
                      recoverAttemptCount++
                      hlsInstance?.recoverMediaError()
                      return
                    }
                  } else {
                    if (recoverAttemptCount < MAX_RECOVER_ATTEMPTS) {
                      recoverAttemptCount++
                      console.log(`Attempting to recover media error (${recoverAttemptCount}/${MAX_RECOVER_ATTEMPTS})`)
                      hlsInstance?.recoverMediaError()
                      return
                    }
                  }
                  console.error('Max recover attempts reached, reloading player...')
                  showNotification('播放器出错，正在重新加载...')
                  setTimeout(() => {
                    window.location.reload()
                  }, 2000)
                  return
                }

                if (data.details === Hls.ErrorDetails.BUFFER_ADD_CODEC_ERROR) {
                  showNotification('当前视频编码格式暂不支持，正在尝试兼容性转码...')
                  if (video.value) {
                    triggerRetranscode(video.value.uuid)
                  }
                  return
                }

                if (data.details === Hls.ErrorDetails.MANIFEST_LOAD_ERROR ||
                    data.details === Hls.ErrorDetails.MANIFEST_LOAD_TIMEOUT ||
                    data.details === Hls.ErrorDetails.LEVEL_LOAD_ERROR ||
                    data.details === Hls.ErrorDetails.LEVEL_LOAD_TIMEOUT) {
                  showNotification('视频加载失败，正在重试...')
                  hlsInstance?.startLoad()
                  return
                }

                console.error('Unhandled fatal HLS error, reloading player...')
                showNotification('播放器出错，正在重新加载...')
                setTimeout(() => {
                  window.location.reload()
                }, 2000)
              }
            })

          } else if (videoEl.canPlayType('application/vnd.apple.mpegurl')) {
            videoEl.src = url
          }
        }
      },
    })

    art.on('ready', () => {
      pendingPlayPromise = art!.play().catch((err) => {
        if (err.name !== 'AbortError') {
          console.error('Play error:', err)
        }
      })
      if (video.value?.currentPosition && video.value.currentPosition > 0) {
        art!.seek = video.value.currentPosition / 1000
      }
    })

    let lastSaveTime = 0
    art.on('timeupdate', (apt) => {
      const now = Date.now()
      if (now - lastSaveTime >= 5000) {
        lastSaveTime = now
        saveProgress(apt.currentTime)
      }
    })

    handleMouseMove()
  } finally {
    isPlayerInitializing = false
  }
}

async function saveProgress(time: number) {
  if (!video.value) return
  const position = Math.floor(time * 1000)
  try {
    await videoApi.updateProgress(video.value.uuid, position)
    if (video.value) {
      video.value.currentPosition = position
    }
  } catch (e) {
    console.error('Failed to save progress:', e)
  }
}

function showNotification(message: string) {
  if ('Notification' in window && Notification.permission === 'granted') {
    new Notification('视频提示', { body: message })
  } else if ('Notification' in window && Notification.permission !== 'denied') {
    Notification.requestPermission().then(permission => {
      if (permission === 'granted') {
        new Notification('视频提示', { body: message })
      } else {
        console.log(message)
      }
    })
  } else {
    console.log(message)
  }
}

async function triggerRetranscode(uuid: string) {
  try {
    const res = await fetch(`/api/videos/${uuid}/retranscode`, { method: 'POST' })
    const data = await res.json()
    if (data.status === 'retranscode initiated') {
      isRetranscoding.value = true
      retranscodeProgress.value = 0
      startRetranscodePolling(uuid)
    }
  } catch (e) {
    console.error('Failed to trigger retranscode:', e)
  }
}

function startRetranscodePolling(uuid: string) {
  if (retranscodePollingTimer) {
    clearInterval(retranscodePollingTimer)
  }
  retranscodePollingTimer = setInterval(async () => {
    try {
      const res = await fetch(`/api/videos/${uuid}/transcode-progress`)
      const data = await res.json()
      if (data.progress !== undefined) {
        retranscodeProgress.value = data.progress
      }
      if (data.status === 'completed') {
        clearRetranscodePolling()
        isRetranscoding.value = false
        fetchVideo()
      } else if (data.status === 'failed') {
        clearRetranscodePolling()
        isRetranscoding.value = false
      }
    } catch (e) {
      console.error('Polling error:', e)
    }
  }, 5000)
}

function clearRetranscodePolling() {
  if (retranscodePollingTimer) {
    clearInterval(retranscodePollingTimer)
    retranscodePollingTimer = null
  }
}

watch(() => video.value, (newVideo) => {
  if (newVideo?.status === 'completed') {
    initPlayer()
  }
})

onMounted(() => {
  fetchVideo()
})

onUnmounted(() => {
  if (hideTimer) clearTimeout(hideTimer)
  if (art) {
    art.destroy()
    art = null
  }
  if (hlsInstance) {
    hlsInstance.destroy()
    hlsInstance = null
  }
  recoverAttemptCount = 0
  clearRetranscodePolling()
})
</script>