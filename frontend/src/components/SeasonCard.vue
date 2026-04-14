<template>
  <div 
    class="flex-shrink-0 cursor-pointer group"
    @click="$emit('click', id)"
  >
    <div class="w-28 aspect-[2/3] rounded-lg overflow-hidden shadow-lg transition-all duration-300 group-hover:scale-105 group-hover:shadow-xl group-hover:ring-2 group-hover:ring-white">
      <img 
        :src="posterUrl || fallbackPoster" 
        :alt="name"
        class="w-full h-full object-cover"
      />
    </div>
    <p class="text-white text-sm mt-2 text-center truncate px-1">{{ name }}</p>
    <p class="text-gray-400 text-xs mt-0.5 text-center">{{ episodeCount }} 集</p>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  id: number
  name: string
  posterUrl: string
  episodeCount: number
}>()

defineEmits<{
  click: [id: number]
}>()

const fallbackPoster = 'https://image.tmdb.org/t/p/w500/8TUEj3W2fZf2Z4M9J8E3LQnK2j.jpg'
</script>
