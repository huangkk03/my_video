<template>
  <div class="flex min-h-screen bg-gray-50">
    <aside class="w-64 bg-gray-900 text-white flex flex-col">
      <div class="p-4 border-b border-gray-800">
        <h1 class="text-xl font-bold">MKV 视频平台</h1>
        <p class="text-xs text-gray-400 mt-1">管理后台</p>
      </div>
      
      <nav class="flex-1 p-4 space-y-1">
        <router-link 
          to="/admin/dashboard" 
          class="flex items-center gap-3 px-4 py-3 rounded-lg transition-colors"
          :class="$route.path === '/admin/dashboard' ? 'bg-primary text-white' : 'text-gray-300 hover:bg-gray-800'"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/>
          </svg>
          仪表盘
        </router-link>
        
        <router-link 
          to="/admin/media" 
          class="flex items-center gap-3 px-4 py-3 rounded-lg transition-colors"
          :class="$route.path === '/admin/media' ? 'bg-primary text-white' : 'text-gray-300 hover:bg-gray-800'"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 4v16M17 4v16M3 8h4m10 0h4M3 12h18M3 16h4m10 0h4M4 20h16a1 1 0 001-1V5a1 1 0 00-1-1H4a1 1 0 00-1 1v14a1 1 0 001 1z"/>
          </svg>
          媒体库管理
        </router-link>
        
        <router-link 
          to="/admin/aliyun" 
          class="flex items-center gap-3 px-4 py-3 rounded-lg transition-colors"
          :class="$route.path === '/admin/aliyun' ? 'bg-primary text-white' : 'text-gray-300 hover:bg-gray-800'"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 15a4 4 0 004 4h9a5 5 0 10-.1-9.999 5.002 5.002 0 10-9.78 2.096A4.001 4.001 0 003 15z"/>
          </svg>
          阿里云盘
        </router-link>
        
        <router-link 
          to="/admin/settings" 
          class="flex items-center gap-3 px-4 py-3 rounded-lg transition-colors"
          :class="$route.path === '/admin/settings' ? 'bg-primary text-white' : 'text-gray-300 hover:bg-gray-800'"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z"/>
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
          </svg>
          系统设置
        </router-link>
      </nav>
      
      <div class="p-4 border-t border-gray-800">
        <router-link to="/" class="flex items-center gap-3 px-4 py-2 text-gray-300 hover:text-white transition-colors">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14"/>
          </svg>
          返回前台
        </router-link>
      </div>
    </aside>
    
    <main class="flex-1 overflow-auto">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
</script>
