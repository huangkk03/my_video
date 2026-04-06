<template>
  <div class="min-h-screen" style="background-color: #101010;">
    <!-- 沉浸式背景 -->
    <div class="fixed inset-0 -z-10">
      <img 
        :src="movie.backdropUrl || 'https://image.tmdb.org/t/p/original/8TUEj3W2fZf2Z4M9J8E3LQnK2j.jpg'" 
        class="w-full h-full object-cover blur-xl brightness-30 scale-105"
        style="transform: translateZ(-10px);"
      />
      <div class="absolute inset-0" style="background: linear-gradient(to bottom, transparent 0%, rgba(16,16,16,0.6) 50%, #101010 100%);"></div>
    </div>

    <!-- 返回按钮 -->
    <div class="sticky top-0 z-40 px-6 py-4" style="background-color: rgba(16,16,16,0.8); backdrop-filter: blur(10px);">
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

    <!-- 主内容 -->
    <main class="px-6 pb-12 max-w-screen-2xl mx-auto">
      <!-- 加载状态 -->
      <div v-if="loading" class="py-12 text-center">
        <div class="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin mx-auto"></div>
      </div>

      <!-- 视频不存在 -->
      <div v-else-if="!movie.uuid" class="py-24 text-center">
        <p class="text-gray-400">视频不存在</p>
      </div>

      <!-- 两列布局 -->
      <div v-else class="flex flex-col md:flex-row gap-8 pt-4">
        <!-- 左列：海报 (1/3) -->
        <div class="flex-shrink-0 mx-auto md:mx-0">
          <div class="w-64 md:w-80 aspect-[2/3] rounded-xl overflow-hidden shadow-2xl">
            <img 
              :src="movie.posterUrl || 'https://image.tmdb.org/t/p/w500/8TUEj3W2fZf2Z4M9J8E3LQnK2j.jpg'" 
              :alt="movie.title"
              class="w-full h-full object-cover"
            />
          </div>
        </div>

        <!-- 右列：信息 (2/3) -->
        <div class="flex-1 text-white">
          <!-- 标题 -->
          <h1 class="text-3xl md:text-4xl font-bold text-white">{{ movie.title }}</h1>
          <p class="text-xl text-gray-400 mt-1">{{ movie.titleCn }}</p>

          <!-- 操作行图标按钮 -->
          <div class="flex items-center gap-3 my-4">
            <!-- 播放按钮 -->
            <button 
              @click="playMovie"
              class="w-12 h-12 rounded-full border-2 border-white flex items-center justify-center hover:bg-white hover:text-black transition-colors"
            >
              <svg class="w-5 h-5 ml-0.5" fill="currentColor" viewBox="0 0 24 24">
                <path d="M8 5v14l11-7z"/>
              </svg>
            </button>
            <!-- 已观看 -->
            <button 
              @click="toggleWatched"
              class="w-12 h-12 rounded-full border-2 flex items-center justify-center transition-colors"
              :class="movie.isWatched ? 'border-green-500 text-green-500' : 'border-gray-400 text-gray-400 hover:border-white'"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
              </svg>
            </button>
            <!-- 收藏 -->
            <button 
              @click="toggleFavorite"
              class="w-12 h-12 rounded-full border-2 flex items-center justify-center transition-colors"
              :class="movie.isFavorite ? 'border-red-500 text-red-500' : 'border-gray-400 text-gray-400 hover:border-white'"
            >
              <svg 
                class="w-5 h-5" 
                :fill="movie.isFavorite ? 'currentColor' : 'none'" 
                stroke="currentColor" 
                viewBox="0 0 24 24"
              >
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
              </svg>
            </button>
            <!-- 更多 -->
            <button class="w-12 h-12 rounded-full border-2 border-gray-400 flex items-center justify-center text-gray-400 hover:border-white hover:text-white transition-colors">
              <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                <path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/>
              </svg>
            </button>
          </div>

          <!-- 元数据行 -->
          <div class="flex flex-wrap items-center gap-x-3 text-gray-300 text-sm">
            <span>{{ movie.year }}</span>
            <span class="text-gray-600">|</span>
            <span>{{ formatRuntime(movie.runtime) }}</span>
            <span class="text-gray-600">|</span>
            <span v-if="movie.maturityRating" class="px-1.5 py-0.5 border border-gray-500 rounded text-xs">{{ movie.maturityRating }}</span>
            <span v-if="movie.maturityRating" class="text-gray-600">|</span>
            <span v-if="movie.rating" class="text-yellow-400">⭐ {{ movie.rating.toFixed(1) }}</span>
            <span v-if="movie.rating" class="text-gray-600">|</span>
            <span v-if="movie.tomatoRating">🍅 {{ movie.tomatoRating }}</span>
            <span v-if="movie.tomatoRating" class="text-gray-600">|</span>
            <span>{{ movie.endTime }}</span>
          </div>

          <!-- 技术信息区 -->
          <div v-if="movie.videoInfo || movie.audioInfo || movie.subtitleInfo" class="bg-gray-800/60 rounded-lg p-4 my-4 space-y-2">
            <div v-if="movie.videoInfo" class="flex items-center gap-2 text-sm text-gray-300">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/>
              </svg>
              {{ movie.videoInfo }}
            </div>
            <div v-if="movie.audioInfo" class="flex items-center gap-2 text-sm text-gray-300">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15.536a5 5 0 001.414 1.414m2.828-9.9a9 9 0 012.728-2.728"/>
              </svg>
              {{ movie.audioInfo }}
            </div>
            <div v-if="movie.subtitleInfo" class="flex items-center gap-2 text-sm text-gray-300">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 8h10M7 12h4m1 8l-4-4H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-3l-4 4z"/>
              </svg>
              {{ movie.subtitleInfo }}
            </div>
          </div>

          <!-- Slogan -->
          <p v-if="movie.slogan" class="text-lg text-yellow-400 italic my-4">"{{ movie.slogan }}"</p>

          <!-- 简介 -->
          <p v-if="movie.overview" class="text-gray-300 leading-relaxed mb-4">{{ movie.overview }}</p>

          <!-- 类型标签 -->
          <div v-if="movie.genres.length > 0" class="flex flex-wrap gap-2 mb-4">
            <span 
              v-for="genre in movie.genres" 
              :key="genre"
              class="px-3 py-1 text-sm rounded-full text-gray-300"
              style="background-color: #2a2a2a;"
            >
              {{ genre }}
            </span>
          </div>

          <!-- 标签区 -->
          <div v-if="movie.tags.length > 0" class="flex flex-wrap gap-x-2">
            <span v-for="tag in movie.tags" :key="tag" class="text-sm text-gray-500">#{{ tag }}</span>
          </div>

          <!-- 导演信息 -->
          <div v-if="movie.director" class="mt-4 text-gray-300 text-sm">
            <span class="text-gray-500">导演：</span>
            <span class="text-white">{{ movie.director }}</span>
          </div>

          <!-- 演职人员区块 -->
          <section v-if="movie.cast.length > 0" class="mt-8">
            <h2 class="text-xl font-semibold text-white mb-4">演职人员</h2>
            <div class="flex gap-4 overflow-x-auto pb-4">
              <div 
                v-for="member in movie.cast" 
                :key="member.id"
                class="flex-shrink-0 text-center group cursor-pointer"
              >
                <!-- 圆形头像 -->
                <div class="w-20 h-20 rounded-full overflow-hidden border-2 border-transparent group-hover:border-white transition-all duration-300 mx-auto mb-2">
                  <img 
                    v-if="member.profileUrl"
                    :src="member.profileUrl" 
                    :alt="member.name"
                    class="w-full h-full object-cover"
                  />
                  <div v-else class="w-full h-full bg-gray-700 flex items-center justify-center">
                    <svg class="w-8 h-8 text-gray-500" fill="currentColor" viewBox="0 0 24 24">
                      <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
                    </svg>
                  </div>
                </div>
                <!-- 演员名 -->
                <p class="text-white text-sm font-medium truncate w-24">{{ member.name }}</p>
                <!-- 角色名 -->
                <p class="text-gray-400 text-xs truncate w-24">{{ member.character }}</p>
              </div>
            </div>
          </section>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { videoApi, type Video } from '../api/video'

const router = useRouter()
const route = useRoute()

interface Movie {
  uuid: string
  title: string
  titleCn: string
  backdropUrl: string
  posterUrl: string
  rating: number
  year: number
  runtime: number
  genres: string[]
  overview: string
  slogan: string
  tags: string[]
  videoInfo: string
  audioInfo: string
  subtitleInfo: string
  maturityRating: string
  tomatoRating: number
  endTime: string
  isWatched: boolean
  isFavorite: boolean
  director: string
  cast: Array<{ id: number; name: string; character: string; profileUrl: string }>
}

const movie = ref<Movie>({
  uuid: '',
  title: '',
  titleCn: '',
  backdropUrl: '',
  posterUrl: '',
  rating: 0,
  year: 0,
  runtime: 0,
  genres: [],
  overview: '',
  slogan: '',
  tags: [],
  videoInfo: '1080p',
  audioInfo: '',
  subtitleInfo: '',
  maturityRating: '',
  tomatoRating: 0,
  endTime: '',
  isWatched: false,
  isFavorite: false,
  director: '',
  cast: [],
})

const loading = ref(true)

function goBack() {
  router.back()
}

function playMovie() {
  if (movie.value.uuid) {
    router.push(`/player/${movie.value.uuid}`)
  }
}

function toggleFavorite() {
  movie.value.isFavorite = !movie.value.isFavorite
}

function toggleWatched() {
  movie.value.isWatched = !movie.value.isWatched
}

function formatRuntime(ms: number): string {
  if (!ms) return '-- 分钟'
  const totalMinutes = Math.floor(ms / 60000)
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  return hours > 0 ? `${hours}h ${minutes}m` : `${minutes} 分钟`
}

interface ActorJson {
  name: string
  character: string | null
  profilePath: string | null
  profileUrl: string | null
}

function parseActors(actorListJson: string | null): Array<{ id: number; name: string; character: string; profileUrl: string }> {
  if (!actorListJson) return []
  try {
    const actors: ActorJson[] = JSON.parse(actorListJson)
    return actors.map((actor, index) => ({
      id: index,
      name: actor.name || '',
      character: actor.character || '',
      profileUrl: actor.profileUrl || '',
    }))
  } catch {
    return []
  }
}

async function fetchVideo() {
  const uuid = route.params.id as string
  if (!uuid) {
    loading.value = false
    return
  }

  try {
    loading.value = true
    const video: Video = await videoApi.getByUuid(uuid)
    
    movie.value = {
      uuid: video.uuid,
      title: video.title || '',
      titleCn: video.title || '',
      backdropUrl: video.backdropPath || '',
      posterUrl: video.posterPath || video.thumbnailPath || '',
      rating: video.rating || 0,
      year: video.releaseYear || 0,
      runtime: video.duration || 0,
      genres: video.genres ? video.genres.split(',').map(g => g.trim()) : [],
      overview: video.overview || '',
      slogan: video.overview ? video.overview.split('。')[0] + '。' : '',
      tags: [],
      videoInfo: '1080p',
      audioInfo: '',
      subtitleInfo: '',
      maturityRating: '',
      tomatoRating: 0,
      endTime: formatRuntime(video.duration || 0),
      isWatched: (video.currentPosition || 0) > 0,
      isFavorite: false,
      director: (video as any).director || '',
      cast: parseActors((video as any).actorListJson || null),
    }
  } catch (e) {
    console.error('Failed to fetch video:', e)
    movie.value.uuid = ''
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchVideo()
})
</script>
