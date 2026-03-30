<template>
  <div class="p-6">
    <h1 class="text-2xl font-semibold text-gray-800 mb-6">仪表盘</h1>
    
    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
      <div class="bg-white rounded-xl shadow-sm p-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-500">视频总数</p>
            <p class="text-3xl font-bold text-gray-800 mt-1">{{ stats.totalVideos }}</p>
          </div>
          <div class="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
            <svg class="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 4v16M17 4v16M3 8h4m10 0h4M3 12h18M3 16h4m10 0h4M4 20h16a1 1 0 001-1V5a1 1 0 00-1-1H4a1 1 0 00-1 1v14a1 1 0 001 1z"/>
            </svg>
          </div>
        </div>
      </div>
      
      <div class="bg-white rounded-xl shadow-sm p-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-500">转码中</p>
            <p class="text-3xl font-bold text-yellow-600 mt-1">{{ stats.transcoding }}</p>
          </div>
          <div class="w-12 h-12 bg-yellow-100 rounded-lg flex items-center justify-center">
            <svg class="w-6 h-6 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
            </svg>
          </div>
        </div>
      </div>
      
      <div class="bg-white rounded-xl shadow-sm p-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-500">已完成</p>
            <p class="text-3xl font-bold text-green-600 mt-1">{{ stats.completed }}</p>
          </div>
          <div class="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center">
            <svg class="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
          </div>
        </div>
      </div>
      
      <div class="bg-white rounded-xl shadow-sm p-6">
        <div class="flex items-center justify-between">
          <div>
            <p class="text-sm text-gray-500">存储使用</p>
            <p class="text-3xl font-bold text-purple-600 mt-1">{{ formatSize(stats.storageUsed) }}</p>
          </div>
          <div class="w-12 h-12 bg-purple-100 rounded-lg flex items-center justify-center">
            <svg class="w-6 h-6 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4"/>
            </svg>
          </div>
        </div>
      </div>
    </div>
    
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <div class="bg-white rounded-xl shadow-sm p-6">
        <h2 class="text-lg font-semibold text-gray-800 mb-4">最近上传</h2>
        <div class="space-y-4">
          <div v-for="video in recentVideos" :key="video.uuid" class="flex items-center gap-4 p-3 bg-gray-50 rounded-lg">
            <div class="w-16 h-12 bg-gray-200 rounded overflow-hidden flex-shrink-0">
              <img v-if="video.thumbnailPath" :src="video.thumbnailPath" class="w-full h-full object-cover" />
            </div>
            <div class="flex-1 min-w-0">
              <p class="font-medium text-gray-800 truncate">{{ video.title }}</p>
              <p class="text-sm text-gray-500">{{ formatDate(video.createdAt) }}</p>
            </div>
            <span :class="getStatusClass(video.status)" class="px-2 py-1 text-xs rounded">
              {{ getStatusText(video.status) }}
            </span>
          </div>
          <div v-if="recentVideos.length === 0" class="text-center text-gray-500 py-8">
            暂无最近上传
          </div>
        </div>
      </div>
      
      <div class="bg-white rounded-xl shadow-sm p-6">
        <h2 class="text-lg font-semibold text-gray-800 mb-4">系统状态</h2>
        <div class="space-y-4">
          <div class="flex items-center justify-between py-2 border-b border-gray-100">
            <span class="text-gray-600">数据库连接</span>
            <span class="flex items-center gap-2">
              <span class="w-2 h-2 bg-green-500 rounded-full"></span>
              <span class="text-green-600">正常</span>
            </span>
          </div>
          <div class="flex items-center justify-between py-2 border-b border-gray-100">
            <span class="text-gray-600">FFmpeg</span>
            <span class="flex items-center gap-2">
              <span class="w-2 h-2 bg-green-500 rounded-full"></span>
              <span class="text-green-600">可用</span>
            </span>
          </div>
          <div class="flex items-center justify-between py-2 border-b border-gray-100">
            <span class="text-gray-600">阿里云盘</span>
            <span class="flex items-center gap-2">
              <span class="w-2 h-2" :class="aliyunConnected ? 'bg-green-500' : 'bg-red-500'"></span>
              <span :class="aliyunConnected ? 'text-green-600' : 'text-red-600'">{{ aliyunConnected ? '已连接' : '未连接' }}</span>
            </span>
          </div>
          <div class="flex items-center justify-between py-2">
            <span class="text-gray-600">转码队列</span>
            <span class="text-gray-800">{{ stats.transcoding }} 个任务</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { videoApi, type Video } from '../../api/video'

const stats = ref({
  totalVideos: 0,
  transcoding: 0,
  completed: 0,
  failed: 0,
  storageUsed: 0
})

const recentVideos = ref<Video[]>([])
const aliyunConnected = ref(false)

function formatSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

function getStatusClass(status: string): string {
  switch (status) {
    case 'transcoding': return 'bg-yellow-100 text-yellow-700'
    case 'completed': return 'bg-green-100 text-green-700'
    case 'failed': return 'bg-red-100 text-red-700'
    default: return 'bg-gray-100 text-gray-700'
  }
}

function getStatusText(status: string): string {
  switch (status) {
    case 'transcoding': return '转码中'
    case 'completed': return '已完成'
    case 'failed': return '失败'
    default: return '待处理'
  }
}

async function fetchStats() {
  try {
    const res = await videoApi.getList(0, 100)
    const videos = res.content
    recentVideos.value = videos.slice(0, 5)
    
    stats.value.totalVideos = videos.length
    stats.value.transcoding = videos.filter(v => v.status === 'transcoding').length
    stats.value.completed = videos.filter(v => v.status === 'completed').length
    stats.value.failed = videos.filter(v => v.status === 'failed').length
    stats.value.storageUsed = videos.reduce((sum, v) => sum + (v.fileSize || 0), 0)
  } catch (e) {
    console.error('Failed to fetch stats:', e)
  }
}

async function testAliyun() {
  try {
    const res = await fetch('/api/videos/aliyun/test')
    const data = await res.json()
    aliyunConnected.value = data.success === true
  } catch (e) {
    aliyunConnected.value = false
  }
}

onMounted(() => {
  fetchStats()
  testAliyun()
})
</script>
