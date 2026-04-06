# TMDB 演员信息刮削修复计划

## 问题描述

当前 TMDB 刮削只获取了电影基本信息（标题、概述、海报、评分、上映日期），**没有获取演员 (cast) 和导演 (director) 信息**，导致详情页演员列表为空。

## 问题根因

1. `ScrapingAggregationService.searchTmdb()` 只调用了 `/search/movie` 接口
2. 获取演员需要额外调用 `/movie/{id}/credits` 接口
3. `MetadataResult` 类缺少 `actors` 和 `director` 字段
4. `CloudMediaService.applyScrapedMetadataToVideo()` 没有保存演员数据

## 修复方案

### 1. 修改 `MetadataResult` 类 (ScrapingAggregationService.java)

新增字段：
```java
private String actors;      // 演员列表，逗号分隔
private String director;     // 导演姓名
```

### 2. 新增 `getTmdbMovieCredits()` 方法 (ScrapingAggregationService.java)

调用 TMDB `/movie/{id}/credits` 接口，获取：
- `cast` - 前 10 位演员，提取 name 字段，逗号分隔
- `crew` - 筛选 job 为 "Director" 的成员，获取 name

API URL:
```
GET /movie/{movie_id}/credits?api_key={key}&language={lang}
```

### 3. 修改 `searchTmdb()` 方法 (ScrapingAggregationService.java)

在获取电影基本信息后，调用 `getTmdbMovieCredits()` 获取演职人员数据，并填充到 `MetadataResult`。

### 4. 修改 `applyScrapedMetadataToVideo()` 方法 (CloudMediaService.java)

新增保存逻辑：
```java
if (metadataResult.getActors() != null) {
    video.setActors(metadataResult.getActors());
}
if (metadataResult.getDirector() != null) {
    video.setDirector(metadataResult.getDirector());
}
```

## 预期结果

- 重新刮削视频后，`actors` 和 `director` 字段被正确保存到数据库
- 前端详情页 `MovieDetail.vue` 能正确显示演员列表
