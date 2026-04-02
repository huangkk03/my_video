# 刮削问题修复记录

## 背景

问题表现为：前端创建系列时提示 `timeout of 60000ms exceeded`，但后端日志显示已经成功请求到 TMDB 数据。

## 根因分析

本次问题不是 TMDB 连通性问题，核心原因是后端在 TMDB 响应解析阶段出现卡死，导致请求线程无法及时返回。

具体触发点：

- `ScrapingAggregationService` 使用了自定义 JSON 解析器。
- 解析 `genre_ids: [18]` 这类数字数组时，游标推进逻辑存在缺陷，可能进入死循环。
- 结果是后端已拿到 TMDB 响应，但停在解析阶段，前端等待 60 秒后超时。

## 修复计划与已完成项

### 1) 替换 TMDB 解析实现（已完成）

文件：`backend/src/main/java/com/video/service/ScrapingAggregationService.java`

- 引入 Jackson：`ObjectMapper` + `JsonNode`
- 将 `searchTmdb()` 和 `searchTmdbTv()` 的解析从自定义 `parseJsonSimple` 改为 Jackson 树模型读取。
- 保持原字段映射不变：
  - `id`
  - `title` / `name`
  - `overview`
  - `poster_path`
  - `release_date` / `first_air_date`
  - `vote_average`

### 2) 保留现有日志语义（已完成）

- 请求 URL 继续进行 API Key 脱敏输出。
- 保留空响应、无结果、异常堆栈等关键日志。
- 增加了 TMDB Movie 解析成功日志，便于确认回退链路。

### 3) 补充落库关键日志（已完成）

文件：`backend/src/main/java/com/video/service/SeriesService.java`

- 在 `seriesRepository.save(series)` 前后增加日志：
  - 开始落库（包含 name/tmdbId）
  - 落库成功（包含 id/name）
- 用于区分“卡在解析”还是“卡在数据库保存”。

### 4) 事务与连接池优化（已完成）

文件：

- `backend/src/main/java/com/video/service/SeriesService.java`
- `backend/src/main/resources/application.yml`

改动：

- 移除 `createSeries` / `rescrapSeries` 方法上的 `@Transactional`，避免在耗时网络请求期间长期持有数据库连接。
- 增加 Hikari 连接池配置：
  - `maximum-pool-size: 20`
  - `connection-timeout: 30000`

### 5) 字段与唯一键风险兜底（已完成）

文件：`backend/src/main/java/com/video/entity/Series.java`

- `overview` 列定义调整为 `LONGTEXT`，避免长简介落库风险。
- `slug` 生成逻辑增加长度限制与时间戳后缀，降低唯一键冲突概率。

## TMDB 数据最终存储位置

创建系列时，TMDB 返回数据会映射并写入 MySQL 的 `series` 表：

- `series.tmdb_id` <- TMDB `id`
- `series.overview` <- TMDB `overview`
- `series.poster_path` <- `https://image.tmdb.org/t/p/w500` + `poster_path`

对应代码位置：

- 映射逻辑：`backend/src/main/java/com/video/service/SeriesService.java`
- 实体字段：`backend/src/main/java/com/video/entity/Series.java`
- 落库调用：`seriesRepository.save(series)`

## 验证结果

已验证可正常刮削，问题复现路径恢复正常：

- 创建系列可返回成功，不再出现前端 60s 超时。
- TMDB 结果可正常映射并写入数据库。
- 海报与简介可落库并在前端展示。

