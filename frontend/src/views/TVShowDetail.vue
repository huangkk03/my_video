<template>
  <div class="min-h-screen" style="background-color: #101010;">
    <!-- 沉浸式背景 -->
    <div class="fixed inset-0 -z-10">
      <img 
        :src="currentBackdrop || 'https://image.tmdb.org/t/p/original/8TUEj3W2fZf2Z4M9J8E3LQnK2j.jpg'" 
        class="w-full h-full object-cover blur-xl brightness-30 scale-105"
        style="transform: translateZ(-10px);"
      />
      <div class="absolute inset-0" style="background: linear-gradient(to bottom, transparent 0%, rgba(16,16,16,0.6) 50%, #101010 100%);"></div>
    </div>

    <!-- 返回按钮 -->
    <div class="sticky top-0 z-40 px-6 py-4" style="background-color: rgba(16,16,16,0.8); backdrop-filter: blur(10px);">
      <button 
        @click="goBack"
        class="flex items-center gap-2 text-gray-400 hover:text-white transition-colors"
      >
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
        </svg>
        返回
      </button>
    </div>

    <!-- 主内容 -->
    <main class="px-6 pb-12 max-w-screen-2xl mx-auto">
      <!-- 加载状态 -->
      <div v-if="loading" class="py-12 text-center">
        <div class="w-8 h-8 border-2 border-primary border-t-transparent rounded-full animate-spin mx-auto"></div>
      </div>

      <!-- Mock 数据视图 -->
      <template v-else-if="mockSeason && !detail?.series">
        <!-- 季度头部信息 -->
        <div class="flex flex-col md:flex-row gap-8 pt-4 mb-8">
          <!-- 季度海报 -->
          <div class="flex-shrink-0 mx-auto md:mx-0">
            <div class="w-48 md:w-56 aspect-[2/3] rounded-xl overflow-hidden shadow-2xl">
              <img 
                :src="mockSeason.posterPath" 
                :alt="mockSeason.name"
                class="w-full h-full object-cover"
              />
            </div>
          </div>
          
          <!-- 季度信息 -->
          <div class="flex-1 text-white">
            <h1 class="text-2xl md:text-3xl font-bold text-white">{{ mockSeason.name }}</h1>
            <p class="text-gray-400 mt-1">第 {{ mockSeason.seasonNumber }} 季 · {{ mockSeason.episodes.length }} 集</p>
            
            <!-- 操作按钮 -->
            <div class="flex items-center gap-3 my-4">
              <button 
                @click="playFirstSeasonEpisode"
                class="px-6 py-2 rounded-lg bg-white text-black font-medium flex items-center gap-2 hover:bg-gray-200 transition-colors"
              >
                <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M8 5v14l11-7z"/>
                </svg>
                播放
              </button>
              <button 
                @click="toggleFavorite"
                class="w-10 h-10 rounded-full border-2 flex items-center justify-center transition-colors"
                :class="isFavorite ? 'border-red-500 text-red-500' : 'border-gray-400 text-gray-400 hover:border-white'"
              >
                <svg class="w-5 h-5" :fill="isFavorite ? 'currentColor' : 'none'" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
                </svg>
              </button>
            </div>
            
            <!-- 季度简介 -->
            <p v-if="mockSeason.overview" class="text-gray-300 leading-relaxed">{{ mockSeason.overview }}</p>
          </div>
        </div>
        
        <!-- 单集列表 - 横向卡片布局 -->
        <section>
          <h2 class="text-xl font-semibold text-white mb-4">单集</h2>
          <div class="flex gap-4 overflow-x-auto pb-4">
            <EpisodeCard
              v-for="episode in mockSeason.episodes"
              :key="episode.number"
              :episode-number="episode.number"
              :title="episode.title"
              :thumbnail-url="episode.stillPath"
              :duration="episode.duration"
              :current-position="episode.currentPosition"
              :overview="episode.overview"
              @click="playMockEpisode(episode.number)"
            />
          </div>
        </section>
      </template>

      <!-- 季度详情视图 -->
      <template v-else-if="isSeasonView && currentSeason">
        <!-- 季度头部信息 -->
        <div class="flex flex-col md:flex-row gap-8 pt-4 mb-8">
          <!-- 季度海报 -->
          <div class="flex-shrink-0 mx-auto md:mx-0">
            <div class="w-48 md:w-56 aspect-[2/3] rounded-xl overflow-hidden shadow-2xl">
              <img 
                :src="currentSeason.posterPath || 'https://image.tmdb.org/t/p/w500/8TUEj3W2fZf2Z4M9J8E3LQnK2j.jpg'" 
                :alt="currentSeason.name"
                class="w-full h-full object-cover"
              />
            </div>
          </div>
          
          <!-- 季度信息 -->
          <div class="flex-1 text-white">
            <h1 class="text-2xl md:text-3xl font-bold text-white">{{ currentSeason.name }}</h1>
            <p class="text-gray-400 mt-1">第 {{ currentSeason.seasonNumber }} 季 · {{ seasonEpisodes.length }} 集</p>
            
            <!-- 操作按钮 -->
            <div class="flex items-center gap-3 my-4">
              <button 
                @click="playFirstSeasonEpisode"
                class="px-6 py-2 rounded-lg bg-white text-black font-medium flex items-center gap-2 hover:bg-gray-200 transition-colors"
              >
                <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M8 5v14l11-7z"/>
                </svg>
                播放
              </button>
              <button 
                @click="toggleFavorite"
                class="w-10 h-10 rounded-full border-2 flex items-center justify-center transition-colors"
                :class="isFavorite ? 'border-red-500 text-red-500' : 'border-gray-400 text-gray-400 hover:border-white'"
              >
                <svg class="w-5 h-5" :fill="isFavorite ? 'currentColor' : 'none'" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
                </svg>
              </button>
            </div>
            
            <!-- 季度简介 -->
            <p v-if="currentSeason.overview" class="text-gray-300 leading-relaxed">{{ currentSeason.overview }}</p>
          </div>
        </div>
        
        <!-- 单集列表 - 横向卡片布局 -->
        <section>
          <h2 class="text-xl font-semibold text-white mb-4">单集</h2>
          <div class="flex gap-4 overflow-x-auto pb-4">
            <EpisodeCard
              v-for="episode in seasonEpisodes"
              :key="episode.uuid"
              :episode-number="episode.episodeNumber"
              :title="episode.title || `第 ${episode.episodeNumber} 集`"
              :thumbnail-url="episode.thumbnailPath || episode.posterPath || ''"
              :duration="episode.duration"
              :current-position="episode.currentPosition"
              :overview="episode.overview"
              @click="playEpisode(episode.uuid)"
            />
          </div>
        </section>
      </template>

      <!-- 主详情视图 - 全部剧集 -->
      <template v-else-if="detail?.series">
        <!-- 两列布局 -->
        <div class="flex flex-col md:flex-row gap-8 pt-4">
          <!-- 左列：海报 (1/3) -->
          <div class="flex-shrink-0 mx-auto md:mx-0">
            <div class="w-64 md:w-80 aspect-[2/3] rounded-xl overflow-hidden shadow-2xl">
              <img 
                :src="detail.series.posterPath || 'https://image.tmdb.org/t/p/w500/8TUEj3W2fZf2Z4M9J8E3LQnK2j.jpg'" 
                :alt="detail.series.name"
                class="w-full h-full object-cover"
              />
            </div>
          </div>

          <!-- 右列：信息 (2/3) -->
          <div class="flex-1 text-white">
            <!-- 标题 -->
            <h1 class="text-3xl md:text-4xl font-bold text-white">{{ detail.series.name }}</h1>

            <!-- 操作行图标按钮 -->
            <div class="flex items-center gap-3 my-4">
              <button 
                @click="playFirstEpisode"
                class="w-12 h-12 rounded-full border-2 border-white flex items-center justify-center hover:bg-white hover:text-black transition-colors"
              >
                <svg class="w-5 h-5 ml-0.5" fill="currentColor" viewBox="0 0 24 24">
                  <path d="M8 5v14l11-7z"/>
                </svg>
              </button>
              <button 
                @click="toggleWatched"
                class="w-12 h-12 rounded-full border-2 flex items-center justify-center transition-colors"
                :class="isWatched ? 'border-green-500 text-green-500' : 'border-gray-400 text-gray-400 hover:border-white'"
              >
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                </svg>
              </button>
              <button 
                @click="toggleFavorite"
                class="w-12 h-12 rounded-full border-2 flex items-center justify-center transition-colors"
                :class="isFavorite ? 'border-red-500 text-red-500' : 'border-gray-400 text-gray-400 hover:border-white'"
              >
                <svg 
                  class="w-5 h-5" 
                  :fill="isFavorite ? 'currentColor' : 'none'" 
                  stroke="currentColor" 
                  viewBox="0 0 24 24"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
                </svg>
              </button>
            </div>

            <!-- 元数据行 -->
            <div class="flex flex-wrap items-center gap-x-3 text-gray-300 text-sm">
              <span>{{ firstVideo?.releaseYear || '未知' }}</span>
              <span class="text-gray-600">|</span>
              <span>{{ detail.seasonCount }} 季</span>
              <span class="text-gray-600">|</span>
              <span>{{ detail.videoCount }} 集</span>
              <span v-if="firstVideo?.rating" class="text-gray-600">|</span>
              <span v-if="firstVideo?.rating" class="text-yellow-400">⭐ {{ firstVideo.rating.toFixed(1) }}</span>
            </div>

            <!-- 简介 -->
            <p v-if="detail.series.overview" class="text-gray-300 leading-relaxed my-4">{{ detail.series.overview }}</p>

            <!-- 导演信息 -->
            <div v-if="firstVideo?.director" class="text-gray-300 text-sm">
              <span class="text-gray-500">导演：</span>
              <span class="text-white">{{ firstVideo.director }}</span>
            </div>

            <!-- 演职人员区块 -->
            <section v-if="cast.length > 0" class="mt-6">
              <h2 class="text-xl font-semibold text-white mb-4">主要演员</h2>
              <div class="flex gap-4 overflow-x-auto pb-4">
                <div 
                  v-for="member in cast" 
                  :key="member.id"
                  class="flex-shrink-0 text-center group cursor-pointer"
                >
                  <div class="w-16 h-16 rounded-full overflow-hidden border-2 border-transparent group-hover:border-white transition-all duration-300 mx-auto mb-2">
                    <img 
                      v-if="member.profileUrl"
                      :src="member.profileUrl" 
                      :alt="member.name"
                      class="w-full h-full object-cover"
                    />
                    <div v-else class="w-full h-full bg-gray-700 flex items-center justify-center">
                      <svg class="w-6 h-6 text-gray-500" fill="currentColor" viewBox="0 0 24 24">
                        <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
                      </svg>
                    </div>
                  </div>
                  <p class="text-white text-sm font-medium truncate w-20">{{ member.name }}</p>
                </div>
              </div>
            </section>
          </div>
        </div>

        <!-- 单季：直接展开单集列表（横向卡片） -->
        <section v-if="isSingleSeason && detail?.videos?.length > 0" class="mt-10">
          <h2 class="text-xl font-semibold text-white mb-4">单集</h2>
          <div class="flex gap-4 overflow-x-auto pb-4">
            <EpisodeCard
              v-for="video in detail.videos"
              :key="video.uuid"
              :episode-number="video.episodeNumber"
              :title="video.title || `第 ${video.episodeNumber} 集`"
              :thumbnail-url="video.thumbnailPath || video.posterPath || ''"
              :duration="video.duration"
              :current-position="video.currentPosition"
              :overview="video.overview"
              @click="playEpisode(video.uuid)"
            />
          </div>
        </section>

        <!-- 多季：显示季度海报墙 -->
        <section v-if="!isSingleSeason && detail?.seasons?.length > 0" class="mt-10">
          <h2 class="text-xl font-semibold text-white mb-4">季度</h2>
          <div class="flex gap-4 overflow-x-auto pb-4">
            <SeasonCard 
              v-for="season in detail.seasons"
              :key="season.id"
              :id="season.id"
              :name="season.name || `第 ${season.seasonNumber} 季`"
              :poster-url="season.posterPath || ''"
              :episode-count="getEpisodeCount(season.id)"
              @click="onSeasonClick"
            />
          </div>
        </section>

        <!-- 多季：剧集列表（垂直列表） -->
        <section v-if="!isSingleSeason && detail?.videos?.length > 0" class="mt-10">
          <h2 class="text-xl font-semibold text-white mb-4">剧集</h2>
          <div class="space-y-4">
            <div 
              v-for="video in detail.videos" 
              :key="video.uuid"
              class="flex gap-4 p-4 rounded-lg cursor-pointer transition-colors hover:bg-gray-800/60"
              @click="playEpisode(video.uuid)"
            >
              <div class="flex-shrink-0 w-12 h-12 rounded-lg bg-gray-700 flex items-center justify-center">
                <span class="text-white font-semibold">{{ video.episodeNumber }}</span>
              </div>
              
              <div class="flex-shrink-0 w-40 aspect-video rounded-lg overflow-hidden bg-gray-800">
                <img 
                  v-if="video.thumbnailPath || video.posterPath"
                  :src="video.thumbnailPath || video.posterPath" 
                  :alt="video.title"
                  class="w-full h-full object-cover"
                />
                <div v-else class="w-full h-full flex items-center justify-center">
                  <svg class="w-8 h-8 text-gray-600" fill="currentColor" viewBox="0 0 24 24">
                    <path d="M8 5v14l11-7z"/>
                  </svg>
                </div>
              </div>
              
              <div class="flex-1 min-w-0">
                <h3 class="text-white font-medium truncate">{{ video.title || `第 ${video.episodeNumber} 集` }}</h3>
                <p v-if="video.overview" class="text-gray-400 text-sm line-clamp-2 mt-1">{{ video.overview }}</p>
                <div class="flex items-center gap-3 mt-2 text-gray-500 text-xs">
                  <span>{{ formatDuration(video.duration) }}</span>
                  <span v-if="video.currentPosition > 0" class="text-green-500">
                    已观看 {{ formatDuration(video.currentPosition) }}
                  </span>
                </div>
              </div>
            </div>
          </div>
        </section>
      </template>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import SeasonCard from '../components/SeasonCard.vue'
import EpisodeCard from '../components/EpisodeCard.vue'
import { seriesApi, type SeriesDetail, type Season, type Video } from '../api/series'

const router = useRouter()
const route = useRoute()

interface Actor {
  id: number
  name: string
  character: string
  profileUrl: string
}

interface MockEpisode {
  number: number
  title: string
  duration: number
  currentPosition: number
  overview: string
  stillPath: string
}

interface MockSeason {
  id: number
  seriesId: number
  seasonNumber: number
  name: string
  posterPath: string
  overview: string
  tmdbId: number
  episodes: MockEpisode[]
}

const detail = ref<SeriesDetail | null>(null)
const loading = ref(true)
const isFavorite = ref(false)
const isWatched = ref(false)
const cast = ref<Actor[]>([])

// Mock 数据 - 灵能百分百 第一季
const mockSeason: MockSeason = {
  id: 66826,
  seriesId: 1,
  seasonNumber: 1,
  name: "灵能百分百 第一季",
  posterPath: "https://image.tmdb.org/t/p/w500/7LW6ObxN5QWkjJlFjSp56yBahB.jpg",
  overview: '平凡的少年茂夫拥有超强的灵力，但他对这份力量毫无自觉，只想过着平凡的每一天。直到某天，一个名叫"灵幻新隆"的男人出现在他面前——这是一个以收取费用委托灵异事件为生的诈骗犯，却拥有着能引导人们走出困境的真正灵能。在灵幻的劝说下，茂夫开始以"影山律"之名协助灵幻处理各种灵异事件。',
  tmdbId: 66826,
  episodes: [
    { number: 1, title: "灵能力者", duration: 1440000, currentPosition: 0, overview: "超能力少年·茂夫的日常，以及与灵幻新隆的相遇。", stillPath: "https://image.tmdb.org/t/p/w500/pZfP6RzdR1uWpNPMWfCBqS6yP8Q.jpg" },
    { number: 2, title: "幽灵潮", duration: 1440000, currentPosition: 720000, overview: "委托工作的第一天，茂夫面对的是一群恶灵...", stillPath: "https://image.tmdb.org/t/p/w500/z1U1oKNHBytWDYMP2SUtCRd0V59.jpg" },
    { number: 3, title: "密室恐吓", duration: 1440000, currentPosition: 0, overview: "来自“灵魂物品”的委托，茂夫第一次遇到强大的敌人。", stillPath: "https://image.tmdb.org/t/p/w500/nUPd2z4I64k9qH1pF5qKtI5sE2.jpg" },
    { number: 4, title: "集齐的因缘", duration: 1440000, currentPosition: 1080000, overview: "茂夫的中学同学·律司登场，他的目标究竟是什么？", stillPath: "https://image.tmdb.org/t/p/w500/6w1j6Zv6j5K6fL0gRrH9qLzQq4O.jpg" },
    { number: 5, title: "oure～我们～", duration: 1440000, currentPosition: 0, overview: "“超自然现象研究部”的部长·神室登场，茂夫被邀请加入社团。", stillPath: "https://image.tmdb.org/t/p/w500/xvt3VQ7L1lGqwpBkdxlJ1XMYySI.jpg" },
    { number: 6, title: "花与yosaku", duration: 1440000, currentPosition: 0, overview: "灵幻新隆的过去逐渐揭开，他的过去与茂夫有着某种联系...", stillPath: "https://image.tmdb.org/t/p/w500/7LW6ObxN5QWkjJlFjSp56yBahB.jpg" },
    { number: 7, title: "残梦", duration: 1440000, currentPosition: 1440000, overview: "Episode 7 overview...", stillPath: "https://image.tmdb.org/t/p/w500/pZfP6RzdR1uWpNPMWfCBqS6yP8Q.jpg" },
    { number: 8, title: "最强的灵能力者", duration: 1440000, currentPosition: 0, overview: "Episode 8 overview...", stillPath: "https://image.tmdb.org/t/p/w500/z1U1oKNHBytWDYMP2SUtCRd0V59.jpg" },
    { number: 9, title: " 第9集",
      duration: 1440000, currentPosition: 0, overview: "Episode 9 overview...", stillPath: "https://image.tmdb.org/t/p/w500/nUPd2z4I64k9qH1pF5qKtI5sE2.jpg" },
    { number: 10, title: "彭德格拉斯", duration: 1440000, currentPosition: 0, overview: "Episode 10 overview...", stillPath: "https://image.tmdb.org/t/p/w500/6w1j6Zv6j5K6fL0gRrH9qLzQq4O.jpg" },
    { number: 11, title: "各自的道路", duration: 1440000, currentPosition: 0, overview: "Episode 11 overview...", stillPath: "https://image.tmdb.org/t/p/w500/xvt3VQ7L1lGqwpBkdxlJ1XMYySI.jpg" },
    { number: 12, title: "Mob和委托人", duration: 1440000, currentPosition: 0, overview: "Episode 12 overview...", stillPath: "https://image.tmdb.org/t/p/w500/7LW6ObxN5QWkjJlFjSp56yBahB.jpg" },
  ]
}

// 判断是否为季度详情视图
const isSeasonView = computed(() => !!route.params.seasonId)
const currentSeasonId = computed(() => {
  const id = route.params.seasonId
  return id ? Number(id) : null
})

// 当前选中季度
const currentSeason = computed(() => {
  if (!isSeasonView.value || !detail.value) return null
  return detail.value.seasons?.find(s => s.id === currentSeasonId.value) || null
})

// 季度剧集列表
const seasonEpisodes = computed(() => {
  if (!isSeasonView.value || !detail.value) return []
  return detail.value.videos?.filter(v => v.seasonId === currentSeasonId.value) || []
})

// 是否单季（动漫电影版/OVA）
const isSingleSeason = computed(() => {
  if (!detail.value?.seasons) return false
  return detail.value.seasons.length === 1
})

// 当前背景图
const currentBackdrop = computed(() => {
  if (isSeasonView.value && currentSeason.value?.posterPath) {
    return currentSeason.value.posterPath
  }
  return detail.value?.series.backdropPath || ''
})

const firstVideo = computed(() => {
  if (!detail.value?.videos?.length) return null
  return detail.value.videos[0]
})

function getEpisodeCount(seasonId: number): number {
  if (!detail.value?.videos) return 0
  return detail.value.videos.filter(v => v.seasonId === seasonId).length
}

function goBack() {
  router.back()
}

function playFirstEpisode() {
  if (detail.value?.videos?.length) {
    router.push(`/player/${detail.value.videos[0].uuid}`)
  }
}

function playFirstSeasonEpisode() {
  if (isSeasonView.value && seasonEpisodes.value.length > 0) {
    router.push(`/player/${seasonEpisodes.value[0].uuid}`)
  } else if (mockSeason && isSeasonView.value) {
    playMockEpisode(1)
  }
}

function playEpisode(uuid: string) {
  router.push(`/player/${uuid}`)
}

function playMockEpisode(episodeNumber: number) {
  // Mock 模式下弹窗提示
  alert(`播放灵能百分百 第${episodeNumber}集`)
}

function toggleFavorite() {
  isFavorite.value = !isFavorite.value
}

function toggleWatched() {
  isWatched.value = !isWatched.value
}

function onSeasonClick(seasonId: number) {
  router.push(`/tv/${route.params.id}/season/${seasonId}`)
}

function formatDuration(ms: number | undefined): string {
  if (!ms) return '--'
  const totalMinutes = Math.floor(ms / 60000)
  const hours = Math.floor(totalMinutes / 60)
  const minutes = totalMinutes % 60
  return hours > 0 ? `${hours}h ${minutes}m` : `${minutes}m`
}

function parseActors(actorListJson: string | null): Actor[] {
  if (!actorListJson) return []
  try {
    const actors = JSON.parse(actorListJson)
    return actors.slice(0, 10).map((actor: any, index: number) => ({
      id: index,
      name: actor.name || '',
      character: actor.character || '',
      profileUrl: actor.profileUrl || '',
    }))
  } catch {
    return []
  }
}

async function fetchDetail() {
  const id = route.params.id as string
  if (!id) {
    loading.value = false
    return
  }

  try {
    loading.value = true
    const data = await seriesApi.getDetail(Number(id))
    detail.value = data
    
    if (data.videos?.length > 0) {
      const firstVid = data.videos[0] as any
      if (firstVid.actorListJson) {
        cast.value = parseActors(firstVid.actorListJson)
      }
    }
  } catch (e) {
    console.error('Failed to fetch series detail:', e)
  } finally {
    loading.value = false
  }
}

// 监听路由变化，刷新数据
watch(() => route.params.id, () => {
  fetchDetail()
})

onMounted(() => {
  fetchDetail()
})
</script>
