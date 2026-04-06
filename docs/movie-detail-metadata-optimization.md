# 电影详情页右侧信息区优化计划

## 1. 目标

在详情页右侧信息列中充实内容，包含：
- 中英文标题
- 操作行图标按钮（播放、已观看、收藏、更多）
- 元数据行（年份、时长、分级、评分等）
- 技术信息区（视频、音频、字幕）
- Slogan 和简介
- 标签区

---

## 2. Mock Data 扩展

```typescript
interface Movie {
  id: number
  title: string
  titleCn: string
  backdropUrl: string
  posterUrl: string
  rating: number        // TMDB 评分
  year: number
  runtime: number
  genres: string[]
  overview: string
  // 新增字段
  slogan: string        // 一句话 Slogan
  tags: string[]        // 标签数组
  videoInfo: string     // "1080p HEVC"
  audioInfo: string     // "English - Dolby"
  subtitleInfo: string  // "English, Chinese"
  maturityRating: string // "R"
  tomatoRating: number  // 烂番茄评分 28
  endTime: string      // "2h 12m"
  isWatched: boolean   // 是否已观看
  isFavorite: boolean  // 是否收藏
}

const movie: Movie = {
  id: 1,
  title: 'The Gray Man',
  titleCn: '灰影人',
  backdropUrl: 'https://image.tmdb.org/t/p/original/8TUEj3W2fZf2Z4M9J8E3LQnK2j.jpg',
  posterUrl: 'https://image.tmdb.org/t/p/w500/8TUEj3W2fZf2Z4M9J8E3LQnK2j.jpg',
  rating: 6.5,
  year: 2022,
  runtime: 122,
  genres: ['动作', '惊悚', '冒险'],
  overview: '当一个顶级刺客意外成为目标时，他必须在全球追击中生存下来，同时还要揭露那些想要消灭他的秘密组织。',
  slogan: 'The Universe Has a Target',
  tags: ['navy', 'submarine', 'CIA', 'assassin', 'action'],
  videoInfo: '1080p HEVC',
  audioInfo: 'English - Dolby Atmos',
  subtitleInfo: 'English, Chinese, Spanish',
  maturityRating: 'R',
  tomatoRating: 28,
  endTime: '2h 02m',
  isWatched: false,
  isFavorite: false,
}
```

---

## 3. 改动文件

| 文件 | 操作 |
|------|------|
| `src/views/MovieDetail.vue` | 修改 |

---

## 4. 右列信息区代码结构

### 4.1 标题区
```vue
<h1 class="text-4xl font-bold text-white">{{ movie.title }}</h1>
<p class="text-xl text-gray-400">{{ movie.titleCn }}</p>
```

### 4.2 操作行图标按钮
```vue
<div class="flex items-center gap-3 my-4">
  <!-- 播放按钮 -->
  <button class="w-12 h-12 rounded-full border-2 border-white flex items-center justify-center hover:bg-white hover:text-black transition-colors">
    <svg class="w-5 h-5 ml-0.5" fill="currentColor" viewBox="0 0 24 24">
      <path d="M8 5v14l11-7z"/>
    </svg>
  </button>
  <!-- 已观看 -->
  <button class="w-12 h-12 rounded-full border-2 border-gray-400 flex items-center justify-center hover:border-white transition-colors"
    :class="movie.isWatched ? 'border-green-500 text-green-500' : 'text-gray-400'">
    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
    </svg>
  </button>
  <!-- 收藏 -->
  <button class="w-12 h-12 rounded-full border-2 border-gray-400 flex items-center justify-center hover:border-white transition-colors"
    :class="movie.isFavorite ? 'border-red-500 text-red-500' : 'text-gray-400'">
    <svg class="w-5 h-5" :fill="movie.isFavorite ? 'currentColor' : 'none'" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4.318 6.318a4.5 4.5 0 000 6.364L12 20.364l7.682-7.682a4.5 4.5 0 00-6.364-6.364L12 7.636l-1.318-1.318a4.5 4.5 0 00-6.364 0z"/>
    </svg>
  </button>
  <!-- 更多 -->
  <button class="w-12 h-12 rounded-full border-2 border-gray-400 flex items-center justify-center text-gray-400 hover:border-white hover:text-white transition-colors">
    <svg class="w-5 h-5" fill="currentColor" viewBox="0 0 24 24">
      <path d="M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z"/>
    </svg>
  </button>
</div>
```

### 4.3 元数据行
```vue
<div class="flex flex-wrap items-center gap-x-3 text-gray-300 text-sm">
  <span>{{ movie.year }}</span>
  <span class="text-gray-600">|</span>
  <span>{{ movie.runtime }} 分钟</span>
  <span class="text-gray-600">|</span>
  <span class="px-1.5 py-0.5 border border-gray-500 rounded text-xs">{{ movie.maturityRating }}</span>
  <span class="text-gray-600">|</span>
  <span class="text-yellow-400">⭐ {{ movie.rating }}</span>
  <span class="text-gray-600">|</span>
  <span>🍅 {{ movie.tomatoRating }}</span>
  <span class="text-gray-600">|</span>
  <span>{{ movie.endTime }}</span>
</div>
```

### 4.4 技术信息区
```vue
<div class="bg-gray-800/60 rounded-lg p-4 my-4 space-y-2">
  <div class="flex items-center gap-2 text-sm text-gray-300">
    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/>
    </svg>
    {{ movie.videoInfo }}
  </div>
  <div class="flex items-center gap-2 text-sm text-gray-300">
    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15.536 8.464a5 5 0 010 7.072m2.828-9.9a9 9 0 010 12.728M5.586 15.536a5 5 0 001.414 1.414m2.828-9.9a9 9 0 012.728-2.728"/>
    </svg>
    {{ movie.audioInfo }}
  </div>
  <div class="flex items-center gap-2 text-sm text-gray-300">
    <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
      <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 8h10M7 12h4m1 8l-4-4H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-3l-4 4z"/>
    </svg>
    {{ movie.subtitleInfo }}
  </div>
</div>
```

### 4.5 简介区
```vue
<p class="text-lg text-yellow-400 italic my-4">"{{ movie.slogan }}"</p>
<p class="text-gray-300 leading-relaxed">{{ movie.overview }}</p>
```

### 4.6 标签区
```vue
<div class="flex flex-wrap gap-x-2 my-4">
  <span v-for="tag in movie.tags" :key="tag" class="text-sm text-gray-500">#{{ tag }}</span>
</div>
```

---

## 5. 实施步骤

| 步骤 | 操作 | 文件 |
|-----|------|------|
| 1 | 更新 Movie 接口定义 | `MovieDetail.vue` |
| 2 | 更新 Mock Data | `MovieDetail.vue` |
| 3 | 修改右列模板结构 | `MovieDetail.vue` |
| 4 | 测试验证 | - |
