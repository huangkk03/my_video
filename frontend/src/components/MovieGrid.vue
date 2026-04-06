<template>
  <div class="grid grid-cols-2 sm:grid-cols-4 lg:grid-cols-6 xl:grid-cols-8 gap-4">
    <MovieCard 
      v-for="movie in movies" 
      :key="movie.id"
      :id="movie.id"
      :title="movie.title"
      :poster-url="movie.posterUrl"
      @click="(id) => $emit('movie-click', id)"
    />
  </div>
</template>

<script setup lang="ts">
import MovieCard from './MovieCard.vue'

interface Movie {
  id: string
  title: string
  posterUrl: string
}

defineProps<{
  movies: Movie[]
}>()

defineEmits<{
  'movie-click': [id: string]
}>()
</script>
