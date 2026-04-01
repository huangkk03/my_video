<template>
  <div class="p-6">
    <h1 class="text-2xl font-semibold text-gray-800 mb-6">系统设置</h1>
    
    <div class="space-y-6">
      <div class="bg-white rounded-xl shadow-sm p-6">
        <h2 class="text-lg font-semibold text-gray-800 mb-4">AList 配置</h2>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">AList 地址</label>
            <input 
              v-model="config.alist_url" 
              type="text"
              placeholder="http://localhost:5244"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">用户名</label>
            <input 
              v-model="config.alist_username" 
              type="text"
              placeholder="admin"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-2">密码</label>
            <input 
              v-model="config.alist_password" 
              type="password"
              placeholder="AList 登录密码"
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
          
          <div class="pt-2">
            <button 
              @click="testTmdbConnection"
              :disabled="testing"
              class="px-4 py-2 bg-blue-50 text-blue-600 border border-blue-200 rounded-lg hover:bg-blue-100 transition-colors disabled:opacity-50"
            >
              {{ testing ? '测试中...' : '测试 TMDB 连接' }}
            </button>
            <p v-if="testResult" :class="testResult.success ? 'text-green-600' : 'text-red-600'" class="mt-2 text-sm">
              {{ testResult.message }}
            </p>
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
  alist_url: 'http://localhost:5244',
  alist_username: 'admin',
  alist_password: '',
  tmdb_api_key: '',
  tmdb_language: 'zh-CN',
  transcode_max_concurrent: '2',
  transcode_quality: '23'
})

const saving = ref(false)
const testing = ref(false)
const testResult = ref<any>(null)

async function testTmdbConnection() {
  testing.value = true
  testResult.value = null
  try {
    const res = await fetch('/api/admin/config/test-tmdb')
    const data = await res.json()
    testResult.value = data
  } catch (e: any) {
    testResult.value = { success: false, message: '请求失败: ' + e.message }
  } finally {
    testing.value = false
  }
}

async function loadConfig() {
  try {
    const res = await fetch('/api/admin/config')
    const data = await res.json()
    
    if (data.alist_url) config.alist_url = data.alist_url
    if (data.alist_username) config.alist_username = data.alist_username
    if (data.alist_password) config.alist_password = data.alist_password
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
        alist_url: config.alist_url,
        alist_username: config.alist_username,
        alist_password: config.alist_password,
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
