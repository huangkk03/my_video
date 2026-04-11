<template>
  <div class="p-6 flex gap-6 h-full">
    <!-- Folder Tree Sidebar -->
    <div class="w-64 flex-shrink-0 bg-white rounded-xl shadow-sm p-4 overflow-y-auto">
      <div class="flex items-center justify-between mb-4">
        <h2 class="font-semibold text-gray-800">文件夹</h2>
        <button
          @click="showCreateFolderModal = true"
          class="p-1 text-primary hover:bg-primary/10 rounded"
          title="创建文件夹"
        >
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
        </button>
      </div>

      <!-- All Videos option -->
      <div
        @click="selectFolder(null)"
        :class="[
          'px-3 py-2 rounded-lg cursor-pointer flex items-center justify-between mb-1',
          selectedFolderId === null ? 'bg-primary/10 text-primary' : 'hover:bg-gray-100'
        ]"
      >
        <span class="flex items-center gap-2">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"/>
          </svg>
          全部视频
        </span>
      </div>

      <!-- Ungrouped option -->
      <div
        @click="selectFolder('ungrouped')"
        :class="[
          'px-3 py-2 rounded-lg cursor-pointer flex items-center justify-between mb-1',
          selectedFolderId === 'ungrouped' ? 'bg-primary/10 text-primary' : 'hover:bg-gray-100'
        ]"
      >
        <span class="flex items-center gap-2">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 8h14M5 8a2 2 0 110-4h14a2 2 0 110 4M5 8v10a2 2 0 002 2h10a2 2 0 002-2V8m-9 4h4"/>
          </svg>
          未分类
        </span>
        <span class="text-xs bg-gray-200 px-2 py-0.5 rounded">{{ ungroupedCount }}</span>
      </div>

      <div class="border-t my-2"></div>

      <!-- Folder tree -->
      <div v-for="folder in folderTree" :key="folder.id" class="folder-item">
        <div
          @click="selectFolder(folder.id)"
          :class="[
            'px-3 py-2 rounded-lg cursor-pointer flex items-center justify-between',
            selectedFolderId === folder.id ? 'bg-primary/10 text-primary' : 'hover:bg-gray-100'
          ]"
        >
          <span class="flex items-center gap-2 truncate">
            <svg class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"/>
            </svg>
            <span class="truncate">{{ folder.name }}</span>
          </span>
          <div class="flex items-center gap-1">
            <span class="text-xs bg-gray-200 px-2 py-0.5 rounded">{{ folder.videoCount }}</span>
            <button
              @click.stop="deleteFolderConfirm(folder)"
              class="p-1 text-gray-400 hover:text-red-500 rounded"
            >
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
              </svg>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Main Content -->
    <div class="flex-1 min-w-0">
      <div class="flex items-center justify-between mb-6">
        <h1 class="text-2xl font-semibold text-gray-800">媒体库管理</h1>
        <div class="flex items-center gap-4">
          <div class="flex gap-2">
            <button
              @click="activeTab = 'videos'"
              :class="[
                'px-4 py-2 rounded-lg font-medium transition-colors',
                activeTab === 'videos'
                  ? 'bg-primary text-white'
                  : 'bg-gray-200 text-gray-600 hover:bg-gray-300'
              ]"
            >
              视频列表
            </button>
            <button
              @click="activeTab = 'series'"
              :class="[
                'px-4 py-2 rounded-lg font-medium transition-colors',
                activeTab === 'series'
                  ? 'bg-primary text-white'
                  : 'bg-gray-200 text-gray-600 hover:bg-gray-300'
              ]"
            >
              系列管理
            </button>
          </div>
        </div>
      </div>

      <!-- Video List Tab -->
      <div v-if="activeTab === 'videos'" class="space-y-6">
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

        <div class="bg-white rounded-xl shadow-sm p-4 border">
        <div class="flex flex-wrap items-center gap-3">
          <span class="text-sm text-gray-600">已选 {{ selectedVideoUuids.length }} 项</span>
          <select
            v-model="batchSeriesId"
            class="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
          >
            <option :value="null">选择系列</option>
            <option v-for="s in seriesList" :key="s.id" :value="s.id">{{ s.name }}</option>
          </select>
          <select
            v-model="batchSeasonId"
            :disabled="!batchSeriesId"
            class="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary disabled:opacity-50"
          >
            <option :value="null">不指定季度</option>
            <option v-for="season in batchSeasons" :key="season.id" :value="season.id">
              {{ season.name }}
            </option>
          </select>
          <input
            v-model.number="batchEpisodeStart"
            type="number"
            min="1"
            placeholder="起始集号(可选)"
            class="w-40 px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
          />
          <button
            @click="batchAssignSelectedVideos"
            :disabled="assigningBatch || selectedVideoUuids.length === 0 || !batchSeriesId"
            class="px-4 py-2 bg-primary text-white rounded-lg hover:bg-opacity-90 disabled:opacity-50"
          >
            {{ assigningBatch ? '归档中...' : '批量归档到系列/季度' }}
          </button>
          <select
            v-model="batchMoveFolderId"
            :disabled="selectedVideoUuids.length === 0"
            class="px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary disabled:opacity-50"
          >
            <option :value="null">移动到文件夹...</option>
            <option v-for="f in folderTree" :key="f.id" :value="f.id">
              {{ f.name }}
            </option>
          </select>
          <button
            @click="executeMoveToFolder"
            :disabled="movingToFolder || selectedVideoUuids.length === 0 || batchMoveFolderId === null"
            class="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-opacity-90 disabled:opacity-50"
          >
            {{ movingToFolder ? '移动中...' : '确认移动' }}
          </button>
          <button
            @click="clearBatchSelection"
            :disabled="selectedVideoUuids.length === 0"
            class="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 disabled:opacity-50"
          >
            清空选择
          </button>
        </div>
      </div>

      <div
        class="border-2 border-dashed border-gray-300 rounded-xl p-8 text-center transition-colors mb-6"
        :class="{ 'border-primary bg-primary/5': isDragging }"
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
          <svg class="w-16 h-16 mx-auto text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"/>
          </svg>
          <div>
            <p class="text-gray-600 mb-2">拖拽视频文件到此处，或</p>
            <button 
              @click="triggerUpload"
              class="px-6 py-2 bg-primary text-white rounded-lg hover:bg-opacity-90 transition-all"
            >
              选择文件
            </button>
          </div>
          <p class="text-sm text-gray-400">支持 MKV, MP4, AVI, MOV 格式</p>
        </div>

        <div v-else class="space-y-4">
          <div class="flex items-center justify-center gap-3">
            <svg class="w-12 h-12 text-primary" fill="currentColor" viewBox="0 0 24 24">
              <path d="M4 4h16a2 2 0 012 2v12a2 2 0 01-2 2H4a2 2 0 01-2-2V6a2 2 0 012-2zm0 2v12h16V6H4zm2 2l8 4-8 4V8z"/>
            </svg>
            <div class="text-left">
              <p class="font-medium text-gray-800">{{ selectedFile.name }}</p>
              <p class="text-sm text-gray-500">{{ formatSize(selectedFile.size) }}</p>
            </div>
            <button @click="clearFile" class="ml-4 text-gray-400 hover:text-red-500">
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
              </svg>
            </button>
          </div>

          <div v-if="uploadProgress > 0 && uploadProgress < 100" class="w-full bg-gray-200 rounded-full h-2 max-w-md mx-auto">
            <div 
              class="bg-primary h-2 rounded-full transition-all"
              :style="{ width: uploadProgress + '%' }"
            />
          </div>

          <div v-if="uploading" class="text-gray-500">
            上传中... {{ uploadProgress }}%
          </div>

          <div v-if="uploadResult" :class="uploadResult.success ? 'text-green-600' : 'text-red-500'" class="font-medium">
            {{ uploadResult.message }}
          </div>

          <div class="space-y-3 max-w-md mx-auto">
            <input
              v-model="videoTitle"
              type="text"
              placeholder="输入视频标题（可选）"
              class="w-full px-4 py-2 bg-white border border-gray-300 text-gray-800 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
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
      
      <div v-if="activeTasks.length > 0" class="bg-white rounded-xl shadow-sm overflow-hidden mb-6">
        <div class="px-6 py-4 border-b border-gray-100 bg-gray-50">
          <h2 class="font-medium text-gray-800">云盘导入任务</h2>
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
            <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">
              <input type="checkbox" :checked="allVisibleSelected" @change="onSelectAllChange" />
            </th>
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
            <td class="px-4 py-4">
              <input type="checkbox" :checked="isVideoSelected(video.uuid)" @change="onVideoSelectChange($event, video.uuid)" />
            </td>
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
                <button 
                  @click="editVideo(video)" 
                  class="text-blue-600 hover:text-blue-800 text-sm"
                >
                  编辑
                </button>
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

    <!-- 编辑视频对话框 -->
    <div v-if="showEditDialog" class="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div class="bg-white rounded-lg p-6 w-full max-w-md">
        <h3 class="text-lg font-semibold mb-4">编辑视频信息</h3>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">标题</label>
            <input v-model="editVideoForm.title" type="text" 
                   class="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary" />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">年代</label>
            <input v-model.number="editVideoForm.releaseYear" type="number" 
                   class="w-full border border-gray-300 rounded-md px-3 py-2 focus:outline-none focus:ring-2 focus:ring-primary"
                   min="1900" max="2099" />
          </div>
        </div>
        <div class="mt-6 flex justify-end space-x-3">
          <button @click="showEditDialog = false" 
                  class="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50">
            取消
          </button>
          <button @click="saveVideoEdit" 
                  class="px-4 py-2 bg-primary text-white rounded-md hover:bg-opacity-90">
            保存
          </button>
        </div>
      </div>
    </div>

    <!-- Series Management Tab -->
    <div v-if="activeTab === 'series'" class="space-y-6">
      <div class="flex justify-between items-center">
        <div class="flex items-center gap-4">
          <input 
            v-model="seriesSearchQuery" 
            type="text" 
            placeholder="搜索系列..." 
            class="px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
          />
        </div>
        <button 
          @click="showCreateSeriesModal = true"
          class="px-4 py-2 bg-primary text-white rounded-lg hover:bg-opacity-90 transition-colors"
        >
          创建系列
        </button>
      </div>

      <div v-if="loadingSeries && seriesList.length === 0" class="text-center py-12 text-gray-500">
        加载中...
      </div>

      <div v-else-if="seriesList.length === 0" class="text-center py-12">
        <p class="text-gray-500 mb-4">暂无系列</p>
        <button 
          @click="showCreateSeriesModal = true"
          class="px-6 py-2 bg-primary text-white rounded-lg hover:bg-opacity-90"
        >
          创建第一个系列
        </button>
      </div>

      <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        <div 
          v-for="s in filteredSeries" 
          :key="s.id"
          class="bg-white rounded-xl shadow-sm overflow-hidden"
        >
          <div class="aspect-[2/3] bg-gray-200 relative">
            <img v-if="s.posterPath" :src="s.posterPath" class="w-full h-full object-cover" />
            <div v-else class="w-full h-full flex items-center justify-center text-gray-400">
              <svg class="w-12 h-12" fill="currentColor" viewBox="0 0 24 24">
                <path d="M4 4h16a2 2 0 012 2v12a2 2 0 01-2 2H4a2 2 0 01-2-2V6a2 2 0 012-2z"/>
              </svg>
            </div>
            <div class="absolute top-2 right-2 flex gap-1">
              <button 
                @click="rescrapSeriesConfirm(s)"
                class="p-1 bg-white/80 rounded hover:bg-white text-blue-500"
                title="重新刮削"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
              </button>
              <button 
                @click="editSeries(s)"
                class="p-1 bg-white/80 rounded hover:bg-white text-gray-600"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z"/>
                </svg>
              </button>
              <button 
                @click="deleteSeriesConfirm(s)"
                class="p-1 bg-white/80 rounded hover:bg-white text-red-500"
              >
                <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
                </svg>
              </button>
            </div>
          </div>
          <div class="p-4">
            <h3 class="font-medium text-gray-800 truncate">{{ s.name }}</h3>
            <p class="text-sm text-gray-500 mt-1">{{ getSeriesVideoCount(s.id) }} 视频 | {{ getSeriesSeasonCount(s.id) }} 季</p>
            <button 
              @click="manageSeasons(s)"
              class="mt-3 w-full px-3 py-1.5 text-sm bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200"
            >
              管理季度
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Create/Edit Series Modal -->
    <div v-if="showCreateSeriesModal || editingSeries" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="closeSeriesModal">
      <div class="bg-white rounded-xl p-6 w-full max-w-md">
        <h3 class="text-lg font-semibold text-gray-800 mb-4">
          {{ editingSeries ? '编辑系列' : '创建系列' }}
        </h3>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">系列名称</label>
            <input 
              v-model="seriesForm.name"
              type="text"
              placeholder="输入系列名称"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">TMDB ID</label>
            <input 
              v-model="seriesForm.tmdbId"
              type="number"
              placeholder="可选，填入后自动刮削"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">海报URL</label>
            <input 
              v-model="seriesForm.posterPath"
              type="text"
              placeholder="海报图片URL"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">简介</label>
            <textarea 
              v-model="seriesForm.overview"
              rows="3"
              placeholder="系列简介（可选）"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            ></textarea>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">分类</label>
            <select 
              v-model="seriesForm.categoryId"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            >
              <option :value="null">请选择分类</option>
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">
                {{ cat.name }}
              </option>
            </select>
          </div>
        </div>
        <div class="flex justify-end gap-3 mt-6">
          <button 
            @click="closeSeriesModal"
            class="px-4 py-2 text-gray-600 hover:text-gray-800"
          >
            取消
          </button>
          <button 
            @click="saveSeries"
            class="px-4 py-2 bg-primary text-white rounded-lg hover:bg-opacity-90"
          >
            {{ editingSeries ? '保存' : '创建' }}
          </button>
        </div>
      </div>
    </div>

    <!-- Manage Seasons Modal -->
    <div v-if="showSeasonsModal && managingSeries" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="showSeasonsModal = false">
      <div class="bg-white rounded-xl p-6 w-full max-w-lg">
        <h3 class="text-lg font-semibold text-gray-800 mb-4">
          管理 {{ managingSeries.name }} 的季度
          <button 
            @click="scrapeAllSeasons"
            class="ml-3 text-sm px-3 py-1 bg-green-100 text-green-700 rounded hover:bg-green-200"
          >
            刮削所有季度
          </button>
        </h3>
        <div class="space-y-3 max-h-96 overflow-y-auto">
          <div v-for="season in managingSeriesSeasons" :key="season.id" class="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
            <div>
              <span class="font-medium">{{ season.name }}</span>
              <span class="text-sm text-gray-500 ml-2">{{ getSeasonVideoCount(season.id) }} 集</span>
            </div>
            <div class="flex gap-2">
              <button 
                @click="scrapeSeason(season)"
                class="text-green-600 hover:text-green-800 text-sm"
              >
                刮削此季度
              </button>
              <button 
                @click="editSeason(season)"
                class="text-blue-600 hover:text-blue-800 text-sm"
              >
                编辑
              </button>
              <button 
                @click="deleteSeasonConfirm(season)"
                class="text-red-600 hover:text-red-800 text-sm"
              >
                删除
              </button>
            </div>
          </div>
          <div v-if="managingSeriesSeasons.length === 0" class="text-center text-gray-500 py-4">
            暂无季度
          </div>
        </div>
        <div class="border-t pt-4 mt-4">
          <h4 class="font-medium text-gray-700 mb-2">添加新季度</h4>
          <div class="flex gap-2">
            <input 
              v-model="newSeasonNumber"
              type="number"
              min="1"
              placeholder="季号"
              class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
            <input 
              v-model="newSeasonName"
              type="text"
              placeholder="季名称（可选）"
              class="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
            />
            <button 
              @click="addSeason"
              class="px-4 py-2 bg-primary text-white rounded-lg hover:bg-opacity-90"
            >
              添加
            </button>
          </div>
        </div>
        <div class="flex justify-end mt-4">
          <button 
            @click="showSeasonsModal = false"
            class="px-4 py-2 text-gray-600 hover:text-gray-800"
          >
            关闭
          </button>
        </div>
      </div>
    </div>

    <!-- Create Folder Modal -->
    <div v-if="showCreateFolderModal" class="fixed inset-0 bg-black/50 flex items-center justify-center z-50" @click.self="showCreateFolderModal = false">
      <div class="bg-white rounded-xl p-6 w-full max-w-md">
        <h3 class="text-lg font-semibold text-gray-800 mb-4">创建文件夹</h3>
        <div class="space-y-4">
          <div>
            <label class="block text-sm font-medium text-gray-700 mb-1">文件夹名称</label>
            <input
              v-model="newFolderName"
              type="text"
              placeholder="输入文件夹名称"
              class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary"
              @keyup.enter="createFolder"
            />
          </div>
        </div>
        <div class="flex justify-end gap-3 mt-6">
          <button
            @click="showCreateFolderModal = false"
            class="px-4 py-2 text-gray-600 hover:text-gray-800"
          >
            取消
          </button>
          <button
            @click="createFolder"
            class="px-4 py-2 bg-primary text-white rounded-lg hover:bg-opacity-90"
          >
            创建
          </button>
        </div>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { videoApi, type Video } from '../../api/video'
import { seriesApi, type Series, type Season } from '../../api/series'
import { categoryApi, type Category } from '../../api/category'
import { folderApi, type FolderTreeNode } from '../../api/folder'

interface ImportTask {
  taskId: string
  sourceName: string
  status: string
  progress: number
  message: string
}

const videos = ref<Video[]>([])
const activeTasks = ref<ImportTask[]>([])
const loading = ref(false)
const page = ref(0)
const size = 20
const hasMore = ref(true)
const searchQuery = ref('')
const statusFilter = ref('')
const progressMap = ref<Record<string, number>>({})
const fileInput = ref<HTMLInputElement | null>(null)
const uploading = ref(false)
const isDragging = ref(false)
const selectedFile = ref<File | null>(null)
const videoTitle = ref('')
const uploadProgress = ref(0)
const uploadResult = ref<{ success: boolean; message: string } | null>(null)

// Series management state
const activeTab = ref('videos')
const seriesList = ref<Series[]>([])
const seriesSearchQuery = ref('')
const loadingSeries = ref(false)
const showCreateSeriesModal = ref(false)
const editingSeries = ref<Series | null>(null)
const seriesForm = ref({
  name: '',
  tmdbId: null as number | null,
  posterPath: '',
  overview: '',
  categoryId: null as number | null,
})

const categories = ref<Category[]>([])
const showSeasonsModal = ref(false)
const managingSeries = ref<Series | null>(null)
const managingSeriesSeasons = ref<Season[]>([])
const newSeasonNumber = ref<number | null>(null)
const newSeasonName = ref('')
const seriesVideoCounts = ref<Record<number, number>>({})
const seriesSeasonCounts = ref<Record<number, number>>({})
const seasonVideoCounts = ref<Record<number, number>>({})
const selectedVideoUuids = ref<string[]>([])
const batchSeriesId = ref<number | null>(null)
const batchSeasonId = ref<number | null>(null)
const batchEpisodeStart = ref<number | null>(null)
const batchSeasons = ref<Season[]>([])
const assigningBatch = ref(false)
const showEditDialog = ref(false)
const editVideoForm = ref({
  uuid: '',
  title: '',
  releaseYear: null as number | null
})

const folderTree = ref<FolderTreeNode[]>([])
const selectedFolderId = ref<number | 'ungrouped' | null>(null)
const ungroupedCount = ref(0)
const showCreateFolderModal = ref(false)
const newFolderName = ref('')
const batchMoveFolderId = ref<number | null>(null)
const movingToFolder = ref(false)

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

let taskTimer: number | null = null

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

async function fetchVideos() {
  loading.value = true
  try {
    let res
    if (selectedFolderId.value === 'ungrouped') {
      res = await folderApi.getUngroupedVideos(page.value, size)
    } else if (selectedFolderId.value !== null) {
      res = await folderApi.getVideos(selectedFolderId.value, page.value, size)
    } else {
      res = await videoApi.getList(page.value, size)
    }
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

function editVideo(video: Video) {
  editVideoForm.value = {
    uuid: video.uuid,
    title: video.title || '',
    releaseYear: video.releaseYear || null
  }
  showEditDialog.value = true
}

async function saveVideoEdit() {
  if (!editVideoForm.value.title) {
    alert('标题不能为空')
    return
  }
  try {
    const response = await fetch(`/api/videos/${editVideoForm.value.uuid}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        title: editVideoForm.value.title,
        releaseYear: editVideoForm.value.releaseYear
      })
    })
    if (response.ok) {
      showEditDialog.value = false
      fetchVideos()
    } else {
      alert('保存失败')
    }
  } catch (e) {
    console.error('Failed to update video:', e)
    alert('保存失败')
  }
}

async function fetchFolderTree() {
  try {
    folderTree.value = await folderApi.getTree()
  } catch (e) {
    console.error('Failed to fetch folder tree:', e)
  }
}

async function fetchUngroupedCount() {
  try {
    const res = await folderApi.getUngroupedCount()
    ungroupedCount.value = res.count
  } catch (e) {
    console.error('Failed to fetch ungrouped count:', e)
  }
}

async function selectFolder(folderId: number | 'ungrouped' | null) {
  selectedFolderId.value = folderId
  page.value = 0
  videos.value = []
  await fetchVideos()
}

async function createFolder() {
  if (!newFolderName.value.trim()) {
    alert('文件夹名称不能为空')
    return
  }
  try {
    await folderApi.create(newFolderName.value.trim())
    showCreateFolderModal.value = false
    newFolderName.value = ''
    await fetchFolderTree()
    await fetchUngroupedCount()
  } catch (e) {
    console.error('Failed to create folder:', e)
    alert('创建文件夹失败')
  }
}

async function deleteFolderConfirm(folder: FolderTreeNode) {
  if (!confirm(`确定要删除文件夹 "${folder.name}" 吗？\n注意：文件夹内的视频将移至未分类。`)) return
  try {
    await folderApi.delete(folder.id)
    if (selectedFolderId.value === folder.id) {
      selectedFolderId.value = null
    }
    await fetchFolderTree()
    await fetchUngroupedCount()
    await fetchVideos()
  } catch (e) {
    console.error('Failed to delete folder:', e)
    alert('删除文件夹失败')
  }
}

async function executeMoveToFolder() {
  if (selectedVideoUuids.value.length === 0) {
    alert('请先选择视频')
    return
  }
  if (batchMoveFolderId.value === null) {
    alert('请选择目标文件夹')
    return
  }
  movingToFolder.value = true
  try {
    const res = await folderApi.batchMoveToFolder(selectedVideoUuids.value, batchMoveFolderId.value)
    alert(`已移动 ${res.movedCount} 个视频`)
    clearBatchSelection()
    batchMoveFolderId.value = null
    await fetchFolderTree()
    await fetchUngroupedCount()
    await fetchVideos()
  } catch (e) {
    console.error('Failed to move videos:', e)
    alert('移动视频失败')
  } finally {
    movingToFolder.value = false
  }
}

function triggerUpload() {
  fileInput.value?.click()
}

function handleFileSelect(e: Event) {
  const input = e.target as HTMLInputElement
  if (input.files?.[0]) {
    selectedFile.value = input.files[0]
    uploadResult.value = null
    uploadProgress.value = 0
  }
}

function handleDrop(e: DragEvent) {
  isDragging.value = false
  const file = e.dataTransfer?.files?.[0]
  if (file && file.type.startsWith('video/')) {
    selectedFile.value = file
    uploadResult.value = null
    uploadProgress.value = 0
  }
}

function clearFile() {
  selectedFile.value = null
  if (fileInput.value) fileInput.value.value = ''
  uploadResult.value = null
  uploadProgress.value = 0
}

async function upload() {
  if (!selectedFile.value) return
  
  uploading.value = true
  uploadProgress.value = 0
  uploadResult.value = null
  
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    if (videoTitle.value) formData.append('title', videoTitle.value)
    
    const res = await fetch('/api/videos/upload', {
      method: 'POST',
      body: formData
    })
    
    const data = await res.json()
    if (res.ok && data.uuid) {
      uploadResult.value = { success: true, message: '上传成功，视频正在转码中...' }
      selectedFile.value = null
      videoTitle.value = ''
      if (fileInput.value) fileInput.value.value = ''
      page.value = 0
      fetchVideos()
    } else {
      uploadResult.value = { success: false, message: data.message || '上传失败' }
    }
  } catch (e) {
    console.error('Upload failed:', e)
    uploadResult.value = { success: false, message: '上传失败，请重试' }
  } finally {
    uploading.value = false
  }
}

// Series management functions
async function fetchSeries() {
  loadingSeries.value = true
  try {
    const res = await seriesApi.getAll()
    seriesList.value = res
    for (const s of res) {
      fetchSeriesDetail(s.id)
    }
  } catch (e) {
    console.error('Failed to fetch series:', e)
  } finally {
    loadingSeries.value = false
  }
}

async function fetchCategories() {
  try {
    categories.value = await categoryApi.getAll()
  } catch (e) {
    console.error('Failed to fetch categories:', e)
  }
}

async function fetchSeriesDetail(id: number) {
  try {
    const detail = await seriesApi.getDetail(id)
    seriesVideoCounts.value[id] = detail.videoCount
    seriesSeasonCounts.value[id] = detail.seasonCount
    for (const season of detail.seasons) {
      fetchSeasonDetail(season.id)
    }
  } catch (e) {
    console.error('Failed to fetch series detail:', e)
  }
}

async function fetchSeasonDetail(id: number) {
  try {
    const detail = await seriesApi.getSeasonDetail(id)
    seasonVideoCounts.value[id] = detail.videoCount
  } catch (e) {
    console.error('Failed to fetch season detail:', e)
  }
}

function getSeriesVideoCount(id: number): number {
  return seriesVideoCounts.value[id] || 0
}

function getSeriesSeasonCount(id: number): number {
  return seriesSeasonCounts.value[id] || 0
}

function getSeasonVideoCount(id: number): number {
  return seasonVideoCounts.value[id] || 0
}

const filteredSeries = computed(() => {
  if (!seriesSearchQuery.value) return seriesList.value
  const q = seriesSearchQuery.value.toLowerCase()
  return seriesList.value.filter(s => s.name.toLowerCase().includes(q))
})

const allVisibleSelected = computed(() => {
  if (filteredVideos.value.length === 0) return false
  return filteredVideos.value.every(v => selectedVideoUuids.value.includes(v.uuid))
})

function isVideoSelected(uuid: string): boolean {
  return selectedVideoUuids.value.includes(uuid)
}

function toggleVideoSelection(uuid: string, checked: boolean) {
  if (checked) {
    if (!selectedVideoUuids.value.includes(uuid)) {
      selectedVideoUuids.value.push(uuid)
    }
    return
  }
  selectedVideoUuids.value = selectedVideoUuids.value.filter(id => id !== uuid)
}

function toggleSelectAllVisible(checked: boolean) {
  const visibleUuids = filteredVideos.value.map(v => v.uuid)
  if (checked) {
    const merged = new Set([...selectedVideoUuids.value, ...visibleUuids])
    selectedVideoUuids.value = Array.from(merged)
    return
  }
  selectedVideoUuids.value = selectedVideoUuids.value.filter(id => !visibleUuids.includes(id))
}

function onSelectAllChange(event: Event) {
  const checked = (event.target as HTMLInputElement).checked
  toggleSelectAllVisible(checked)
}

function onVideoSelectChange(event: Event, uuid: string) {
  const checked = (event.target as HTMLInputElement).checked
  toggleVideoSelection(uuid, checked)
}

function clearBatchSelection() {
  selectedVideoUuids.value = []
  batchSeasonId.value = null
  batchEpisodeStart.value = null
}

async function loadBatchSeasons() {
  if (!batchSeriesId.value) {
    batchSeasons.value = []
    batchSeasonId.value = null
    return
  }
  try {
    batchSeasons.value = await seriesApi.getSeasons(batchSeriesId.value)
    if (batchSeasonId.value && !batchSeasons.value.some(s => s.id === batchSeasonId.value)) {
      batchSeasonId.value = null
    }
  } catch (e) {
    console.error('Failed to load batch seasons:', e)
    batchSeasons.value = []
    batchSeasonId.value = null
  }
}

async function batchAssignSelectedVideos() {
  if (selectedVideoUuids.value.length === 0) {
    alert('请先选择视频')
    return
  }
  if (!batchSeriesId.value) {
    alert('请选择系列')
    return
  }
  assigningBatch.value = true
  try {
    const payload: any = {
      videoUuids: selectedVideoUuids.value,
      seriesId: batchSeriesId.value,
    }
    if (batchSeasonId.value) payload.seasonId = batchSeasonId.value
    if (batchEpisodeStart.value && batchEpisodeStart.value > 0) payload.episodeStart = batchEpisodeStart.value
    const res = await seriesApi.batchAssign(payload)
    if (res.success) {
      alert(`批量归档成功，已处理 ${res.assignedCount} 条视频`)
      clearBatchSelection()
      fetchSeries()
      page.value = 0
      await fetchVideos()
    } else {
      alert(res.message || '批量归档失败')
    }
  } catch (e: any) {
    console.error('Failed to batch assign videos:', e)
    const msg = e?.response?.data?.message || e?.message || '批量归档失败'
    alert(msg)
  } finally {
    assigningBatch.value = false
  }
}

function editSeries(s: Series) {
  editingSeries.value = s
  seriesForm.value = {
    name: s.name,
    tmdbId: s.tmdbId,
    posterPath: s.posterPath || '',
    overview: s.overview || '',
    categoryId: (s as any).categoryId || null,
  }
}

function closeSeriesModal() {
  showCreateSeriesModal.value = false
  editingSeries.value = null
  seriesForm.value = {
    name: '',
    tmdbId: null,
    posterPath: '',
    overview: '',
    categoryId: null,
  }
}

async function saveSeries() {
  if (!seriesForm.value.name) {
    alert('请输入系列名称')
    return
  }
  try {
    if (editingSeries.value) {
      await seriesApi.update(editingSeries.value.id, seriesForm.value)
    } else {
      await seriesApi.create(seriesForm.value)
    }
    closeSeriesModal()
    fetchSeries()
  } catch (e: any) {
    console.error('Failed to save series:', e)
    const errorMsg = e.response?.data?.message || e.message || '未知错误'
    alert(`保存失败: ${errorMsg}`)
  }
}

async function deleteSeriesConfirm(s: Series) {
  if (!confirm(`确定要删除系列 "${s.name}" 吗？\n注意：视频不会被删除，只是取消关联。`)) return
  try {
    await seriesApi.delete(s.id)
    fetchSeries()
  } catch (e) {
    console.error('Failed to delete series:', e)
    alert('删除失败')
  }
}

async function rescrapSeriesConfirm(s: Series) {
  if (!confirm(`确定要重新刮削系列 "${s.name}" 吗？这会覆盖现有的海报和简介。`)) return
  try {
    await seriesApi.rescrap(s.id)
    fetchSeries()
    alert('刮削成功')
  } catch (e) {
    console.error('Failed to rescrap series:', e)
    alert('刮削失败')
  }
}

async function manageSeasons(s: Series) {
  managingSeries.value = s
  showSeasonsModal.value = true
  try {
    const detail = await seriesApi.getDetail(s.id)
    managingSeriesSeasons.value = detail.seasons
    for (const season of detail.seasons) {
      fetchSeasonDetail(season.id)
    }
  } catch (e) {
    console.error('Failed to fetch seasons:', e)
  }
}

async function addSeason() {
  if (!managingSeries.value || !newSeasonNumber.value) {
    alert('请输入季号')
    return
  }
  try {
    await seriesApi.createSeason(managingSeries.value.id, {
      seasonNumber: newSeasonNumber.value,
      name: newSeasonName.value || `第 ${newSeasonNumber.value} 季`,
    })
    newSeasonNumber.value = null
    newSeasonName.value = ''
    if (managingSeries.value) {
      await manageSeasons(managingSeries.value)
    }
    fetchSeries()
  } catch (e) {
    console.error('Failed to add season:', e)
    alert('添加失败')
  }
}

async function editSeason(season: Season) {
  const newName = prompt('请输入新的季名称:', season.name)
  if (newName === null) return
  try {
    await seriesApi.updateSeason(season.id, { name: newName })
    if (managingSeries.value) {
      await manageSeasons(managingSeries.value)
    }
    fetchSeries()
  } catch (e) {
    console.error('Failed to update season:', e)
    alert('更新失败')
  }
}

async function deleteSeasonConfirm(season: Season) {
  if (!confirm(`确定要删除季度 "${season.name}" 吗？\n注意：视频不会被删除，只是取消关联。`)) return
  try {
    await seriesApi.deleteSeason(season.id)
    if (managingSeries.value) {
      await manageSeasons(managingSeries.value)
    }
    fetchSeries()
  } catch (e) {
    console.error('Failed to delete season:', e)
    alert('删除失败')
  }
}

async function scrapeSeason(season: Season) {
  if (!managingSeries.value) return
  if (!confirm(`确定要刮削季度 "${season.name}" 吗？`)) return
  try {
    await seriesApi.scrapeSeason(managingSeries.value.id, season.seasonNumber)
    alert('刮削成功')
    if (managingSeries.value) {
      await manageSeasons(managingSeries.value)
    }
  } catch (e) {
    console.error('Failed to scrape season:', e)
    alert('刮削失败')
  }
}

async function scrapeAllSeasons() {
  if (!managingSeries.value) return
  if (!confirm(`确定要刮削所有季度吗？这可能需要一些时间。`)) return
  try {
    await seriesApi.scrapeAllSeasons(managingSeries.value.id)
    alert('刮削成功')
    if (managingSeries.value) {
      await manageSeasons(managingSeries.value)
    }
  } catch (e) {
    console.error('Failed to scrape all seasons:', e)
    alert('刮削失败')
  }
}

onMounted(() => {
  fetchVideos()
  fetchActiveTasks()
  fetchSeries()
  fetchCategories()
  fetchFolderTree()
  fetchUngroupedCount()
  taskTimer = window.setInterval(fetchActiveTasks, 5000)
})

onUnmounted(() => {
  if (taskTimer) {
    clearInterval(taskTimer)
  }
})

watch(batchSeriesId, () => {
  loadBatchSeasons()
})
</script>
