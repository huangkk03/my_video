# 电影详情页演职人员 (Cast) 区块开发计划

## 1. 目标

在详情页剧情简介下方添加"演职人员"(Cast) 区块，包含：
- 横向滚动列表
- 圆形演员头像
- 演员名和角色名
- 悬停放大/描边效果

---

## 2. Mock Data 结构

```typescript
interface Cast {
  id: number
  name: string
  character: string
  profileUrl: string
}

const cast: Cast[] = [
  { id: 1, name: 'Ryan Gosling', character: 'Lloyd Hansen', profileUrl: 'https://image.tmdb.org/t/p/w200/pPD2cP7B8xM0SqpGngUz0M3GUD4.jpg' },
  { id: 2, name: 'Chris Evans', character: 'Danny Hansen', profileUrl: 'https://image.tmdb.org/t/p/w200/pT97mVTBOV8S3P2rnAm8ldD5W3d.jpg' },
  { id: 3, name: 'Ana de Armas', character: 'Dizzy', profileUrl: 'https://image.tmdb.org/t/p/w200/ABJ1uw1EGOT6G16V9GFvxRtnC.jpg' },
  { id: 4, name: 'Billy Bob Thornton', character: 'Victor', profileUrl: 'https://image.tmdb.org/t/p/w200/lLY3t8M7VJfGZ4PxGz5NBfqxYf.jpg' },
  { id: 5, name: 'Dafne Keen', character: 'Claire Russell', profileUrl: 'https://image.tmdb.org/t/p/w200/2wsJBj4J4P0TxV4VrQSX7lXdn4Q.jpg' },
]
```

---

## 3. 改动文件

| 文件 | 操作 |
|------|------|
| `src/views/MovieDetail.vue` | 修改 |

---

## 4. Cast 区块模板代码

```vue
<!-- 演职人员区块 -->
<section class="mt-8">
  <h2 class="text-xl font-semibold text-white mb-4">演职人员</h2>
  <div class="flex gap-4 overflow-x-auto pb-4">
    <div 
      v-for="member in cast" 
      :key="member.id"
      class="flex-shrink-0 text-center group cursor-pointer"
    >
      <!-- 圆形头像 -->
      <div class="w-20 h-20 rounded-full overflow-hidden border-2 border-transparent group-hover:border-white transition-all duration-300 mx-auto mb-2">
        <img 
          v-if="member.profileUrl"
          :src="member.profileUrl" 
          :alt="member.name"
          class="w-full h-full object-cover"
        />
        <div v-else class="w-full h-full bg-gray-700 flex items-center justify-center">
          <svg class="w-8 h-8 text-gray-500" fill="currentColor" viewBox="0 0 24 24">
            <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z"/>
          </svg>
        </div>
      </div>
      <!-- 演员名 -->
      <p class="text-white text-sm font-medium truncate w-24">{{ member.name }}</p>
      <!-- 角色名 -->
      <p class="text-gray-400 text-xs truncate w-24">{{ member.character }}</p>
    </div>
  </div>
</section>
```

---

## 5. 实施步骤

| 步骤 | 操作 | 文件 |
|-----|------|------|
| 1 | 在 MovieDetail.vue 中添加 Cast 接口 | `MovieDetail.vue` |
| 2 | 添加 cast Mock 数据 | `MovieDetail.vue` |
| 3 | 在简介区后添加 Cast 区块模板 | `MovieDetail.vue` |
| 4 | 测试验证 | - |

---

## 6. 交互效果

| 效果 | 实现 |
|------|------|
| 横向滚动 | `overflow-x-auto` |
| 圆形头像 | `w-20 h-20 rounded-full` |
| 悬停放大/描边 | `group-hover:border-white` + `transition-all` |
| 无图占位符 | 灰色背景 + 用户图标 |
