<template>
  <div
    class="flex-shrink-0 w-80 cursor-pointer group"
    @click="$emit('click')"
  >
    <div class="relative aspect-video rounded-lg overflow-hidden bg-gray-800">
      <img
        :src="thumbnailUrl || fallback"
        :alt="title"
        class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
      />
      <div class="absolute inset-0 bg-black/0 group-hover:bg-black/50 transition-colors duration-300 flex items-center justify-center">
        <div class="w-16 h-16 rounded-full bg-white/90 flex items-center justify-center opacity-0 group-hover:opacity-100 transition-opacity duration-300 scale-75 group-hover:scale-100">
          <svg class="w-8 h-8 text-black ml-1" fill="currentColor" viewBox="0 0 24 24">
            <path d="M8 5v14l11-7z"/>
          </svg>
        </div>
      </div>
      <div class="absolute top-2 left-2 px-2 py-1 rounded bg-black/70 text-white text-xs font-medium">
        E{{ padNumber(episodeNumber) }}
      </div>
      <div v-if="currentPosition === 0" class="absolute top-2 right-2 px-2 py-1 rounded bg-blue-600 text-white text-xs font-medium">
        未看
      </div>
      <div class="absolute bottom-2 right-2 px-2 py-1 rounded bg-black/70 text-white text-xs">
        {{ formatDur(duration) }}
      </div>
    </div>
    <div class="mt-2">
      <h3 class="text-white text-sm font-medium truncate">{{ title }}</h3>
      <div v-if="currentPosition > 0" class="mt-1">
        <div class="h-1 bg-gray-700 rounded-full overflow-hidden">
          <div class="h-full bg-green-500 rounded-full" :style="{ width: progressPct + '%' }"></div>
        </div>
        <p class="text-gray-500 text-xs mt-0.5">{{ formatDur(currentPosition) }} watched</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  episodeNumber: number
  title: string
  thumbnailUrl: string
  duration: number
  currentPosition: number
  overview?: string
}>()

defineEmits<{ click: [] }>()

const fallback = 'https://image.tmdb.org/t/p/w500/8TUEj3W2fZf2Z4M9J8E3LQnK2j.jpg'

const progressPct = computed(() => {
  if (!props.duration || !props.currentPosition) return 0
  return Math.min(100, (props.currentPosition / props.duration) * 100)
})

function padNumber(n: number): string {
  return String(n).padStart(2, '0')
}

function formatDur(ms: number): string {
  if (!ms) return '--'
  const totalMinutes = Math.floor(ms / 60000)
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`
}
</script>
