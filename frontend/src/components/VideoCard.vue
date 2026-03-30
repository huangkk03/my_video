<template>
  <div 
    class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden cursor-pointer hover:scale-105 hover:shadow-lg transition-all duration-300"
    @click="goToPlayer"
  >
    <div class="relative aspect-video bg-gray-100">
      <img 
        v-if="video.thumbnailPath" 
        :src="video.thumbnailPath" 
        :alt="video.title"
        class="w-full h-full object-cover"
      />
      <div v-else class="w-full h-full flex items-center justify-center">
        <svg class="w-16 h-16 text-gray-300" fill="currentColor" viewBox="0 0 24 24">
          <path d="M4 4h16a2 2 0 012 2v12a2 2 0 01-2 2H4a2 2 0 01-2-2V6a2 2 0 012-2zm0 2v12h16V6H4zm2 2l8 4-8 4V8z"/>
        </svg>
      </div>
      <div class="absolute bottom-2 right-2 px-2 py-1 bg-black/70 text-white text-xs rounded">
        {{ formatDuration(video.duration) }}
      </div>
      <div 
        v-if="video.status !== 'completed'"
        class="absolute top-2 right-2 px-2 py-1 text-xs rounded text-white"
        :class="statusClass"
      >
        {{ statusText }}
      </div>
    </div>
    <div class="p-4">
      <h3 class="font-semibold text-gray-800 truncate">{{ video.title }}</h3>
      <div class="mt-2 flex items-center justify-between text-sm text-gray-500">
        <span>{{ formatDate(video.createdAt) }}</span>
        <span>{{ formatSize(video.fileSize) }}</span>
      </div>
      <div v-if="video.currentPosition > 0 && video.status === 'completed'" class="mt-2">
        <div class="h-1 bg-gray-200 rounded-full overflow-hidden">
          <div 
            class="h-full bg-primary transition-all"
            :style="{ width: progressPercent + '%' }"
          />
        </div>
        <p class="text-xs text-gray-400 mt-1">已播放 {{ formatDuration(video.currentPosition) }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import type { Video } from '../api/video'

const props = defineProps<{ video: Video }>()
const router = useRouter()

const statusClass = computed(() => {
  switch (props.video.status) {
    case 'transcoding': return 'bg-yellow-500'
    case 'completed': return 'bg-green-500'
    case 'failed': return 'bg-red-500'
    default: return 'bg-gray-500'
  }
})

const statusText = computed(() => {
  switch (props.video.status) {
    case 'transcoding': return '转码中'
    case 'completed': return '已完成'
    case 'failed': return '失败'
    default: return '待处理'
  }
})

const progressPercent = computed(() => {
  if (props.video.duration === 0) return 0
  return Math.round((props.video.currentPosition / props.video.duration) * 100)
})

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

function formatSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

function goToPlayer() {
  if (props.video.status === 'completed') {
    router.push(`/player/${props.video.uuid}`)
  }
}
</script>