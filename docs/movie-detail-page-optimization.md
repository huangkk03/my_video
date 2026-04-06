# 电影详情页 (MovieDetail.vue) 开发计划

## 1. 目标

开发电影详情页 (MovieDetail.vue)，包含：
- 沉浸式背景（Backdrop）
- 两列布局（左海报，右信息）

---

## 2. 页面布局

```
┌──────────────────────────────────────────────────────┐
│  [返回按钮]                                         │
├──────────────────────────────────────────────────────┤
│  ┌─────────┐  标题                                  │
│  │         │  ★ 8.5  2024  120 分钟               │
│  │  海报   │  [类型标签] [类型标签]                 │
│  │         │                                        │
│  │         │  简介文字...                           │
│  └─────────┘  [播放] [收藏]                         │
└──────────────────────────────────────────────────────┘
```

---

## 3. Mock 数据结构

```typescript
interface Movie {
  id: number
  title: string
  backdropUrl: string
  posterUrl: string
  rating: number
  year: number
  runtime: number
  genres: string[]
  overview: string
}

const mockMovie: Movie = {
  id: 1,
  title: 'Avatar: The Way of Water',
  backdropUrl: 'https://image.tmdb.org/t/p/original/sTJTcVzOPvVZzJ4b0G8zL1X6NLG.jpg',
  posterUrl: 'https://image.tmdb.org/t/p/w500/t6HIqrRAclMCA60NsSmeqe9RmNV.jpg',
  rating: 8.5,
  year: 2022,
  runtime: 192,
  genres: ['科幻', '动作', '冒险'],
  overview: '《阿凡达：水之道》是一部2022年上映的美国科幻电影，由詹姆斯·卡梅隆执导。故事发生在潘多拉星球，讲述杰克·萨利一家如何在神秘的水世界Navitr中发现新的危险和挑战。'
}
```

---

## 4. 改动文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `src/views/MovieDetail.vue` | 新建 | 电影详情页组件 |
| `src/router.ts` | 修改 | 添加 `/movie/:id` 路由 |
| `src/views/HomeView.vue` | 修改 | 点击海报跳转详情页 |

---

## 5. MovieDetail.vue 组件规格

### 沉浸式背景
- 全屏背景图
- 高斯模糊 (blur-xl) + 亮度降低 (brightness-30)
- 从上到下渐变遮罩 (bg-gradient-to-b from-transparent via-transparent to-[#101010])

### 两列布局
- 左列：海报 (约 1/3 宽度)，圆角 + 阴影
- 右列：所有文字信息 (约 2/3 宽度)

### 右列信息
- 标题 (text-4xl font-bold text-white)
- 评分、年份、时长
- 类型标签
- 简介
- 操作按钮（播放、收藏）

---

## 6. 实施步骤

| 步骤 | 操作 | 文件 |
|-----|------|------|
| 1 | 新建 MovieDetail.vue | `src/views/MovieDetail.vue` |
| 2 | 添加路由 `/movie/:id` | `src/router.ts` |
| 3 | HomeView 点击跳转到 MovieDetail | `src/views/HomeView.vue` |
| 4 | 测试验证 | - |

---

## 7. 后续可扩展

1. 演员列表
2. 相关推荐
3. 预告片播放
4. 评论功能
