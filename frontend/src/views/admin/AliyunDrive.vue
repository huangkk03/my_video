<template>
  <div class="p-6">
    <div class="flex items-center justify-between mb-6">
      <h1 class="text-2xl font-semibold text-gray-800">AList 存储</h1>
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
    
    <div v-if="!alistConnected" class="bg-yellow-50 border border-yellow-200 rounded-xl p-4 mb-6 flex justify-between items-center">
      <div class="flex items-center gap-3">
        <svg class="w-6 h-6 text-yellow-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"/>
        </svg>
        <div>
          <p class="font-medium text-yellow-800">AList 未连接</p>
          <p class="text-sm text-yellow-600">请确保 AList 服务正常运行，并在系统设置中配置正确的地址和凭证</p>
        </div>
      </div>
      <router-link to="/admin/settings" class="px-4 py-2 bg-yellow-100 text-yellow-800 rounded-lg hover:bg-yellow-200 transition-colors text-sm font-medium">
        去设置
      </router-link>
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
          :disabled="!alistConnected || searching"
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
    
    <div v-if="activeTasks.length > 0" class="bg-white rounded-xl shadow-sm overflow-hidden mb-6">
      <div class="px-6 py-4 border-b border-gray-100 bg-gray-50 flex justify-between items-center">
        <h2 class="font-medium text-gray-800">当前导入与转码任务</h2>
        <router-link to="/admin/media" class="text-sm text-primary hover:underline">去媒体库查看</router-link>
      </div>
      <table class="w-full">
        <tbody class="divide-y divide-gray-200">
          <tr v-for="task in activeTasks" :key="task.taskId" class="hover:bg-gray-50">
            <td class="px-6 py-4">
              <div class="font-medium text-gray-800">{{ task.sourceName }}</div>
              <div class="text-xs text-gray-500 mt-1">{{ task.message || '处理中...' }}</div>
            </td>
            <td class="px-6 py-4 w-1/3">
              <div class="flex items-center gap-3">
                <div class="flex-1 h-2 bg-gray-200 rounded-full overflow-hidden">
                  <div class="h-full bg-blue-500 transition-all" :style="{ width: (task.progress || 0) + '%' }"></div>
                </div>
                <span class="text-xs text-gray-500 w-10">{{ task.progress || 0 }}%</span>
              </div>
            </td>
            <td class="px-6 py-4 text-right">
              <div class="flex items-center justify-end gap-2">
                <span class="px-2 py-1 text-xs rounded bg-blue-100 text-blue-700">
                  {{ getTaskStatusText(task.status) }}
                </span>
                <button 
                  @click="cancelTask(task.taskId)"
                  class="text-xs text-red-500 hover:text-red-700 hover:underline"
                >
                  取消
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <div class="flex items-center gap-2 mb-4 text-sm text-gray-600" v-if="alistConnected">
      <button @click="loadFiles('/')" class="hover:text-primary cursor-pointer">根目录</button>
      <template v-if="currentPath !== '/'">
        <span class="text-gray-400">/</span>
        <span class="text-gray-800">{{ currentPath }}</span>
        <button @click="goUp" class="ml-4 px-2 py-1 text-xs bg-gray-100 hover:bg-gray-200 rounded border">返回上级</button>
      </template>
    </div>

    <div v-if="loading" class="flex justify-center py-12">
      <svg class="w-8 h-8 text-primary animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
      </svg>
    </div>
    
    <div v-else-if="files.length > 0" class="bg-white rounded-xl shadow-sm overflow-hidden">
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
          <tr v-for="file in files" :key="file.path" class="hover:bg-gray-50">
            <td class="px-6 py-4">
              <div class="flex items-center gap-3 cursor-pointer" @click="file.isFolder ? loadFiles(file.path) : null">
                <div class="w-10 h-10 bg-gray-100 rounded flex items-center justify-center">
                  <svg v-if="file.isFolder" class="w-6 h-6 text-yellow-500" fill="currentColor" viewBox="0 0 24 24">
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
                {{ file.isFolder ? '文件夹' : '文件' }}
              </span>
            </td>
            <td class="px-6 py-4">
              <button 
                v-if="!file.isFolder"
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
import { ref, onMounted, onUnmounted } from 'vue'

interface CloudFile {
  name: string
  path: string
  isFolder: boolean
  size: number
  modified: string
}

interface ImportTask {
  taskId: string
  sourceName: string
  status: string
  progress: number
  message: string
}

const searchQuery = ref('')
const files = ref<CloudFile[]>([])
const activeTasks = ref<ImportTask[]>([])
const loading = ref(false)
const searching = ref(false)
const currentPath = ref('/')
const searchPerformed = ref(false)
const alistConnected = ref(true)
const connectionError = ref('')
const downloading = ref(false)
const downloadingName = ref('')

let taskTimer: number | null = null

const videoExtensions = ['.mp4', '.mkv', '.avi', '.mov', '.wmv', '.flv', '.webm']

function getTaskStatusText(status: string): string {
  switch (status) {
    case 'pending': return '等待中'
    case 'downloading': return '下载中'
    case 'scraping': return '刮削中'
    case 'transcoding': return '转码中'
    case 'failed': return '失败'
    default: return status
  }
}

async function fetchActiveTasks() {
  try {
    const res = await fetch('/api/cloud/tasks/active')
    if (res.ok) {
      activeTasks.value = await res.json()
    }
  } catch (e) {
    console.error('Failed to fetch tasks:', e)
  }
}

async function cancelTask(taskId: string) {
  if (!confirm('确定要取消此任务吗？')) return
  try {
    const res = await fetch(`/api/cloud/tasks/${taskId}/cancel`, { method: 'POST' })
    const data = await res.json()
    if (data.success) {
      fetchActiveTasks()
    } else {
      alert(data.message || '取消失败')
    }
  } catch (e) {
    alert('取消失败')
  }
}

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
    const res = await fetch(`/api/cloud/search?keyword=${encodeURIComponent(searchQuery.value)}`)
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
    const res = await fetch('/api/cloud/test')
    const data = await res.json()
    
    if (data.success) {
      connectionError.value = ''
      alistConnected.value = true
      await loadFiles()
      alert('连接成功')
    } else {
      connectionError.value = data.message || '连接失败'
      alistConnected.value = false
    }
  } catch (e: any) {
    connectionError.value = e.message || '连接失败'
    alistConnected.value = false
  }
}

async function loadFiles(path = '/') {
  loading.value = true
  currentPath.value = path
  try {
    const res = await fetch(`/api/cloud/files?path=${encodeURIComponent(path)}`)
    files.value = await res.json()
  } catch (e: any) {
    console.error('Failed to load files:', e)
  } finally {
    loading.value = false
  }
}

function goUp() {
  if (currentPath.value === '/') return
  const parts = currentPath.value.split('/').filter(Boolean)
  parts.pop()
  const parentPath = parts.length > 0 ? '/' + parts.join('/') : '/'
  loadFiles(parentPath)
}

async function refreshToken() {
  try {
    await fetch('/api/cloud/test')
    alert('Token 有效')
  } catch (e) {
    alert('Token 无效，请重新配置')
  }
}

async function downloadFile(file: CloudFile) {
  if (!confirm(`确定要下载 "${file.name}" 吗？\n下载完成后将自动转码`)) return
  
  downloading.value = true
  downloadingName.value = file.name
  
  try {
    const res = await fetch('/api/cloud/import', {
      method: 'POST',
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
      body: `fileName=${encodeURIComponent(file.name)}&filePath=${encodeURIComponent(file.path)}&fileSize=${file.size || 0}`
    })
    
    const data = await res.json()
    
    if (data.taskId) {
      alert('下载任务已创建，进度将在上方显示')
      fetchActiveTasks()
    } else if (data.message) {
      alert('下载失败: ' + data.message)
    }
  } catch (e: any) {
    alert('下载失败: ' + e.message)
  } finally {
    downloading.value = false
    downloadingName.value = ''
  }
}

onMounted(async () => {
  fetchActiveTasks()
  taskTimer = window.setInterval(fetchActiveTasks, 5000)
  
  try {
    const res = await fetch('/api/cloud/test')
    const data = await res.json()
    alistConnected.value = data.success === true
    if (!data.success) {
      connectionError.value = data.message || 'AList 连接失败'
    } else {
      await loadFiles()
    }
  } catch (e) {
    alistConnected.value = false
    connectionError.value = '无法连接到 AList'
  }
})

onUnmounted(() => {
  if (taskTimer) {
    clearInterval(taskTimer)
  }
})
</script>
