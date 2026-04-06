# 详情页与后端数据关联计划

## 1. 目标

将 MovieDetail.vue 和 HomeView.vue 与后端真实 API 关联。

---

## 2. 后端 Video 实体字段

| 字段 | 类型 | 说明 |
|------|------|------|
| uuid | String | 唯一标识 |
| title | String | 标题 |
| posterPath | String | 海报路径 |
| backdropPath | String | 背景图路径 |
| overview | String | 简介 |
| rating | Double | 评分 |
| releaseYear | Integer | 发布年份 |
| duration | Long | 时长(毫秒) |
| genres | String | 类型(逗号分隔) |
| actors | String | 演员(逗号分隔) |
| currentPosition | Long | 播放位置 |

---

## 3. API 接口

- `GET /api/videos/{uuid}` - 获取视频详情
- `GET /api/videos` - 获取视频列表

---

## 4. 改动文件

| 文件 | 改动 |
|------|------|
| `src/api/video.ts` | 添加 `getByUuid` 方法 |
| `src/views/MovieDetail.vue` | 接收 uuid 参数，调用 API 获取真实数据 |
| `src/views/HomeView.vue` | 修改 mock 数据，传入真实 uuid |
| `src/router.ts` | 路由参数改为 `:uuid` |

---

## 5. API 接口定义 (src/api/video.ts)

```typescript
async getByUuid(uuid: string): Promise<Video> {
  const res = await fetch(`/api/videos/${uuid}`)
  if (!res.ok) throw new Error('Failed to fetch video')
  return res.json()
}
```

---

## 6. MovieDetail.vue 改动

### 接口定义
```typescript
interface Movie {
  uuid: string
  title: string
  titleCn: string
  backdropUrl: string
  posterUrl: string
  rating: number
  year: number
  runtime: number
  genres: string[]
  overview: string
  slogan: string
  tags: string[]
  videoInfo: string
  audioInfo: string
  subtitleInfo: string
  maturityRating: string
  tomatoRating: number
  endTime: string
  isWatched: boolean
  isFavorite: boolean
  cast: Cast[]
}

interface Cast {
  id: number
  name: string
  character: string
  profileUrl: string
}
```

### API 调用逻辑
```typescript
const route = useRoute()
const videoUuid = route.params.uuid as string

const movie = ref<Movie | null>(null)

async function fetchVideo() {
  try {
    const video = await videoApi.getByUuid(videoUuid)
    movie.value = {
      uuid: video.uuid,
      title: video.title,
      posterUrl: video.posterPath || '',
      backdropUrl: video.backdropPath || '',
      // ... 映射其他字段
    }
  } catch (e) {
    console.error('Failed to fetch video:', e)
  }
}
```

---

## 7. HomeView.vue 改动

### Mock 数据改为真实 UUID
```typescript
const mockMovies = [
  { id: '实际-video-uuid-1', title: 'Avatar', posterUrl: '...' },
  // ...
]
```

---

## 8. 路由改动

```typescript
// router.ts
{ path: '/movie/:uuid', name: 'movie-detail', component: () => import('./views/MovieDetail.vue') }
```

---

## 9. 实施步骤

| 步骤 | 操作 | 文件 |
|-----|------|------|
| 1 | 在 video.ts 添加 getByUuid 方法 | `src/api/video.ts` |
| 2 | 修改 MovieDetail.vue 接收 uuid 并调用 API | `src/views/MovieDetail.vue` |
| 3 | 修改 HomeView.vue 使用真实 UUID | `src/views/HomeView.vue` |
| 4 | 测试验证 | - |

---

## 10. 暂无字段处理

以下字段后端无数据，暂时使用占位值：

| 字段 | 占位值 |
|------|--------|
| titleCn | 使用 title |
| slogan | 使用 overview 首句 |
| tags | 空数组 |
| videoInfo | "1080p" |
| audioInfo | "" |
| subtitleInfo | "" |
| maturityRating | "" |
| tomatoRating | 0 |
| cast | 从 actors 字符串解析（逗号分隔） |
