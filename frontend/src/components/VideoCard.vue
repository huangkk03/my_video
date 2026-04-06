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

      <!-- 状态标签 -->
      <div
        v-if="video.status !== 'completed'"
        class="absolute top-2 left-2 px-2 py-1 text-xs rounded text-white"
        :class="statusClass"
      >
        {{ statusText }}
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
import type { Video } from '../api/video'

const props = defineProps<{ video: Video }>()

const posterUrl = computed(() => {
  return props.video.posterPath || props.video.thumbnailPath || ''
})

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
  if (!props.video.currentPosition || !props.video.duration || props.video.duration === 0) {
    return 0
  }
  return Math.min(100, (props.video.currentPosition / props.video.duration) * 100)
})
</script>
