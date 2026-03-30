<template>
  <div>
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-semibold text-gray-800">全部视频</h1>
      <div class="text-sm text-gray-500">
        共 {{ total }} 个视频
      </div>
    </div>

    <div v-if="loading && videos.length === 0" class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 2xl:grid-cols-6 gap-4">
      <SkeletonCard v-for="i in 12" :key="i" />
    </div>

    <div v-else-if="videos.length === 0" class="text-center py-16">
      <svg class="w-24 h-24 mx-auto text-gray-300 mb-4" fill="currentColor" viewBox="0 0 24 24">
        <path d="M4 4h16a2 2 0 012 2v12a2 2 0 01-2 2H4a2 2 0 01-2-2V6a2 2 0 012-2zm0 2v12h16V6H4zm2 2l8 4-8 4V8z"/>
      </svg>
      <p class="text-gray-500 text-lg mb-4">暂无视频</p>
      <router-link to="/upload" class="inline-flex items-center px-4 py-2 bg-primary text-white rounded-lg hover:bg-opacity-90 transition-all hover:scale-105">
        上传第一个视频
      </router-link>
    </div>

    <div v-else>
      <div class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 2xl:grid-cols-6 gap-4">
        <VideoCard v-for="video in videos" :key="video.uuid" :video="video" />
      </div>

      <div v-if="loading" class="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 2xl:grid-cols-6 gap-4 mt-4">
        <SkeletonCard v-for="i in 6" :key="i" />
      </div>

      <div v-if="hasMore && !loading" class="text-center mt-8">
        <button 
          @click="loadMore" 
          class="px-6 py-2 border border-gray-300 text-gray-600 rounded-lg hover:bg-gray-50 transition-colors"
        >
          加载更多
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { videoApi, type Video } from '../api/video'
import VideoCard from '../components/VideoCard.vue'
import SkeletonCard from '../components/SkeletonCard.vue'

const videos = ref<Video[]>([])
const loading = ref(false)
const page = ref(0)
const size = 20
const total = ref(0)
const hasMore = ref(true)

async function fetchVideos() {
  loading.value = true
  try {
    const res = await videoApi.getList(page.value, size)
    if (page.value === 0) {
      videos.value = res.content
    } else {
      videos.value.push(...res.content)
    }
    total.value = res.totalElements || res.content.length
    hasMore.value = res.content.length === size
  } catch (e) {
    console.error('Failed to fetch videos:', e)
  } finally {
    loading.value = false
  }
}

function loadMore() {
  page.value++
  fetchVideos()
}

onMounted(() => {
  fetchVideos()
})
</script>