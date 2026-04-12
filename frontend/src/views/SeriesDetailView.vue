<template>
  <div class="min-h-screen" style="background-color: #101010;">
    <!-- Backdrop Background -->
    <div v-if="series" class="relative h-96 -mb-48">
      <div
        class="absolute inset-0 bg-cover bg-center"
        :style="{
          backgroundImage: series.backdropPath ? `url(${series.backdropPath})` : 'none',
          filter: 'blur(40px) brightness(0.3)'
        }"
      ></div>
      <div class="absolute inset-0" style="background: linear-gradient(to top, #101010 0%, rgba(16,16,16,0.6) 50%, rgba(16,16,16,0.8) 100%);"></div>
    </div>

    <!-- Content -->
    <div class="relative z-10 px-6 pb-8 max-w-screen-2xl mx-auto">
      <!-- Back Button -->
      <div class="pt-6 mb-4">
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

      <!-- Series Info Header -->
      <div v-else class="flex flex-col md:flex-row gap-8">
        <!-- Poster -->
        <div class="flex-shrink-0 mx-auto md:mx-0">
          <div class="w-48 md:w-64 aspect-[2/3] rounded-xl overflow-hidden bg-gray-800 shadow-2xl">
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
        <div class="flex-1 text-center md:text-left">
          <h1 class="text-3xl md:text-4xl font-bold text-white mb-3">{{ series.name }}</h1>

          <!-- Meta Info -->
          <div class="flex flex-wrap items-center justify-center md:justify-start gap-3 text-gray-400 mb-4">
            <span v-if="series.releaseYear" class="text-lg">{{ series.releaseYear }}</span>
            <span v-if="series.runtime" class="text-lg">{{ series.runtime }} 分钟</span>
            <span class="text-lg">{{ seasons.length }} 季</span>
            <span class="text-lg">{{ videos.length }} 集</span>
          </div>

          <!-- Rating -->
          <div v-if="series.rating" class="flex items-center justify-center md:justify-start gap-2 mb-4">
            <div class="flex items-center gap-1">
              <svg class="w-5 h-5 text-yellow-400" fill="currentColor" viewBox="0 0 24 24">
                <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
              </svg>
              <span class="text-xl font-semibold text-white">{{ series.rating.toFixed(1) }}</span>
            </div>
          </div>

          <!-- Genres -->
          <div v-if="series.genres" class="flex flex-wrap items-center justify-center md:justify-start gap-2 mb-4">
            <span
              v-for="genre in series.genres.split(',')"
              :key="genre"
              class="px-3 py-1 text-sm rounded-full text-gray-300"
              style="background-color: #2a2a2a;"
            >
              {{ genre.trim() }}
            </span>
          </div>

          <!-- Overview -->
          <p v-if="series.overview" class="text-gray-300 leading-relaxed max-w-3xl mb-6">
            {{ series.overview }}
          </p>

          <!-- Action Buttons -->
          <div class="flex flex-wrap items-center justify-center md:justify-start gap-3">
            <button
              v-if="displayedVideos.length > 0"
              @click="playFirstUnwatched"
              class="flex items-center gap-2 px-6 py-3 bg-primary text-white rounded-lg hover:bg-opacity-90 transition-colors"
            >
              <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                <path d="M8 5v14l11-7z"/>
              </svg>
              继续观看
            </button>
            <button
              @click="toggleFavorite"
              class="flex items-center gap-2 px-6 py-3 rounded-lg transition-colors"
              :class="isFavorite ? 'bg-yellow-500/20 text-yellow-400' : 'bg-gray-700 text-gray-300 hover:bg-gray-600'"
            >
              <svg class="w-5 h-5" :fill="isFavorite ? 'currentColor' : 'none'" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z"/>
              </svg>
              收藏
            </button>
          </div>
        </div>
      </div>

      <!-- Season Tabs -->
      <div v-if="seasons.length > 0" class="mt-10">
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

      <!-- Episodes List -->
      <div v-if="selectedSeasonId !== null && currentSeasonVideos.length > 0" class="mt-8">
        <h3 class="text-white text-xl font-semibold mb-4">{{ getSeasonName(selectedSeasonId) }}</h3>
        <div class="space-y-2">
          <div
            v-for="(video, index) in currentSeasonVideos"
            :key="video.uuid"
            @click="goToPlayer(video.uuid)"
            class="flex items-center gap-4 p-3 rounded-lg cursor-pointer transition-colors"
            style="background-color: #1a1a1a;"
            @mouseenter="hoveredEpisode = video.uuid"
            @mouseleave="hoveredEpisode = null"
          >
            <!-- Episode Number -->
            <div class="w-12 h-12 rounded-lg flex items-center justify-center text-gray-400 font-semibold"
                 :class="hoveredEpisode === video.uuid ? 'bg-primary text-white' : 'bg-gray-800'">
              {{ video.episodeNumber || index + 1 }}
            </div>

            <!-- Thumbnail -->
            <div class="w-24 h-14 rounded-lg overflow-hidden bg-gray-800 relative">
              <img
                v-if="video.thumbnailPath || video.posterPath"
                :src="video.posterPath || video.thumbnailPath"
                :alt="video.title"
                class="w-full h-full object-cover"
              />
              <div v-if="video.currentPosition > 0" class="absolute bottom-0 left-0 right-0 h-1 bg-gray-600">
                <div class="h-full bg-primary" :style="{ width: getProgressPercent(video) + '%' }"></div>
              </div>
            </div>

            <!-- Info -->
            <div class="flex-1 min-w-0">
              <h4 class="text-white font-medium truncate">{{ video.title }}</h4>
              <p class="text-gray-400 text-sm truncate">{{ video.overview || '暂无简介' }}</p>
            </div>

            <!-- Duration -->
            <div class="text-gray-400 text-sm">
              {{ formatDuration(video.duration) }}
            </div>

            <!-- Play Icon on Hover -->
            <div v-if="hoveredEpisode === video.uuid" class="w-10 h-10 rounded-full bg-primary flex items-center justify-center">
              <svg class="w-5 h-5 text-white ml-0.5" fill="currentColor" viewBox="0 0 24 24">
                <path d="M8 5v14l11-7z"/>
              </svg>
            </div>
          </div>
        </div>
      </div>

      <!-- Videos Grid (for "All" or no season filter) -->
      <div v-else class="mt-8">
        <div v-if="displayedVideos.length === 0" class="text-center py-12">
          <p class="text-gray-400">暂无视频</p>
        </div>
        <div v-else class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 2xl:grid-cols-8 gap-4">
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
const hoveredEpisode = ref<string | null>(null)
const isFavorite = ref(false)

const displayedVideos = computed(() => {
  if (selectedSeasonId.value === null) {
    return videos.value
  }
  const targetSeasonId = Number(selectedSeasonId.value)
  return videos.value.filter(v => Number(v.seasonId) === targetSeasonId)
})

const currentSeasonVideos = computed(() => {
  return displayedVideos.value.sort((a, b) => (a.episodeNumber || 0) - (b.episodeNumber || 0))
})

function getSeasonVideoCount(seasonId: number): number {
  return videos.value.filter(v => Number(v.seasonId) === Number(seasonId)).length
}

function getSeasonName(seasonId: number | null): string {
  if (seasonId === null) return '全部'
  const season = seasons.value.find(s => s.id === seasonId)
  return season ? season.name : ''
}

function getProgressPercent(video: Video): number {
  if (!video.currentPosition || !video.duration || video.duration === 0) return 0
  return Math.min(100, (video.currentPosition / video.duration) * 100)
}

function formatDuration(ms: number | undefined): string {
  if (!ms) return '--:--'
  const minutes = Math.floor(ms / 60000)
  if (minutes >= 60) {
    const h = Math.floor(minutes / 60)
    const m = minutes % 60
    return `${h}:${m.toString().padStart(2, '0')}`
  }
  return `${minutes} 分钟`
}

function goBack() {
  router.back()
}

function goToPlayer(uuid: string) {
  router.push(`/player/${uuid}`)
}

function playFirstUnwatched() {
  const firstUnwatched = videos.value.find(v => !v.currentPosition || v.currentPosition === 0)
  if (firstUnwatched) {
    goToPlayer(firstUnwatched.uuid)
  } else if (videos.value.length > 0) {
    goToPlayer(videos.value[0].uuid)
  }
}

function toggleFavorite() {
  isFavorite.value = !isFavorite.value
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
