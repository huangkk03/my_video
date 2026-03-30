<template>
  <div class="p-6">
    <h1 class="text-2xl font-semibold text-gray-800 mb-6">系统设置</h1>
    
    <div class="space-y-6">
      <div class="bg-white rounded-xl shadow-sm p-6">
        <h2 class="text-lg font-semibold text-gray-800 mb-4">阿里云盘配置</h2>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">Refresh Token</label>
            <div class="flex gap-2">
              <input 
                v-model="config.aliyundrive_refresh_token" 
                :type="showToken ? 'text' : 'password'"
                placeholder="请输入阿里云盘 Refresh Token"
                class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
              />
              <button 
                @click="showToken = !showToken"
                class="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
              >
                {{ showToken ? '隐藏' : '显示' }}
              </button>
            </div>
            <p class="mt-1 text-sm text-gray-500">在阿里云盘开放平台获取 Refresh Token</p>
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">根目录 ID</label>
            <input 
              v-model="config.aliyundrive_root_folder_id" 
              type="text"
              placeholder="root"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
        </div>
      </div>
      
      <div class="bg-white rounded-xl shadow-sm p-6">
        <h2 class="text-lg font-semibold text-gray-800 mb-4">TMDB API 配置</h2>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">API Key</label>
            <input 
              v-model="config.tmdb_api_key" 
              type="text"
              placeholder="请输入 TMDB API Key"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">语言</label>
            <select 
              v-model="config.tmdb_language"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="zh-CN">简体中文</option>
              <option value="en-US">English</option>
            </select>
          </div>
        </div>
      </div>
      
      <div class="bg-white rounded-xl shadow-sm p-6">
        <h2 class="text-lg font-semibold text-gray-800 mb-4">转码设置</h2>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">最大并发转码数</label>
            <input 
              v-model.number="config.transcode_max_concurrent" 
              type="number"
              min="1"
              max="10"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">视频质量 (CRF)</label>
            <select 
              v-model="config.transcode_quality"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <option value="18">高质量 (CRF 18)</option>
              <option value="23">标准 (CRF 23)</option>
              <option value="28">低质量 (CRF 28)</option>
            </select>
          </div>
        </div>
      </div>
      
      <div class="flex justify-end gap-4">
        <button 
          @click="loadConfig"
          class="px-6 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
        >
          重置
        </button>
        <button 
          @click="saveConfig"
          :disabled="saving"
          class="px-6 py-2 bg-primary text-white rounded-lg hover:bg-opacity-90 transition-colors disabled:opacity-50"
        >
          {{ saving ? '保存中...' : '保存配置' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'

const config = reactive({
  aliyundrive_refresh_token: '',
  aliyundrive_root_folder_id: 'root',
  tmdb_api_key: '',
  tmdb_language: 'zh-CN',
  transcode_max_concurrent: '2',
  transcode_quality: '23'
})

const showToken = ref(false)
const saving = ref(false)

async function loadConfig() {
  try {
    const res = await fetch('/api/admin/config')
    const data = await res.json()
    
    if (data.aliyundrive_refresh_token) config.aliyundrive_refresh_token = data.aliyundrive_refresh_token
    if (data.aliyundrive_root_folder_id) config.aliyundrive_root_folder_id = data.aliyundrive_root_folder_id
    if (data.tmdb_api_key) config.tmdb_api_key = data.tmdb_api_key
    if (data.tmdb_language) config.tmdb_language = data.tmdb_language
    if (data.transcode_max_concurrent) config.transcode_max_concurrent = data.transcode_max_concurrent
    if (data.transcode_quality) config.transcode_quality = data.transcode_quality
  } catch (e) {
    console.error('Failed to load config:', e)
  }
}

async function saveConfig() {
  saving.value = true
  try {
    const res = await fetch('/api/admin/config', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        aliyundrive_refresh_token: config.aliyundrive_refresh_token,
        aliyundrive_root_folder_id: config.aliyundrive_root_folder_id,
        tmdb_api_key: config.tmdb_api_key,
        tmdb_language: config.tmdb_language,
        transcode_max_concurrent: config.transcode_max_concurrent,
        transcode_quality: config.transcode_quality
      })
    })
    
    if (res.ok) {
      alert('配置保存成功')
    } else {
      alert('配置保存失败')
    }
  } catch (e) {
    alert('配置保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadConfig()
})
</script>
