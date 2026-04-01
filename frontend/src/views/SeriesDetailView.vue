<template>
  <div class="min-h-screen bg-[#0A0F1E]">
    <!-- Backdrop Background -->
    <div v-if="series" class="relative h-80 -mb-40">
      <div 
        class="absolute inset-0 bg-cover bg-center"
        :style="{ 
          backgroundImage: series.backdropPath ? `url(${series.backdropPath})` : 'none',
          filter: 'blur(30px) brightness(0.4)'
        }"
      ></div>
      <div class="absolute inset-0 bg-gradient-to-t from-[#0A0F1E] via-[#0A0F1E]/50 to-transparent"></div>
    </div>

    <!-- Content -->
    <div class="relative z-10 px-6 pb-8">
      <!-- Back Button -->
      <div class="pt-4 mb-4">
        <button 
          @click="goBack"
          class="flex items-center gap-2 text-gray-400 hover:text-white transition-colors"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
          </svg>
          返回
        </button>
      </div>

      <div v-if="loading" class="py-12 text-center">
        <div class="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin mx-auto"></div>
      </div>

      <div v-else-if="!series" class="py-24 text-center">
        <p class="text-gray-400">系列不存在</p>
      </div>

      <div v-else class="flex flex-col md:flex-row gap-8">
        <!-- Poster -->
        <div class="flex-shrink-0">
          <div class="w-64 aspect-[2/3] rounded-xl overflow-hidden bg-gray-800 shadow-2xl">
            <img 
              v-if="series.posterPath" 
              :src="series.posterPath" 
              :alt="series.name"
              class="w-full h-full object-cover"
            />
            <div v-else class="w-full h-full flex items-center justify-center">
              <svg class="w-16 h-16 text-gray-600" fill="currentColor" viewBox="0 0 24 24">
                <path d="M4 4h16a2 2 0 012 2v12a2 2 0 01-2 2H4a2 2 0 01-2-2V6a2 2 0 012-2z"/>
              </svg>
            </div>
          </div>
        </div>

        <!-- Info -->
        <div class="flex-1 pt-4">
          <h1 class="text-4xl font-bold text-white mb-2">{{ series.name }}</h1>
          
          <div class="flex flex-wrap items-center gap-4 text-gray-400 mb-6">
            <span v-if="series.tmdbId" class="px-2 py-1 bg-blue-500/20 text-blue-400 rounded text-xs">TMDB: {{ series.tmdbId }}</span>
            <span class="text-sm">{{ seasons.length }} 季</span>
            <span class="text-sm">{{ videos.length }} 集</span>
          </div>

          <p v-if="series.overview" class="text-gray-300 leading-relaxed mb-8 max-w-2xl">
            {{ series.overview }}
          </p>
        </div>
      </div>

      <!-- Season Tabs -->
      <div v-if="seasons.length > 0" class="mt-8">
        <div class="flex gap-2 overflow-x-auto pb-2">
          <button 
            @click="selectedSeasonId = null"
            :class="[
              'px-4 py-2 rounded-lg font-medium whitespace-nowrap transition-colors',
              selectedSeasonId === null 
                ? 'bg-primary text-white' 
                : 'bg-gray-800 text-gray-400 hover:text-white'
            ]"
          >
            全部 ({{ videos.length }})
          </button>
          <button 
            v-for="season in seasons" 
            :key="season.id"
            @click="selectedSeasonId = season.id"
            :class="[
              'px-4 py-2 rounded-lg font-medium whitespace-nowrap transition-colors',
              selectedSeasonId === season.id 
                ? 'bg-primary text-white' 
                : 'bg-gray-800 text-gray-400 hover:text-white'
            ]"
          >
            {{ season.name }} ({{ getSeasonVideoCount(season.id) }})
          </button>
        </div>
      </div>

      <!-- Videos Grid -->
      <div class="mt-8">
        <div v-if="displayedVideos.length === 0" class="text-center py-12">
          <p class="text-gray-400">暂无视频</p>
        </div>
        <div v-else class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 2xl:grid-cols-8 gap-6">
          <VideoCard 
            v-for="video in displayedVideos" 
            :key="video.uuid" 
            :video="video" 
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { seriesApi, type Series, type Season, type Video } from '../api/series'
import VideoCard from '../components/VideoCard.vue'

const router = useRouter()
const route = useRoute()

const series = ref<Series | null>(null)
const seasons = ref<Season[]>([])
const videos = ref<Video[]>([])
const loading = ref(true)
const selectedSeasonId = ref<number | null>(null)

const displayedVideos = computed(() => {
  if (selectedSeasonId.value === null) {
    return videos.value
  }
  return videos.value.filter(v => v.seasonId === selectedSeasonId.value)
})

function getSeasonVideoCount(seasonId: number): number {
  return videos.value.filter(v => v.seasonId === seasonId).length
}

function goBack() {
  router.back()
}

async function fetchDetail() {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const detail = await seriesApi.getDetail(id)
    series.value = detail.series
    seasons.value = detail.seasons
    videos.value = detail.videos
    if (seasons.value.length > 0) {
      selectedSeasonId.value = seasons.value[0].id
    }
  } catch (e) {
    console.error('Failed to fetch series detail:', e)
    series.value = null
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDetail()
})
</script>
