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
              placeholder="搜索视频..."
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
      <!-- 电影海报墙 -->
      <section v-if="activeTab === 'home' || activeTab === 'movies'" class="mb-10">
        <h2 class="text-white text-xl font-semibold mb-4">
          {{ activeTab === 'home' ? '最近更新' : '电影' }}
        </h2>
        <div v-if="loadingVideos" class="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-6 xl:grid-cols-8 gap-4">
          <div v-for="i in 16" :key="i" class="aspect-[2/3] rounded-xl bg-gray-800 animate-pulse" />
        </div>
        <div v-else-if="filteredMovies.length === 0" class="text-gray-400 py-12 text-center">
          暂无电影
        </div>
        <MovieGrid
          v-else
          :movies="filteredMovies"
          @movie-click="onMovieClick"
        />
      </section>

      <!-- 电视剧海报墙 -->
      <section v-if="(activeTab === 'home' || activeTab === 'tvshows') && filteredSeries.length > 0" class="mb-10">
        <h2 class="text-white text-xl font-semibold mb-4">电视剧</h2>
        <div v-if="loadingSeries" class="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-6 xl:grid-cols-8 gap-4">
          <div v-for="i in 8" :key="i" class="aspect-[2/3] rounded-xl bg-gray-800 animate-pulse" />
        </div>
        <MovieGrid
          v-else
          :movies="filteredSeries"
          @movie-click="onSeriesClick"
        />
      </section>

      <!-- 动漫海报墙 -->
      <section v-if="(activeTab === 'home' || activeTab === 'anime') && filteredAnime.length > 0" class="mb-10">
        <h2 class="text-white text-xl font-semibold mb-4">动漫</h2>
        <div v-if="loadingSeries" class="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-6 xl:grid-cols-8 gap-4">
          <div v-for="i in 8" :key="i" class="aspect-[2/3] rounded-xl bg-gray-800 animate-pulse" />
        </div>
        <MovieGrid
          v-else
          :movies="filteredAnime"
          @movie-click="onSeriesClick"
        />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import MovieGrid from '../components/MovieGrid.vue'
import { videoApi, type Video } from '../api/video'
import { seriesApi, type Series } from '../api/series'

const router = useRouter()
const searchQuery = ref('')
const activeTab = ref('home')
const loadingVideos = ref(true)
const loadingSeries = ref(true)
const videos = ref<Video[]>([])
const seriesList = ref<Series[]>([])

const tabs = [
  { id: 'home', name: '首页' },
  { id: 'movies', name: '电影' },
  { id: 'tvshows', name: '电视剧' },
  { id: 'anime', name: '动漫' },
]

interface MovieGridItem {
  id: string
  title: string
  posterUrl: string
}

const filteredMovies = computed((): MovieGridItem[] => {
  let result = videos.value.filter(v => !v.seriesId)
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(v => v.title.toLowerCase().includes(q))
  }
  return result.map(v => ({
    id: v.uuid,
    title: v.title,
    posterUrl: v.posterPath || v.thumbnailPath || '',
  }))
})

const filteredSeries = computed((): MovieGridItem[] => {
  let result = seriesList.value.filter(s => s.categoryId === 2)
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(s => s.name.toLowerCase().includes(q))
  }
  return result.map(s => ({
    id: String(s.id),
    title: s.name,
    posterUrl: s.posterPath || '',
  }))
})

const filteredAnime = computed((): MovieGridItem[] => {
  let result = seriesList.value.filter(s => s.categoryId === 3)
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(s => s.name.toLowerCase().includes(q))
  }
  return result.map(s => ({
    id: String(s.id),
    title: s.name,
    posterUrl: s.posterPath || '',
  }))
})

function onMovieClick(uuid: string) {
  router.push(`/movie/${uuid}`)
}

function onSeriesClick(id: string) {
  router.push(`/tv/${id}`)
}

async function fetchVideos() {
  try {
    loadingVideos.value = true
    const res = await videoApi.getList(0, 1000)
    videos.value = res.content || []
  } catch (e) {
    console.error('Failed to fetch videos:', e)
  } finally {
    loadingVideos.value = false
  }
}

async function fetchSeries() {
  try {
    loadingSeries.value = true
    const res = await seriesApi.getPage(0, 1000)
    seriesList.value = res.content || []
  } catch (e) {
    console.error('Failed to fetch series:', e)
  } finally {
    loadingSeries.value = false
  }
}

onMounted(() => {
  fetchVideos()
  fetchSeries()
})
</script>
