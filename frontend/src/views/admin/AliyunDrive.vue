<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-semibold text-gray-800">阿里云盘</h1>
      <div class="flex items-center gap-4">
        <button 
          @click="testConnection" 
          class="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-2"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
          </svg>
          测试连接
        </button>
        <button 
          @click="refreshToken" 
          class="px-4 py-2 bg-primary text-white rounded-lg hover:bg-opacity-90 transition-colors flex items-center gap-2"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
          </svg>
          刷新 Token
        </button>
      </div>
    </div>
    
    <div v-if="!aliyunConfigured" class="bg-yellow-50 border border-yellow-200 rounded-xl p-4 mb-6">
      <div class="flex items-center gap-3">
        <svg class="w-6 h-6 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
        </svg>
        <div>
          <p class="font-medium text-yellow-800">阿里云盘未配置</p>
          <p class="text-sm text-yellow-600">请在系统设置中配置 Refresh Token</p>
        </div>
        <router-link to="/admin/settings" class="ml-auto text-primary hover:underline">
          去设置
        </router-link>
      </div>
    </div>
    
    <div class="bg-white rounded-xl shadow-sm p-4 mb-6">
      <div class="flex items-center gap-4">
        <input 
          v-model="searchQuery" 
          type="text" 
          placeholder="搜索云盘文件..." 
          class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
          @keyup.enter="searchFiles"
        />
        <button 
          @click="searchFiles" 
          :disabled="!aliyunConfigured || searching"
          class="px-6 py-2 bg-primary text-white rounded-lg hover:bg-opacity-90 transition-colors disabled:opacity-50"
        >
          {{ searching ? '搜索中...' : '搜索' }}
        </button>
      </div>
    </div>
    
    <div v-if="connectionError" class="bg-red-50 border border-red-200 rounded-xl p-4 mb-6">
      <div class="flex items-center gap-3">
        <svg class="w-6 h-6 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
        <div>
          <p class="font-medium text-red-800">连接失败</p>
          <p class="text-sm text-red-600">{{ connectionError }}</p>
        </div>
      </div>
    </div>
    
    <div v-if="downloading" class="bg-blue-50 border border-blue-200 rounded-xl p-4 mb-6">
      <div class="flex items-center gap-3">
        <svg class="w-6 h-6 text-blue-600 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
        </svg>
        <div>
          <p class="font-medium text-blue-800">正在下载: {{ downloadingName }}</p>
          <p class="text-sm text-blue-600">下载完成后将自动开始转码</p>
        </div>
      </div>
    </div>
    
    <div v-if="files.length > 0" class="bg-white rounded-xl shadow-sm overflow-hidden">
      <table class="w-full">
        <thead class="bg-gray-50">
          <tr>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">文件名</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">大小</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">类型</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase">操作</th>
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-200">
          <tr v-for="file in files" :key="file.id" class="hover:bg-gray-50">
            <td class="px-6 py-4">
              <div class="flex items-center gap-3">
                <div class="w-10 h-10 bg-gray-100 rounded flex items-center justify-center">
                  <svg v-if="file.type === 'folder'" class="w-6 h-6 text-yellow-500" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M10 4H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z"/>
                  </svg>
                  <svg v-else class="w-6 h-6 text-blue-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 4v16M17 4v16M3 8h4m10 0h4M3 12h18M3 16h4m10 0h4M4 20h16a1 1 0 001-1V5a1 1 0 00-1-1H4a1 1 0 00-1 1v14a1 1 0 001 1z"/>
                  </svg>
                </div>
                <span class="font-medium text-gray-800">{{ file.name }}</span>
              </div>
            </td>
            <td class="px-6 py-4 text-gray-600">{{ formatSize(file.size) }}</td>
            <td class="px-6 py-4">
              <span class="px-2 py-1 text-xs rounded bg-gray-100 text-gray-600">
                {{ file.category || file.type }}
              </span>
            </td>
            <td class="px-6 py-4">
              <button 
                v-if="isVideoFile(file.name)"
                @click="downloadFile(file)" 
                :disabled="downloading"
                class="text-primary hover:underline disabled:opacity-50"
              >
                下载并转码
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <div v-else-if="!searching && searchPerformed" class="bg-white rounded-xl shadow-sm p-8 text-center">
      <svg class="w-16 h-16 mx-auto text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
      </svg>
      <p class="text-gray-500">未找到相关文件</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'

interface AliyunFile {
  id: string
  name: string
  size: number
  type: string
  category: string
}

const searchQuery = ref('')
const files = ref<AliyunFile[]>([])
const searching = ref(false)
const searchPerformed = ref(false)
const aliyunConfigured = ref(true)
const connectionError = ref('')
const downloading = ref(false)
const downloadingName = ref('')

const videoExtensions = ['.mp4', '.mkv', '.avi', '.mov', '.wmv', '.flv', '.webm']

function isVideoFile(filename: string): boolean {
  const ext = filename.toLowerCase().slice(filename.lastIndexOf('.'))
  return videoExtensions.includes(ext)
}

function formatSize(bytes: number): string {
  if (!bytes) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

async function searchFiles() {
  if (!searchQuery.value.trim()) return
  
  searching.value = true
  searchPerformed.value = true
  connectionError.value = ''
  
  try {
    const res = await fetch(`/api/videos/aliyun/search?query=${encodeURIComponent(searchQuery.value)}`)
    const data = await res.json()
    
    if (Array.isArray(data)) {
      files.value = data
    } else if (data.error) {
      connectionError.value = data.error
      files.value = []
    }
  } catch (e: any) {
    connectionError.value = e.message || '搜索失败'
    files.value = []
  } finally {
    searching.value = false
  }
}

async function testConnection() {
  try {
    const res = await fetch('/api/videos/aliyun/test')
    const data = await res.json()
    
    if (data.success) {
      connectionError.value = ''
      alert('连接成功')
    } else {
      connectionError.value = data.message || '连接失败'
    }
  } catch (e: any) {
    connectionError.value = e.message || '连接失败'
  }
}

async function refreshToken() {
  try {
    await fetch('/api/videos/aliyun/refresh-token', { method: 'POST' })
    alert('Token 已刷新')
  } catch (e) {
    alert('刷新失败')
  }
}

async function downloadFile(file: AliyunFile) {
  if (!confirm(`确定要下载 "${file.name}" 吗？\n下载完成后将自动转码`)) return
  
  downloading.value = true
  downloadingName.value = file.name
  
  try {
    const res = await fetch('/api/videos/aliyun/download', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: `fileId=${encodeURIComponent(file.id)}&title=${encodeURIComponent(file.name)}`
    })
    
    const data = await res.json()
    
    if (data.uuid) {
      alert('下载任务已创建，请在媒体库中查看转码进度')
    } else if (data.error) {
      alert('下载失败: ' + data.error)
    }
  } catch (e: any) {
    alert('下载失败: ' + e.message)
  } finally {
    downloading.value = false
    downloadingName.value = ''
  }
}

onMounted(async () => {
  try {
    const res = await fetch('/api/videos/aliyun/test')
    const data = await res.json()
    aliyunConfigured.value = data.success === true
    if (!data.success) {
      connectionError.value = data.message || '请配置阿里云盘'
    }
  } catch (e) {
    aliyunConfigured.value = false
  }
})
</script>
