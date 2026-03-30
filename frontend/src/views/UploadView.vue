<template>
  <div class="max-w-2xl mx-auto py-8">
    <div class="flex items-center gap-4 mb-6">
      <router-link to="/" class="text-gray-400 hover:text-white">
        <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
        </svg>
      </router-link>
      <h1 class="text-2xl font-semibold text-white">上传视频</h1>
    </div>

    <div
      class="border-2 border-dashed border-gray-700 rounded-xl p-8 text-center transition-colors bg-gray-800/50"
      :class="{ 'border-primary bg-primary/10': isDragging }"
      @dragover.prevent="isDragging = true"
      @dragleave.prevent="isDragging = false"
      @drop.prevent="handleDrop"
    >
      <input
        ref="fileInput"
        type="file"
        accept="video/*"
        class="hidden"
        @change="handleFileSelect"
      />
      
      <div v-if="!selectedFile" class="space-y-4">
        <svg class="w-16 h-16 mx-auto text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/>
        </svg>
        <div>
          <p class="text-gray-300 mb-2">拖拽视频文件到此处，或</p>
          <button 
            @click="fileInput?.click()"
            class="px-6 py-2 bg-primary text-white rounded-lg hover:bg-opacity-90 transition-all"
          >
            选择文件
          </button>
        </div>
        <p class="text-sm text-gray-500">支持 MKV, MP4, AVI, MOV 格式</p>
      </div>

      <div v-else class="space-y-4">
        <div class="flex items-center justify-center gap-3">
          <svg class="w-12 h-12 text-primary" fill="currentColor" viewBox="0 0 24 24">
            <path d="M4 4h16a2 2 0 012 2v12a2 2 0 01-2 2H4a2 2 0 01-2-2V6a2 2 0 012-2zm0 2v12h16V6H4zm2 2l8 4-8 4V8z"/>
          </svg>
          <div class="text-left">
            <p class="font-medium text-white">{{ selectedFile.name }}</p>
            <p class="text-sm text-gray-400">{{ formatSize(selectedFile.size) }}</p>
          </div>
          <button @click="clearFile" class="ml-4 text-gray-400 hover:text-red-400">
            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
            </svg>
          </button>
        </div>

        <div v-if="uploadProgress > 0 && uploadProgress < 100" class="w-full bg-gray-700 rounded-full h-2">
          <div 
            class="bg-primary h-2 rounded-full transition-all"
            :style="{ width: uploadProgress + '%' }"
          />
        </div>

        <div v-if="uploading" class="text-gray-400">
          上传中... {{ uploadProgress }}%
        </div>

        <div v-if="uploadResult" :class="uploadResult.success ? 'text-green-400' : 'text-red-400'">
          {{ uploadResult.message }}
        </div>

        <div class="space-y-3">
          <input
            v-model="videoTitle"
            type="text"
            placeholder="输入视频标题（可选）"
            class="w-full px-4 py-2 bg-gray-700 border border-gray-600 text-white rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
          />

          <button
            @click="upload"
            :disabled="uploading"
            class="w-full px-4 py-2 bg-primary text-white rounded-lg hover:bg-opacity-90 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {{ uploading ? '上传中...' : '开始上传' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="uploadedUuid" class="mt-6 p-4 bg-green-900/30 border border-green-700 rounded-lg">
      <p class="text-green-400 mb-3">上传成功！视频正在转码中...</p>
      <div class="flex gap-3">
        <router-link 
          to="/"
          class="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-all"
        >
          返回列表
        </router-link>
        <router-link 
          :to="`/player/${uploadedUuid}`"
          class="px-4 py-2 border border-green-500 text-green-400 rounded-lg hover:bg-green-900/30 transition-all"
        >
          查看详情
        </router-link>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { videoApi } from '../api/video'

const fileInput = ref<HTMLInputElement>()
const selectedFile = ref<File | null>(null)
const videoTitle = ref('')
const isDragging = ref(false)
const uploading = ref(false)
const uploadProgress = ref(0)
const uploadResult = ref<{ success: boolean; message: string } | null>(null)
const uploadedUuid = ref('')

function handleFileSelect(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files?.[0]) {
    selectedFile.value = input.files[0]
    uploadResult.value = null
  }
}

function handleDrop(e: DragEvent) {
  isDragging.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file && file.type.startsWith('video/')) {
    selectedFile.value = file
    uploadResult.value = null
  }
}

function clearFile() {
  selectedFile.value = null
  fileInput.value!.value = ''
  uploadResult.value = null
  uploadProgress.value = 0
}

function formatSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

async function upload() {
  if (!selectedFile.value) return
  
  uploading.value = true
  uploadProgress.value = 0
  uploadResult.value = null
  
  try {
    const res = await videoApi.upload(selectedFile.value, videoTitle.value || undefined)
    uploadedUuid.value = res.uuid
    uploadResult.value = { success: true, message: res.message }
  } catch (e: any) {
    uploadResult.value = { 
      success: false, 
      message: e.response?.data?.message || '上传失败，请重试' 
    }
  } finally {
    uploading.value = false
  }
}
</script>