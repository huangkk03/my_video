<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-semibold text-gray-800">媒体库管理</h1>
      <div class="flex items-center gap-4">
        <input 
          v-model="searchQuery" 
          type="text" 
          placeholder="搜索视频..." 
          class="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
        />
        <select 
          v-model="statusFilter" 
          class="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
        >
          <option value="">全部状态</option>
          <option value="pending">待处理</option>
          <option value="transcoding">转码中</option>
          <option value="completed">已完成</option>
          <option value="failed">失败</option>
        </select>
      </div>
    </div>
    
    <div v-if="loading && videos.length === 0" class="bg-white rounded-xl shadow-sm overflow-hidden">
      <div class="p-8 text-center text-gray-500">加载中...</div>
    </div>
    
    <div v-else-if="videos.length === 0" class="bg-white rounded-xl shadow-sm p-8 text-center">
      <svg class="w-16 h-16 mx-auto text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 4v16M17 4v16M3 8h4m10 0h4M3 12h18M3 16h4m10 0h4M4 20h16a1 1 0 001-1V5a1 1 0 00-1-1H4a1 1 0 00-1 1v14a1 1 0 001 1z"/>
      </svg>
      <p class="text-gray-500">暂无视频</p>
    </div>
    
    <div v-else class="bg-white rounded-xl shadow-sm overflow-hidden">
      <table class="w-full">
        <thead class="bg-gray-50">
          <tr>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">封面</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">标题</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">状态</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">大小</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">时长</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">上传时间</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-200">
          <tr v-for="video in filteredVideos" :key="video.uuid" class="hover:bg-gray-50">
            <td class="px-6 py-4">
              <div class="w-20 h-14 bg-gray-200 rounded overflow-hidden">
                <img v-if="video.thumbnailPath" :src="video.thumbnailPath" class="w-full h-full object-cover" />
              </div>
            </td>
            <td class="px-6 py-4">
              <div class="font-medium text-gray-800">{{ video.title }}</div>
              <div class="text-sm text-gray-500">{{ video.originalFilename }}</div>
            </td>
            <td class="px-6 py-4">
              <div v-if="video.status === 'transcoding'" class="flex items-center gap-2">
                <div class="w-16 h-2 bg-gray-200 rounded-full overflow-hidden">
                  <div class="h-full bg-yellow-500 transition-all" :style="{ width: getProgress(video.uuid) + '%' }"></div>
                </div>
                <span class="text-xs text-gray-500">{{ getProgress(video.uuid) }}%</span>
              </div>
              <span :class="getStatusClass(video.status)" class="px-2 py-1 text-xs rounded">
                {{ getStatusText(video.status) }}
              </span>
            </td>
            <td class="px-6 py-4 text-gray-600">{{ formatSize(video.fileSize) }}</td>
            <td class="px-6 py-4 text-gray-600">{{ formatDuration(video.duration) }}</td>
            <td class="px-6 py-4 text-gray-600">{{ formatDate(video.createdAt) }}</td>
            <td class="px-6 py-4">
              <div class="flex items-center gap-2">
                <router-link 
                  :to="'/player/' + video.uuid" 
                  class="text-primary hover:underline text-sm"
                >
                  播放
                </router-link>
                <button 
                  @click="rescrapVideo(video.uuid)" 
                  class="text-blue-600 hover:text-blue-800 text-sm"
                  :disabled="video.status !== 'completed'"
                >
                  重新刮削
                </button>
                <button 
                  @click="deleteVideo(video.uuid)" 
                  class="text-red-600 hover:text-red-800 text-sm"
                >
                  删除
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      
      <div v-if="hasMore" class="p-4 text-center border-t">
        <button 
          @click="loadMore" 
          class="px-4 py-2 text-primary hover:underline"
        >
          加载更多
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { videoApi, type Video } from '../../api/video'

const videos = ref<Video[]>([])
const loading = ref(false)
const page = ref(0)
const size = 20
const hasMore = ref(true)
const searchQuery = ref('')
const statusFilter = ref('')
const progressMap = ref<Record<string, number>>({})

const filteredVideos = computed(() => {
  let result = videos.value
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    result = result.filter(v => v.title.toLowerCase().includes(q))
  }
  if (statusFilter.value) {
    result = result.filter(v => v.status === statusFilter.value)
  }
  return result
})

function formatSize(bytes: number): string {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

function formatDuration(ms: number): string {
  if (!ms) return '--'
  const seconds = Math.floor(ms / 1000)
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  if (h > 0) return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
  return `${m}:${s.toString().padStart(2, '0')}`
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

function getProgress(uuid: string): number {
  return progressMap.value[uuid] || 0
}

async function fetchVideos() {
  loading.value = true
  try {
    const res = await videoApi.getList(page.value, size)
    if (page.value === 0) {
      videos.value = res.content
    } else {
      videos.value.push(...res.content)
    }
    hasMore.value = res.content.length === size
    
    for (const video of res.content) {
      if (video.status === 'transcoding') {
        fetchProgress(video.uuid)
      }
    }
  } catch (e) {
    console.error('Failed to fetch videos:', e)
  } finally {
    loading.value = false
  }
}

async function fetchProgress(uuid: string) {
  try {
    const res = await fetch(`/api/videos/${uuid}/transcode-progress`)
    const data = await res.json()
    progressMap.value[uuid] = data.progress || 0
  } catch (e) {
    console.error('Failed to fetch progress:', e)
  }
}

function loadMore() {
  page.value++
  fetchVideos()
}

async function deleteVideo(uuid: string) {
  if (!confirm('确定要删除这个视频吗？')) return
  try {
    await videoApi.delete(uuid)
    videos.value = videos.value.filter(v => v.uuid !== uuid)
  } catch (e) {
    console.error('Failed to delete video:', e)
    alert('删除失败')
  }
}

async function rescrapVideo(uuid: string) {
  try {
    await fetch(`/api/videos/${uuid}/rescrap`, { method: 'POST' })
    alert('已触发重新刮削')
  } catch (e) {
    console.error('Failed to rescrap:', e)
  }
}

onMounted(() => {
  fetchVideos()
})
</script>
