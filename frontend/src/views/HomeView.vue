<template>
  <div class="min-h-screen bg-[#0A0F1E]">
    <div class="flex items-center justify-between mb-8 pt-4">
      <h1 class="text-2xl font-semibold text-white">全部视频</h1>
      <div class="flex items-center gap-4">
        <div class="text-sm text-gray-400">
          共 {{ total }} 个视频
        </div>
        <router-link to="/admin" class="px-4 py-2 bg-gray-800 text-gray-300 rounded-lg hover:bg-gray-700 transition-colors flex items-center gap-2">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
          </svg>
          管理后台
        </router-link>
      </div>
    </div>

    <div v-if="loading && videos.length === 0" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 2xl:grid-cols-8 gap-6">
      <SkeletonCard v-for="i in 12" :key="i" />
    </div>

    <div v-else-if="videos.length === 0" class="text-center py-24">
      <svg class="w-24 h-24 mx-auto text-gray-600 mb-4" fill="currentColor" viewBox="0 0 24 24">
        <path d="M4 4h16a2 2 0 012 2v12a2 2 0 01-2 2H4a2 2 0 01-2-2V6a2 2 0 012-2zm0 2v12h16V6H4zm2 2l8 4-8 4V8z"/>
      </svg>
      <p class="text-gray-400 text-lg mb-4">暂无视频</p>
      <router-link to="/upload" class="inline-flex items-center px-6 py-3 bg-primary text-white rounded-xl hover:bg-opacity-90 transition-all hover:scale-105">
        上传第一个视频
      </router-link>
    </div>

    <div v-else>
      <div class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 2xl:grid-cols-8 gap-6">
        <VideoCard v-for="video in videos" :key="video.uuid" :video="video" />
      </div>

      <div v-if="loading" class="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6 2xl:grid-cols-8 gap-6 mt-6">
        <SkeletonCard v-for="i in 6" :key="i" />
      </div>

      <div v-if="hasMore && !loading" class="text-center mt-10">
        <button 
          @click="loadMore" 
          class="px-8 py-3 bg-gray-800 text-gray-300 rounded-xl hover:bg-gray-700 transition-colors"
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