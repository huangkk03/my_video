<template>
  <div class="folder-tree-item">
    <div
      @click="$emit('select', folder.id)"
      :class="[
        'px-3 py-2 rounded-lg cursor-pointer flex items-center justify-between group',
        selectedId === folder.id ? 'bg-primary/10 text-primary' : 'hover:bg-gray-100'
      ]"
      :style="{ paddingLeft: indent * 16 + 12 + 'px' }"
    >
      <span class="flex items-center gap-2 truncate">
        <button
          v-if="folder.children && folder.children.length > 0"
          @click.stop="$emit('toggle', folder.id)"
          class="p-0.5 hover:bg-gray-200 rounded"
        >
          <svg
            class="w-4 h-4 text-gray-400 transition-transform"
            :class="{ 'rotate-90': isExpanded }"
            fill="none" stroke="currentColor" viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
          </svg>
        </button>
        <svg v-else class="w-4 h-4" style="visibility: hidden"></svg>
        <svg class="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"/>
        </svg>
        <span class="truncate">{{ folder.name }}</span>
      </span>
      <div class="flex items-center gap-1">
        <span class="text-xs bg-gray-200 px-2 py-0.5 rounded">{{ folder.videoCount }}</span>
        <button
          @click.stop="$emit('addSubfolder', folder.id)"
          class="p-1 text-gray-400 hover:text-primary rounded opacity-0 group-hover:opacity-100"
          title="添加子文件夹"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
          </svg>
        </button>
        <button
          @click.stop="$emit('delete', folder)"
          class="p-1 text-gray-400 hover:text-red-500 rounded opacity-0 group-hover:opacity-100"
          title="删除文件夹"
        >
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
          </svg>
        </button>
      </div>
    </div>
    <div v-if="isExpanded && folder.children && folder.children.length > 0">
      <FolderTreeItem
        v-for="child in folder.children"
        :key="child.id"
        :folder="child"
        :selectedId="selectedId"
        :indent="indent + 1"
        :expandedIds="expandedIds"
        @select="$emit('select', $event)"
        @toggle="$emit('toggle', $event)"
        @addSubfolder="$emit('addSubfolder', $event)"
        @delete="$emit('delete', $event)"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { FolderTreeNode } from '../../api/folder'

const props = defineProps<{
  folder: FolderTreeNode
  selectedId: number | 'ungrouped' | null
  indent?: number
  expandedIds: Set<number>
}>()

defineEmits<{
  select: [id: number]
  toggle: [id: number]
  addSubfolder: [id: number]
  delete: [folder: FolderTreeNode]
}>()

const isExpanded = computed(() => props.expandedIds.has(props.folder.id))
</script>
