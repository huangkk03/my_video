<template>
  <div class="min-h-screen bg-[#0A0F1E]">
    <div class="flex items-center justify-between mb-6 pt-4">
      <div class="flex items-center gap-6">
        <h1 class="text-2xl font-semibold text-white">媒体库</h1>
        <div class="flex gap-2">
          <button 
            @click="activeTab = 'poster'"
            :class="[
              'px-4 py-2 rounded-lg font-medium transition-colors',
              activeTab === 'poster' 
                ? 'bg-primary text-white' 
                : 'bg-gray-800 text-gray-400 hover:text-white'
            ]"
          >
            海报墙
          </button>
          <button 
            @click="activeTab = 'all'"
            :class="[
              'px-4 py-2 rounded-lg font-medium transition-colors',
              activeTab === 'all' 
                ? 'bg-primary text-white' 
                : 'bg-gray-800 text-gray-400 hover:text-white'
            ]"
          >
            全部视频
          </button>
        </div>
      </div>
      <router-link to="/admin" class="px-4 py-2 bg-gray-800 text-gray-300 rounded-lg hover:bg-gray-700 transition-colors flex items-center gap-2">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
        </svg>
        管理后台
      </router-link>
    </div>

    <!-- Poster Wall Tab -->
    <div v-if="activeTab === 'poster'">
      <div v-if="loadingSeries && seriesList.length === 0" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 2xl:grid-cols-8 gap-6">
        <div v-for="i in 12" :key="i" class="aspect-[2/3] bg-gray-800 rounded-xl animate-pulse"></div>
      </div>

      <div v-else-if="seriesList.length === 0" class="text-center py-24">
        <svg class="w-24 h-24 mx-auto text-gray-600 mb-4" fill="currentColor" viewBox="0 0 24 24">
          <path d="M4 4h16a2 2 0 012 2v12a2 2 0 01-2 2H4a2 2 0 01-2-2V6a2 2 0 012-2zm0 2v12h16V6H4zm2 2l8 4-8 4V8z"/>
        </svg>
        <p class="text-gray-400 text-lg mb-4">暂无系列</p>
        <router-link to="/admin/media" class="inline-flex items-center px-6 py-3 bg-primary text-white rounded-xl hover:bg-opacity-90 transition-all hover:scale-105">
          去后台创建系列
        </router-link>
      </div>

      <div v-else>
        <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 2xl:grid-cols-8 gap-6">
          <div 
            v-for="s in seriesList" 
            :key="s.id"
            @click="goToSeries(s.id)"
            class="group cursor-pointer"
          >
            <div class="aspect-[2/3] rounded-xl overflow-hidden relative">
              <img 
                v-if="s.posterPath" 
                :src="s.posterPath" 
                :alt="s.name"
                class="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
              />
              <div v-else class="w-full h-full bg-gray-800 flex items-center justify-center">
                <svg class="w-12 h-12 text-gray-600" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M4 4h16a2 2 0 012 2v12a2 2 0 01-2 2H4a2 2 0 01-2-2V6a2 2 0 012-2z"/>
                </svg>
              </div>
              <div class="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300">
                <div class="absolute bottom-0 left-0 right-0 p-3">
                  <p class="text-white font-medium text-sm truncate">{{ s.name }}</p>
                  <p class="text-gray-300 text-xs">{{ getSeriesVideoCount(s.id) }} 集</p>
                </div>
              </div>
            </div>
            <p class="text-gray-300 text-sm mt-2 truncate group-hover:text-white transition-colors">{{ s.name }}</p>
          </div>
        </div>

        <div v-if="loadingSeries" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 2xl:grid-cols-8 gap-6 mt-6">
          <div v-for="i in 6" :key="i" class="aspect-[2/3] bg-gray-800 rounded-xl animate-pulse"></div>
        </div>

        <div v-if="hasMoreSeries && !loadingSeries" class="text-center mt-10">
          <button 
            @click="loadMoreSeries" 
            class="px-8 py-3 bg-gray-800 text-gray-300 rounded-xl hover:bg-gray-700 transition-colors"
          >
            加载更多
          </button>
        </div>
      </div>
    </div>

    <!-- All Videos Tab -->
    <div v-if="activeTab === 'all'">
      <div class="flex items-center gap-4 mb-6">
        <div class="text-sm text-gray-400">
          共 {{ total }} 个视频
        </div>
      </div>

      <div v-if="loadingVideos && videos.length === 0" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 2xl:grid-cols-8 gap-6">
        <SkeletonCard v-for="i in 12" :key="i" />
      </div>

      <div v-else-if="videos.length === 0" class="text-center py-24">
        <svg class="w-24 h-24 mx-auto text-gray-600 mb-4" fill="currentColor" viewBox="0 0 24 24">
          <path d="M4 4h16a2 2 0 012 2v12a2 2 0 01-2 2H4a2 2 0 01-2-2V6a2 2 0 012-2zm0 2v12h16V6H4zm2 2l8 4-8 4V8z"/>
        </svg>
        <p class="text-gray-400 text-lg mb-4">暂无视频</p>
        <router-link to="/admin/media" class="inline-flex items-center px-6 py-3 bg-primary text-white rounded-xl hover:bg-opacity-90 transition-all hover:scale-105">
          上传第一个视频
        </router-link>
      </div>

      <div v-else>
        <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 2xl:grid-cols-8 gap-6">
          <VideoCard v-for="video in videos" :key="video.uuid" :video="video" />
        </div>

        <div v-if="loadingVideos" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 2xl:grid-cols-8 gap-6 mt-6">
          <SkeletonCard v-for="i in 6" :key="i" />
        </div>

        <div v-if="hasMoreVideos && !loadingVideos" class="text-center mt-10">
          <button 
            @click="loadMoreVideos" 
            class="px-8 py-3 bg-gray-800 text-gray-300 rounded-xl hover:bg-gray-700 transition-colors"
          >
            加载更多
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { videoApi, type Video } from '../api/video'
import { seriesApi, type Series } from '../api/series'
import VideoCard from '../components/VideoCard.vue'
import SkeletonCard from '../components/SkeletonCard.vue'

const router = useRouter()
const activeTab = ref('poster')

const videos = ref<Video[]>([])
const seriesList = ref<Series[]>([])
const seriesVideoCounts = ref<Record<number, number>>({})
const loadingVideos = ref(false)
const loadingSeries = ref(false)
const pageVideos = ref(0)
const pageSeries = ref(0)
const size = 20
const total = ref(0)
const hasMoreVideos = ref(true)
const hasMoreSeries = ref(true)

async function fetchVideos() {
  loadingVideos.value = true
  try {
    const res = await videoApi.getList(pageVideos.value, size)
    if (pageVideos.value === 0) {
      videos.value = res.content
    } else {
      videos.value.push(...res.content)
    }
    total.value = res.totalElements || res.content.length
    hasMoreVideos.value = res.content.length === size
  } catch (e) {
    console.error('Failed to fetch videos:', e)
  } finally {
    loadingVideos.value = false
  }
}

async function fetchSeries() {
  loadingSeries.value = true
  try {
    const res = await seriesApi.getPage(pageSeries.value, size)
    if (pageSeries.value === 0) {
      seriesList.value = res.content
    } else {
      seriesList.value.push(...res.content)
    }
    for (const s of res.content) {
      fetchSeriesDetail(s.id)
    }
    hasMoreSeries.value = res.content.length === size
  } catch (e) {
    console.error('Failed to fetch series:', e)
  } finally {
    loadingSeries.value = false
  }
}

async function fetchSeriesDetail(id: number) {
  try {
    const detail = await seriesApi.getDetail(id)
    seriesVideoCounts.value[id] = detail.videoCount
  } catch (e) {
    console.error('Failed to fetch series detail:', e)
  }
}

function getSeriesVideoCount(id: number): number {
  return seriesVideoCounts.value[id] || 0
}

function loadMoreVideos() {
  pageVideos.value++
  fetchVideos()
}

function loadMoreSeries() {
  pageSeries.value++
  fetchSeries()
}

function goToSeries(id: number) {
  router.push(`/series/${id}`)
}

onMounted(() => {
  fetchSeries()
  fetchVideos()
})
</script>
