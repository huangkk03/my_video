# 系列季度刮削修复方案

## 问题描述

1. **季度刮削没出来**: TMDB 刮削 API 存在但未连接到前端，无法自动从 TMDB 刮削季度和剧集信息
2. **菜单点不进去**: 需要检查前端"管理季度"功能的交互问题

## 当前状态

### 后端已有
- `ScrapingAggregationService.getTmdbTvSeasonDetails(tvId, seasonNumber)` - 调用 TMDB API 获取季度数据
- `MetadataController.GET /api/metadata/tv/{tmdbId}/season/{seasonNumber}` - 返回 TMDB 数据，但不保存到数据库
- `SeriesService` - 系列管理基本功能

### 后端缺失
- 从 TMDB 数据创建 Season 记录的方法
- 从 TMDB 数据创建 Video 记录的方法
- 将季度刮削连接到前端的 API 端点

### 前端已有
- MediaLibrary.vue 中的"管理季度"模态框
- 手动创建/删除季度的功能

### 前端缺失
- 调用后端刮削的按钮和逻辑

## 修复方案

### Phase 1: 后端 - SeriesService 添加刮削方法

**文件**: `backend/src/main/java/com/video/service/SeriesService.java`

**新增方法 `scrapeSeasonFromTmdb(Long seriesId, Integer seasonNumber)`**:

```java
public Map<String, Object> scrapeSeasonFromTmdb(Long seriesId, Integer seasonNumber) {
    Map<String, Object> result = new HashMap<>();
    
    // 1. 获取 Series 信息
    Series series = seriesRepository.findById(seriesId).orElse(null);
    if (series == null || series.getTmdbId() == null) {
        result.put("success", false);
        result.put("message", "Series not found or no TMDB ID");
        return result;
    }
    
    // 2. 调用 TMDB API 获取季度数据
    TmdbSeasonDetail seasonDetail = scrapingAggregationService.getTmdbTvSeasonDetails(
        series.getTmdbId(), seasonNumber);
    if (seasonDetail == null) {
        result.put("success", false);
        result.put("message", "Failed to fetch season data from TMDB");
        return result;
    }
    
    // 3. 创建或更新 Season 记录
    Season season = createOrUpdateSeason(seriesId, seasonDetail);
    
    // 4. 创建 Episode 视频记录
    List<Video> episodes = createSeasonEpisodes(season, seasonDetail.getEpisodes());
    
    result.put("success", true);
    result.put("season", season);
    result.put("episodes", episodes);
    result.put("episodeCount", episodes.size());
    
    return result;
}
```

**辅助方法**:

```java
private Season createOrUpdateSeason(Long seriesId, TmdbSeasonDetail detail) {
    // 查找是否已存在该季度
    List<Season> existing = seasonRepository.findBySeriesIdOrderBySeasonNumber(seriesId);
    Season season = existing.stream()
        .filter(s -> s.getSeasonNumber().equals(detail.getSeasonNumber()))
        .findFirst()
        .orElse(new Season());
    
    // 更新字段
    season.setSeriesId(seriesId);
    season.setSeasonNumber(detail.getSeasonNumber());
    season.setName(detail.getName());
    season.setOverview(detail.getOverview());
    season.setTmdbId(detail.getId());
    if (detail.getPosterPath() != null) {
        season.setPosterPath("https://image.tmdb.org/t/p/w500" + detail.getPosterPath());
    }
    
    return seasonRepository.save(season);
}

private List<Video> createSeasonEpisodes(Season season, List<TmdbEpisode> tmdbEpisodes) {
    List<Video> videos = new ArrayList<>();
    for (TmdbEpisode ep : tmdbEpisodes) {
        Video video = new Video();
        video.setUuid(UUID.randomUUID().toString());
        video.setTitle(ep.getName());
        video.setSeriesId(season.getSeriesId());
        video.setSeasonId(season.getId());
        video.setEpisodeNumber(ep.getEpisodeNumber());
        video.setOverview(ep.getOverview());
        if (ep.getStillPath() != null) {
            video.setThumbnailPath("https://image.tmdb.org/t/p/w500" + ep.getStillPath());
        }
        video.setStatus("pending");
        video.setCurrentPosition(0L);
        videos.add(videoRepository.save(video));
    }
    return videos;
}
```

### Phase 2: 后端 - SeriesController 添加 API 端点

**文件**: `backend/src/main/java/com/video/controller/SeriesController.java`

**新增端点**:

```java
@PostMapping("/{id}/seasons/{seasonNumber}/scrape")
public ResponseEntity<Map<String, Object>> scrapeSeason(
    @PathVariable Long id,
    @PathVariable Integer seasonNumber
) {
    Map<String, Object> result = seriesService.scrapeSeasonFromTmdb(id, seasonNumber);
    if (Boolean.TRUE.equals(result.get("success"))) {
        return ResponseEntity.ok(result);
    } else {
        return ResponseEntity.badRequest().body(result);
    }
}

@PostMapping("/{id}/seasons/scrape-all")
public ResponseEntity<Map<String, Object>> scrapeAllSeasons(@PathVariable Long id) {
    Series series = seriesService.getSeriesById(id).orElse(null);
    if (series == null || series.getTmdbId() == null) {
        return ResponseEntity.badRequest().body(Map.of(
            "success", false,
            "message", "Series not found or no TMDB ID"
        ));
    }
    
    // 调用 TMDB 获取所有季度信息
    TmdbTvDetail tvDetail = scrapingAggregationService.searchTmdbTvById(series.getTmdbId());
    if (tvDetail == null || tvDetail.getNumberOfSeasons() == null) {
        return ResponseEntity.badRequest().body(Map.of(
            "success", false,
            "message", "Failed to fetch TV details from TMDB"
        ));
    }
    
    List<Map<String, Object>> scrapedSeasons = new ArrayList<>();
    for (int i = 1; i <= tvDetail.getNumberOfSeasons(); i++) {
        Map<String, Object> seasonResult = seriesService.scrapeSeasonFromTmdb(id, i);
        scrapedSeasons.add(seasonResult);
    }
    
    return ResponseEntity.ok(Map.of(
        "success", true,
        "totalSeasons", tvDetail.getNumberOfSeasons(),
        "seasons", scrapedSeasons
    ));
}
```

### Phase 3: 前端 - 添加刮削按钮

**文件**: `frontend/src/views/admin/MediaLibrary.vue`

**修改位置**: Manage Seasons Modal

**添加内容**:

1. 添加"刮削此季度"按钮
2. 添加"刮削所有季度"按钮
3. 调用新的 API 并处理响应

```vue
<!-- 季度列表项 -->
<div v-for="season in managingSeriesSeasons" :key="season.id" class="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
  <div>
    <p class="font-medium">{{ season.name || '第' + season.seasonNumber + '季' }}</p>
    <p class="text-sm text-gray-500">{{ getSeasonVideoCount(season.id) }} 集</p>
  </div>
  <div class="flex gap-2">
    <button @click="scrapeSeason(season.seasonNumber)" class="px-3 py-1 text-sm bg-blue-500 text-white rounded hover:bg-blue-600">
      刮削
    </button>
    <button @click="deleteSeason(season.id)" class="px-3 py-1 text-sm bg-red-500 text-white rounded hover:bg-red-600">
      删除
    </button>
  </div>
</div>

<!-- 刮削所有季度按钮 -->
<button @click="scrapeAllSeasons" class="w-full mt-4 px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600">
  刮削所有季度
</button>
```

**新增方法**:

```typescript
async function scrapeSeason(seasonNumber: number) {
  if (!managingSeries.value) return
  try {
    await seriesApi.scrapeSeason(managingSeries.value.id, seasonNumber)
    alert('刮削成功')
    fetchManagingSeriesSeasons(managingSeries.value.id)
  } catch (e) {
    alert('刮削失败')
  }
}

async function scrapeAllSeasons() {
  if (!managingSeries.value) return
  if (!confirm('确定要刮削所有季度吗？这可能需要几分钟。')) return
  try {
    await seriesApi.scrapeAllSeasons(managingSeries.value.id)
    alert('刮削成功')
    fetchManagingSeriesSeasons(managingSeries.value.id)
  } catch (e) {
    alert('刮削失败')
  }
}
```

### Phase 4: 前端 - API 添加方法

**文件**: `frontend/src/api/series.ts`

**新增方法**:

```typescript
scrapeSeason(seriesId: number, seasonNumber: number): Promise<any> {
  return api.post(`/series/${seriesId}/seasons/${seasonNumber}/scrape`).then(res => res.data)
},

scrapeAllSeasons(seriesId: number): Promise<any> {
  return api.post(`/series/${seriesId}/seasons/scrape-all`).then(res => res.data)
},
```

## 数据结构

### TmdbSeasonDetail (TMDB 返回)
- `id`: Long
- `seasonNumber`: Integer
- `name`: String
- `overview`: String
- `posterPath`: String
- `episodes`: List<TmdbEpisode>

### TmdbEpisode (TMDB 返回)
- `id`: Long
- `episodeNumber`: Integer
- `name`: String
- `overview`: String
- `stillPath`: String

### Season (数据库)
- `id`: Long
- `seriesId`: Long
- `seasonNumber`: Integer
- `name`: String
- `posterPath`: String
- `overview`: String
- `tmdbId`: Long

### Video (数据库)
- `uuid`: String
- `title`: String
- `seriesId`: Long
- `seasonId`: Long
- `episodeNumber`: Integer
- `thumbnailPath`: String
- `overview`: String
- `status`: String
- `currentPosition`: Long

## 预期效果

1. 在"管理季度"模态框中，每个季度旁边显示"刮削"按钮
2. 点击"刮削"按钮后，自动从 TMDB 获取该季度的剧集信息
3. 自动创建 Video 记录，包含集名、简介、缩略图等
4. 支持"刮削所有季度"一键刮削整个系列

## 注意事项

1. **TMDB API 限制**: 注意 API 调用频率限制
2. **已存在处理**: 如果 Season/Video 已存在，选择更新还是跳过
3. **错误处理**: 网络错误、TMDB 无数据等情况需要友好提示
4. **进度反馈**: 刮削多个季度时需要显示进度
