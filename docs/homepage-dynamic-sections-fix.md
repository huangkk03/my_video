# 首页分类区块动态显示修复计划

## 问题描述

当前首页 Tab 下，分类区块的显示逻辑是固定的：
- 电视剧区块始终显示
- 动漫区块只有点击动漫 Tab 才显示
- 即使分类下没有内容，仍会显示"暂无xxx"提示

**期望行为**：
- 分类下**有内容**时 → 显示区块标题 + 海报墙
- 分类下**没有内容**时 → **整个区块都不显示**（包括标题和"暂无"提示）

## 当前代码结构

### 模板区块定义

```html
<!-- 电视剧区块 -->
<section v-if="activeTab === 'home' || activeTab === 'tvshows'">
  <h2>电视剧</h2>
  <MovieGrid :movies="filteredSeries" />
  <div v-else-if="filteredSeries.length === 0">暂无电视剧</div>
</section>

<!-- 动漫区块 -->
<section v-if="activeTab === 'anime'">
  <h2>动漫</h2>
  <MovieGrid :movies="filteredAnime" />
  <div v-else-if="filteredAnime.length === 0">暂无动漫</div>
</section>
```

### 筛选逻辑

```typescript
const filteredSeries = computed(() => {
  return seriesList.value.filter(s => s.categoryId === 2)  // 电视剧
})

const filteredAnime = computed(() => {
  return seriesList.value.filter(s => s.categoryId === 3)  // 动漫
})
```

## 修改方案

### 修改规则

| 区块 | 当前条件 | 修改后条件 |
|------|----------|-----------|
| 电视剧 | `v-if="activeTab === 'home' \|\| activeTab === 'tvshows'"` | `v-if="(activeTab === 'home' \|\| activeTab === 'tvshows') && filteredSeries.length > 0"` |
| 动漫 | `v-if="activeTab === 'home' \|\| activeTab === 'anime'"` | `v-if="(activeTab === 'home' \|\| activeTab === 'anime') && filteredAnime.length > 0"` |

### 需要修改的文件

**文件**: `frontend/src/views/HomeView.vue`

### 区块1: 电视剧海报墙

**当前代码**:
```html
<section v-if="activeTab === 'home' || activeTab === 'tvshows'" class="mb-10">
  <h2 class="text-white text-xl font-semibold mb-4">
    {{ activeTab === 'home' ? '电视剧' : '电视剧' }}
  </h2>
  ...
  <div v-else-if="filteredSeries.length === 0" class="text-gray-400 py-12 text-center">
    暂无电视剧
  </div>
</section>
```

**修改为**:
```html
<section v-if="(activeTab === 'home' || activeTab === 'tvshows') && filteredSeries.length > 0" class="mb-10">
  <h2 class="text-white text-xl font-semibold mb-4">电视剧</h2>
  <MovieGrid :movies="filteredSeries" @movie-click="onSeriesClick" />
</section>
```

### 区块2: 动漫海报墙

**当前代码**:
```html
<section v-if="activeTab === 'home' || activeTab === 'anime'" class="mb-10">
  <h2 class="text-white text-xl font-semibold mb-4">
    {{ activeTab === 'home' ? '动漫' : '动漫' }}
  </h2>
  ...
  <div v-else-if="filteredAnime.length === 0" class="text-gray-400 py-12 text-center">
    暂无动漫
  </div>
</section>
```

**修改为**:
```html
<section v-if="(activeTab === 'home' || activeTab === 'anime') && filteredAnime.length > 0" class="mb-10">
  <h2 class="text-white text-xl font-semibold mb-4">动漫</h2>
  <MovieGrid :movies="filteredAnime" @movie-click="onSeriesClick" />
</section>
```

## 预期效果

| 分类数据情况 | 首页显示 |
|-------------|---------|
| 只有电视剧有内容 | 只显示电视剧区块 |
| 只有动漫有内容 | 只显示动漫区块 |
| 两者都有内容 | 两个区块都显示 |
| 两者都没有内容 | 只显示"最近更新" |

## 实施步骤

1. 修改电视剧区块的条件和结构
2. 修改动漫区块的条件和结构
3. 移除冗余的 `v-else-if` "暂无xxx" 分支
4. 重新构建 Docker 容器
5. 测试验证

## 注意事项

- 最近更新区块保持不变，始终显示
- 分类区块只在有内容时才显示
- 分类区块的标题不再使用三元表达式简化
