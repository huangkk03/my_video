import './style.css'

<template>
  <div class="min-h-screen bg-secondary">
    <header class="bg-white shadow-sm border-b border-gray-100">
      <nav class="container mx-auto px-4 py-4 flex items-center justify-between">
        <router-link to="/" class="text-2xl font-semibold text-primary flex items-center gap-2">
          <svg class="w-8 h-8" fill="currentColor" viewBox="0 0 24 24">
            <path d="M4 4h16a2 2 0 012 2v12a2 2 0 01-2 2H4a2 2 0 01-2-2V6a2 2 0 012-2zm0 2v12h16V6H4zm2 2l8 4-8 4V8z"/>
          </svg>
          MKV 视频平台
        </router-link>
        <div class="flex items-center gap-4">
          <router-link to="/" class="text-gray-600 hover:text-primary transition-colors">视频列表</router-link>
          <router-link to="/upload" class="px-4 py-2 bg-primary text-white rounded-lg hover:bg-opacity-90 transition-all">
            上传视频
          </router-link>
        </div>
      </nav>
    </header>
    <main class="container mx-auto px-4 py-8">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
</script>