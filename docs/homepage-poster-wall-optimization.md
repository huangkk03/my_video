# 首页海报墙优化计划

## 1. 目标

将首页改造为 Jellyfin 风格的海报墙，核心组件：
- `MovieGrid.vue` - 响应式网格布局
- `MovieCard.vue` - 电影卡片组件

---

## 2. 响应式布局

| 设备 | 列数 | Tailwind class |
|------|------|---------------|
| 手机 | 2 列 | `grid-cols-2` |
| 平板 | 4 列 | `sm:grid-cols-4` |
| PC | 6 列 | `lg:grid-cols-6` |
| 大屏 | 8 列 | `xl:grid-cols-8` |

---

## 3. Mock 数据结构

```typescript
interface MockMovie {
  id: number
  title: string
  posterUrl: string
}
```

---

## 4. 改动文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `src/components/MovieCard.vue` | 新建 | 2:3 比例海报卡片，scale-105 悬停效果 + 阴影加深 |
| `src/components/MovieGrid.vue` | 新建 | 响应式网格布局组件 |
| `src/views/HomeView.vue` | 修改 | 使用 MovieGrid 替代现有 VideoCard 列表 |

---

## 5. MovieCard.vue 组件规格

### 结构
- 2:3 比例海报图片 (`aspect-[2/3]`)
- 海报下方简洁标题
- 悬停：scale-105 + shadow-2xl

### 代码
```vue
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
  id: number
  title: string
  posterUrl: string
}>()

defineEmits<{
  click: [id: number]
}>()
</script>
```

---

## 6. MovieGrid.vue 组件规格

### 结构
- 响应式网格容器
- 接收 movies 数组
- 透传点击事件

### 代码
```vue
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
  id: number
  title: string
  posterUrl: string
}

defineProps<{
  movies: Movie[]
}>()

defineEmits<{
  'movie-click': [id: number]
}>()
</script>
```

---

## 7. HomeView.vue 改动

### 新增 Mock 数据
```typescript
const mockMovies = [
  { id: 1, title: 'Avatar: The Way of Water', posterUrl: '...' },
  // 共 12 部
]
```

### 替换组件
```vue
<MovieGrid :movies="mockMovies" @movie-click="onMovieClick" />
```

### 点击处理
```typescript
function onMovieClick(id: number) {
  console.log('Clicked movie ID:', id)
}
```

---

## 8. 实施步骤

| 步骤 | 操作 | 文件 |
|-----|------|------|
| 1 | 新建 MovieCard.vue | `src/components/MovieCard.vue` |
| 2 | 新建 MovieGrid.vue | `src/components/MovieGrid.vue` |
| 3 | 修改 HomeView.vue | `src/views/HomeView.vue` |
| 4 | 测试验证 | - |

---

## 9. 后续可扩展

1. 从后端 API 获取真实数据替代 Mock
2. 实现搜索过滤功能
3. 分类标签切换（电影/电视剧/动漫）
4. 无限滚动加载
