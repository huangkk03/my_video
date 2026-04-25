<template>
  <div class="share-search-panel">
    <div class="mb-4">
      <h2 class="text-lg font-semibold text-gray-800 mb-3">搜索网盘资源</h2>
      <div class="flex gap-2">
        <input
          v-model="searchKeyword"
          type="text"
          placeholder="输入电影名称搜索..."
          class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
          @keyup.enter="handleSearch"
        />
        <button
          @click="handleSearch"
          :disabled="searching || !searchKeyword.trim()"
          class="px-6 py-2 bg-primary text-white rounded-lg hover:bg-opacity-90 transition-colors disabled:opacity-50"
        >
          {{ searching ? '搜索中...' : '搜索' }}
        </button>
      </div>
    </div>

    <div v-if="searchError" class="mb-4 p-3 bg-red-50 border border-red-200 rounded-lg text-red-600 text-sm">
      {{ searchError }}
    </div>

    <div v-if="!searched && !searching" class="text-center py-12 text-gray-500">
      <svg class="w-16 h-16 mx-auto text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
      </svg>
      <p>输入电影名称开始搜索</p>
    </div>

    <div v-else-if="searching" class="text-center py-12">
      <svg class="w-8 h-8 mx-auto text-primary animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
      </svg>
      <p class="mt-2 text-gray-500">搜索中...</p>
    </div>

    <div v-else-if="searchResults.length === 0 && searched" class="text-center py-12 text-gray-500">
      <svg class="w-16 h-16 mx-auto text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.172 16.172a4 4 0 015.656 0M9 10h.01M15 10h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
      </svg>
      <p>未找到相关资源</p>
      <p class="text-sm mt-1">尝试其他关键词</p>
    </div>

    <div v-else class="space-y-3">
      <div class="text-sm text-gray-500 mb-2">
        搜索结果 (共 {{ searchResults.length }} 条)
      </div>
      <div
        v-for="(result, index) in searchResults"
        :key="index"
        class="bg-gray-50 rounded-lg p-4 hover:bg-gray-100 transition-colors"
      >
        <div class="flex items-start justify-between gap-3">
          <div class="flex-1 min-w-0">
            <a
              :href="result.url"
              target="_blank"
              class="text-primary font-medium hover:underline block truncate"
            >
              {{ result.title }}
            </a>
            <p class="text-xs text-gray-500 mt-1">
              来源: {{ result.source }}
            </p>
            <p v-if="result.snippet" class="text-xs text-gray-400 mt-1 line-clamp-2">
              {{ result.snippet }}
            </p>
          </div>
          <button
            @click="copyLink(result)"
            class="flex-shrink-0 px-3 py-1 text-sm bg-white border border-gray-300 rounded hover:bg-gray-50 transition-colors"
          >
            {{ copiedIndex === index ? '已复制' : '复制链接' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="searchResults.length > 0" class="mt-4 p-3 bg-yellow-50 border border-yellow-200 rounded-lg text-xs text-yellow-700">
      注：部分链接可能已失效或需要阿里云盘客户端下载
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

interface SearchResult {
  title: string
  url: string
  source: string
  snippet: string
}

const searchKeyword = ref('')
const searching = ref(false)
const searched = ref(false)
const searchResults = ref<SearchResult[]>([])
const searchError = ref('')
const copiedIndex = ref<number | null>(null)

async function handleSearch() {
  if (!searchKeyword.value.trim()) return

  searching.value = true
  searched.value = false
  searchError.value = ''
  searchResults.value = []

  try {
    const response = await fetch(
      `/api/share/search?keyword=${encodeURIComponent(searchKeyword.value.trim())}&limit=20`
    )
    const data = await response.json()

    if (data.success) {
      searchResults.value = data.results || []
    } else {
      searchError.value = data.message || '搜索失败'
    }
  } catch (e: any) {
    searchError.value = '搜索请求失败: ' + e.message
  } finally {
    searching.value = false
    searched.value = true
  }
}

async function copyLink(result: SearchResult) {
  try {
    await navigator.clipboard.writeText(result.url)
    copiedIndex.value = searchResults.value.indexOf(result)
    setTimeout(() => {
      copiedIndex.value = null
    }, 2000)
  } catch (e) {
    console.error('Failed to copy:', e)
    alert('复制失败，请手动复制链接')
  }
}
</script>
