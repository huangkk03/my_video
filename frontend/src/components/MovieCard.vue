<template>
  <div 
    class="group cursor-pointer transition-all duration-300 hover:scale-105 hover:shadow-2xl"
    @click="$emit('click', id)"
  >
    <div class="aspect-[2/3] rounded-xl overflow-hidden">
      <img 
        :src="posterUrl" 
        :alt="title"
        class="w-full h-full object-cover"
      />
    </div>
    <h3 class="text-white text-sm mt-2 truncate">{{ title }}</h3>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  id: string
  title: string
  posterUrl: string
}>()

defineEmits<{
  click: [id: string]
}>()
</script>
