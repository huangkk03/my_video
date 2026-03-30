---
name: mkv-video-expert
description: 专门用于构建基于 Vue 3 + Spring Boot 的在线视频平台，具备 MKV 自动转码 HLS 及全链路开发能力。
version: 2.0.0
author: Gemini-Collaborator
tags: [video-streaming, ffmpeg, spring-boot, vue3, hls]
---

# MKV 视频平台全链路开发技能

当你识别到用户想要构建“视频网站”、“播放 MKV”或“流媒体”任务时，请自动激活此技能，并严格遵循以下四个阶段的执行逻辑。

## 阶段 1：产品与需求定义 (Product Discovery)
**目标**：将模糊需求转化为可执行的技术规格。
- **输出要求**：
  - 生成 `REQUIREMENTS.md`：包含 MVP 功能（上传、转码、HLS 播放、进度记忆）。
  - 生成 `DB_SCHEMA.sql`：包含 `video_metadata`（存储路径、状态）、`transcode_tasks`（进度、错误日志）。
  - **触发条件**：用户提到“我要做一个视频网站”时首先执行。

## 阶段 2：架构设计规范 (Architecture)
**目标**：确立音视频处理的最佳实践。
- **核心约束**：
  - **禁止**：直接在前端播放 MKV（浏览器不支持）。
  - **必须**：后端采用 FFmpeg 进行 `MKV -> HLS (.m3u8 + .ts)` 的异步转码。
  - **存储**：切片必须存储在结构化目录 `/data/videos/{uuid}/index.m3u8`。
  - **协议**：后端控制器必须支持 `HTTP 206 Partial Content`。

## 阶段 3：代码实施标准 (Implementation)
### 后端 (Spring Boot 3 + Java 25)
- **FFmpeg 模板**：调用系统 FFmpeg 使用以下命令：
  `ffmpeg -i {input} -codec:v libx264 -codec:a aac -f hls -hls_time 10 -hls_list_size 0 {output}/index.m3u8`
- **异步控制**：必须使用 `@Async` 并配合 `CompletableFuture` 处理转码回调。
- **硬件加速**：若检测到环境为 ARM/RK3588，优先建议使用 `h264_v4l2m2m` 编码器。

### 前端 (Vue 3.5+ & Vite)
- **播放器集成**：首选 `ArtPlayer` + `Hls.js`。
- **生命周期**：必须在 `onUnmounted` 中调用 `art.destroy()` 和 `hls.destroy()` 防止内存泄漏。
- **状态同步**：实现 `timeupdate` 监听，每 5 秒同步一次进度到后端。

## 阶段 4：质量保证与部署 (QA & DevOps)
- **自动化测试**：生成 `VideoTranscodeTest.java`，测试 0 字节文件和非视频文件的异常捕获。
- **部署脚本**：生成包含 FFmpeg 运行库的 `Dockerfile`（推荐多阶段构建）。
- **Nginx 配置**：提供支持 `cross-origin` 的 HLS 静态分发配置。

## 示例触发指令 (Usage)
- "根据 mkv-video-expert 技能，开始阶段 1 的需求梳理。"
- "现在执行阶段 3，生成后端的转码 Service 类。"

## 阶段 5：视觉与交互增强 (UI/UX Excellence)
**目标**：提升页面质感，实现主流流媒体平台（Bilibili/Netflix）的视觉标准。


### 1. 核心视觉风格 (Visual Identity)
根据用户偏好选择以下一种风格执行：

- **风格 A：Bilibili 灵动感 (Community Style)**
  - **色彩**：主色 `#00AEEC` (少女粉/天霁蓝)，背景使用纯白或极浅灰 (#F1F2F3)。
  - **圆角**：统一使用 `rounded-lg (8px)` 或 `rounded-xl`，营造亲和力。
  - **字体**：优先使用 `system-ui, -apple-system, sans-serif`，字重设为 500 以增强可读性。
  - **装饰**：使用细微的 `shadow-sm` 和 `border-gray-100`。

- **风格 B：Netflix 沉浸感 (Cinematic Style)**
  - **色彩**：背景深黑 (#0F172A)，主色红色 (#E50914)，文字纯白。
  - **滤镜**：图片和视频卡片在非 Hover 状态下可带微弱的 `brightness-90`。
  - **排版**：无边框设计，通过间距和深浅阴影 (shadow-2xl) 区分层级。

### 2. 高级交互规范 (Micro-Interactions)
- **卡片悬停 (Card Hover)**：
  - 必须实现：`hover:scale-105` + `transition-all duration-300`。
  - 进阶：悬停时显示视频预览（自动播放无声片段）和“稍后再看”按钮。
- **加载状态 (Loading)**：
  - 严禁使用 Loading 文字，必须生成符合卡片比例的 **骨架屏 (Skeleton)**。
  - 使用 Tailwind 的 `animate-pulse` 类。
- **播放器控制条 (Player UI)**：
  - 采用 **毛玻璃效果 (Glassmorphism)**：`bg-white/10 backdrop-blur-md`。
  - 控制条需在鼠标静止 3 秒后通过 `opacity-0` 平滑隐藏。

### 3. 响应式网格 (Grid Layout)
- 视频列表必须严格遵循以下断点：
  - Mobile (sm): 1 col (gap-4)
  - Tablet (md): 2-3 cols
  - Desktop (lg): 4 cols
  - UltraWide (xl): 5-6 cols
  
## 阶段 7：容器化与自动化编排 (Docker & DevOps Skill)
**目标**：实现一键构建镜像并在本地环境（支持 X86/ARM）快速部署测试。

### 1. 多阶段构建 Dockerfile 规范
- **后端 (Spring Boot)**：
  - 基础镜像必须包含 `ffmpeg`。
  - 使用 `eclipse-temurin:21-jre` 或更高版本。
  - 分阶段构建：`maven:3.9-eclipse-temurin` 构建 Jar -> 运行镜像。
- **前端 (Vue 3)**：
  - 使用 `node:20` 构建静态文件。
  - 使用 `nginx:stable-alpine` 进行分发，配置 `try_files $uri $uri/ /index.html`。

### 2. Docker Compose 编排规范
- **服务定义**：
  - `video-api`: Spring Boot 后端，挂载 `/data/videos` 卷用于持久化转码文件。
  - `video-ui`: Nginx 前端，监听 80 端口，配置反向代理 `/api` 到后端。
  - `video-db`: MySQL 8.0，初始化脚本自动建表。
  - `video-redis`: 用于缓存转码进度。
- **环境变量**：所有敏感信息（DB 密码、上传路径）必须通过 `.env` 文件注入。

### 3. 本地部署测试指令 (Deployment Automation)
- 提供 `deploy.sh` 脚本模板：
  - `docker-compose down -v` (清理)
  - `docker-compose build --no-cache` (强力构建)
  - `docker-compose up -d` (后台启动)

## 阶段 8：质量检测 (Testing)
- 自动化生成针对 Docker 环境的健康检查 (Healthcheck)。
- 验证 HLS 切片在容器内部挂载目录的写入权限。  

## 阶段 10：前端容器化生产规范 (Frontend Docker Production)
**目标**：将 Vue 3 静态资源封装为高性能 Nginx 镜像，解决跨域与路由回退问题。

### 1. 前端多阶段构建 (Multi-stage Build)
- **构建阶段 (Build)**：使用 `node:20-alpine`，运行 `npm install` 和 `npm run build`。
- **运行阶段 (Runtime)**：使用 `nginx:stable-alpine`，仅保留编译后的 `dist` 目录。
- **体积优化**：清理 `node_modules`，镜像大小需控制在 50MB 以内。

### 2. Nginx 配置模板 (nginx.conf)
- **路由兼容**：必须配置 `try_files $uri $uri/ /index.html` 以支持 Vue Router 的 History 模式。
- **API 反向代理**：配置 `location /api/` 转发到 `video-api:8080`，避免前端直接暴露后端端口。
- **HLS 优化**：为 `.m3u8` 和 `.ts` 文件开启 `add_header Cache-Control "no-cache"` (防止索引文件被强制缓存)。

### 3. Docker Compose 集成
- **统一网桥**：前端容器与后端、数据库必须位于同一个 `docker-network`。
- **端口映射**：前端对外暴露 `80` 或 `8080`，后端端口不再对外暴露，仅供前端容器内部访问。


## 阶段 11：音视频处理性能优化 (Transcoding Optimization)
**目标**：解决转码慢、体积膨胀（如 330MB 变 692MB）的痛点。

### 1. 体积优化策略 (Storage Efficiency)
- **避免无效重编码**：若源文件编码为 H.264，禁止使用 `-vcodec libx264`，必须使用 `-vcodec copy`。
  - *原理*：只改变封装格式 (Container)，不重新计算像素，体积几乎不变，速度提升 100 倍。
- **压制比特率 (Bitrate Control)**：若必须重编码（如 MKV 转 H.264），必须限制 `-crf 23` (均衡) 或固定码率 `-b:v 2M`，防止 FFmpeg 盲目分配高码率。
- **TS 切片冗余控制**：在 HLS 配置中加入 `-hls_flags delete_segments`，及时清理临时文件。

### 2. 转码速度优化 (Speed Optimization)
- **极速预设**：重编码时强制使用 `-preset superfast` 或 `ultrafast`。
- **硬件加速方案 (Hardware Acceleration)**：
  - **通用 (Intel/AMD)**：使用 `h264_vaapi` 或 `h264_nvenc` (NVIDIA)。
  - **ARM/RK3588 (针对用户环境)**：必须配置 `h264_rkmpp` 或 `h264_v4l2m2m` 硬件编码器，跳过 CPU 计算。
- **多线程处理**：配置 `-threads 0` 自动利用所有 CPU 核心。

### 3. HLS 切片策略
- **切片时长**：设置 `-hls_time 6`（6秒一个切片），兼顾起播速度与索引文件大小。
- **关键帧对齐**：使用 `-g 60` (假设 30fps) 强制关键帧间距，确保切片在 I 帧处切割，避免播放卡顿。

## 阶段 12：Jellyfin 沉浸式 UI/UX 规范 (Cinematic Experience)
**目标**：构建像 Jellyfin/Plex 那样的海报墙布局和沉浸式详情页。

### 1. 海报墙网格 (The Poster Wall)
- **卡片排版**：强制使用 **2:3 垂直比例 (Vertical Aspect Ratio)** 的海报卡片。
- **视觉层级**：卡片上只显示海报图，悬停 (Hover) 时通过 `backdrop-blur-sm` 浮窗显示标题和年份。禁止显示播放按钮。
- **布局间距**：使用 `gap-8` 保持宽阔留白，整体背景设为深色 (#0A0F1E)。

### 2. 沉浸式详情页 (The Watch Page)
- **背景毛玻璃 (Backdrop Blur)**：必须将该视频的横向海报 (Backdrop) 作为全屏背景，并应用 `blur-2xl brightness-50` 滤镜，营造电影感。
- **左右布局**：
  - 左侧 (30%)：显示 2:3 垂直海报。
  - 右侧 (70%)：大字号标题 -> 年份/分级/时长标签行 -> **豆瓣/IMDb 评分标签** -> 剧情简介 -> 演职人员头像列表。
- **播放按钮**：在左侧海报上方或右侧标题下方放置醒目的“圆形播放”大按钮。

## 阶段 13：自动化媒体刮削引擎 (Metadata Scraping Engine)
**目标**：实现视频上传后，后端自动识别影片并获取封面、简介、评分等元数据。

### 1. 文件命名识别协议 (File Naming Protocol)
- **标准格式**：后端需实现正则表达式解析器，识别 `Movie_Name (Year).mkv` 格式（如：`The_Matrix (1999).mkv`）。

### 2. 刮削 API 集成 (Scraping APIs)
- **通用数据源 (TheMovieDB - TMDB)**：
  - 调用 TMDB API 获取影片元数据（简介、英文海报、演职人员）。
- **国产数据源 (豆瓣 scraping)**：
  - （可选）集成豆瓣第三方 API 或脚本，优先刮削中文海报（更符合北京/深圳用户习惯）和豆瓣评分。

### 3. 异步流程控制 (Async Workflow)
- **刮削流水线**：
  1. 视频上传完成。
  2. 触发 `ScrapingService` 异步任务（@Async）。
  3. 解析文件名 -> 调用 TMDB API -> **下载海报到本地 /data/posters/** -> 将影片信息、评分存入 `video_metadata` 表。
  4. 更新视频状态为“已刮削”。
  
  
## 阶段 14：管理控制台与系统配置 (Admin & Configuration)
**目标**：构建可视化后台，管理 API 密钥、媒体库和系统状态。

### 1. 后端管理接口 (Admin REST APIs)
- **配置中心**：实现 `/api/admin/config` 接口，支持持久化存储以下参数：
  - `TMDB_API_KEY`: 用于全球电影数据刮削。
  - `DOUBAN_SCRAPER_MODE`: 切换豆瓣刮削策略（API 或动态爬虫）。
  - `TRANSCODE_THREAD_COUNT`: 限制并行转码线程数（针对 RK3588 优化）。
- **媒体运维**：
  - `DELETE /api/admin/videos/{id}`: 级联删除数据库记录、源 MKV 文件及 HLS 切片目录。
  - `POST /api/admin/videos/rescrap`: 强制对特定视频重新执行刮削逻辑。

### 2. 前端控制台排版 (Dashboard UI)
- **布局设计**：采用侧边栏导航 (Sidebar Layout)，分为：【仪表盘】、【媒体库管理】、【系统设置】。
- **媒体看板**：
  - 使用 **表格视图 (Table View)** 展示：封面预览、文件名、转码进度条 (Progress Bar)、刮削状态标签。
  - 操作列：【播放】、【重新刮削】、【编辑元数据】、【删除】。
- **设置页面**：使用 `Card` 组件分类配置 API Key，支持“点击查看/隐藏”敏感信息，并提供“测试连接”按钮。

### 3. 多源刮削策略 (Hybrid Scraping Logic)
- **优先级逻辑**：
  - 默认流：解析文件名 -> 优先匹配 TMDB (获取剧照/演职员) -> 补充匹配豆瓣 (获取中文简介/国内评分)。
- **错误处理**：若 TMDB 连接失败，自动切换回本地 Mock 数据或仅保留基础文件名。


---
# 技能扩展：阿里云盘与多源刮削专家
---

## 阶段 15：管理控制台原型规范 (Admin Dashboard UI/UX)
**目标**：构建响应式、侧边栏排版的 Vue 3 管理后台。

### 1. 侧边栏导航 (Sidebar Navigation)
- **核心菜单**：
  - 【仪表盘】(Dashboard): 显示系统状态、转码队列、最近刮削。
  - 【媒体库管理】(Media Library): 表格视图，支持搜索、删除、重新刮削。
  - 【阿里云盘】(Aliyun Drive): **核心：支持在线搜索云盘资源、一键下载。**
  - 【系统设置】(Settings): 配置 TMDB Key、豆瓣 Scraping 策略、阿里云盘 RefreshToken。

### 2. 交互组件
- **视频管理表格**：需包含封面预览、文件名、转码进度条 (`<v-progress-linear>`)、刮削状态 (成功/失败/进行中标签)。
- **设置卡片**：使用 `v-card` 分类配置 API Key，支持点击眼睛图标隐藏/显示敏感信息。

### 3. 路由与状态约束 (Router & State Constraints)
- **禁止使用原生事件驱动**：禁止在 HTML 模板中使用 `onclick` 或全局 `showPage` 切换视图。
- **强制使用 Vue Router**：所有菜单跳转必须基于 `vue-router`，使用 `<router-link>` 配合 `active-class` 实现选中状态的高亮。
- **页面组件化**：每个后台子模块（如阿里云盘搜索）必须是独立的 `.vue` 组件，存储在 `views/admin/` 目录下。

## 阶段 16：阿里云盘 OpenAPI 集成协议 (Aliyun Drive Integration)
**目标**：实现阿里云盘资源的在线搜索、资源选中后的后台异步下载及自动触发转码流程。

### 1. 认证与令牌管理 (Auth & Token)
- **RefreshToken 持久化**：后端需实现 `AliyunAuthService`，将 RefreshToken 存入 MySQL，并定时调用 API 刷新 `access_token`。

### 2. 云端搜索与下载流水线 (Search & Download Pipeline)
- **前端云端搜索**：在 Vue 中增加搜索框，调用后端 `/api/aliyun/search?keyword=...` 接口，展示云盘文件列表（文件名、大小）。
- **后端异步下载**：
  - 调用阿里云盘文件下载接口获取真实 URL。
  - 使用 Spring Boot 的异步任务 (`@Async`) 或第三方库（如 Java-wget）将文件下载到本地 **/data/downloads/incoming** 目录。
- **自动化转码触发**：下载完成后，**自动** 将本地文件路径传递给 `VideoTranscodeService` 启动 HLS 转码流程。

## 阶段 17：多源并发刮削状态机 (Multi-source Concurrent Scraping)
**目标**：针对单个视频，同时启动 TMDB 和豆瓣刮削任务，并通过状态机聚合结果，优先展示中文元数据。

### 1. 并发模型 (Concurrency Model)
- **CompletableFuture 聚合**：使用 `CompletableFuture.allOf()` 同时发起对 TMDB 和豆瓣的请求。
- **超时控制**：必须为刮削请求设置超时（如 5 秒），防止因单个源网络问题拖慢整个流程。

### 2. 元数据聚合逻辑 (Metadata Merging Logic)
- **标题与简介**：优先使用豆瓣刮削到的 **中文** 标题和简介；若失败，降级使用 TMDB 的英文数据。
- **海报与剧照**：优先使用豆瓣的 **垂直海报**（更符合国内习惯）；TMDB 用于补充演职人员剧照。
- **评分**：同时保存 TMDB 评分和豆瓣评分，前端优先展示豆瓣评分。


### 18. 工程化架构准则 (Engineering Standards)
- **拒绝全局变量**：禁止通过全局 `var` 或 `function` 定义组件。
- **模块化强制**：所有前端代码必须基于 **ES6 Modules**。组件必须是独立的 `.vue` 文件。
- **构建工具**：必须配置 `vite.config.js`，确保开发环境具备热更新功能。
- **严格依赖序**：所有第三方库（如 Vue, Vue Router）必须通过 `npm` 管理，严禁在 HTML 中混用多个版本的 CDN 链接。  

## 阶段 19：基于 AList 的全网盘集成 (AList Integration)
**目标**：通过 AList 中转层，实现对阿里云盘等主流网盘的搜索与自动化下载。

### 1. 挂载与连接规范 (Connection)
- **AList 客户端**：后端必须支持配置 `ALIST_ADMIN_URL` 和 `ALIST_TOKEN`。
- **WebDAV 支持**：优先使用 WebDAV 协议进行文件列取，确保存取性能。

### 2. 搜索与下载流 (Search & Sync Flow)
- **云端搜索**：通过 AList 的 `/api/fs/search` 接口实现全盘搜索。
- **流式下载 (Streaming Download)**：后端获取 AList 生成的直链后，使用 `HttpURLConnection` 或 `WebClient` 将文件流式拉取到本地存储，下载完成后自动触发转码。

### 3. 本地化部署 (DevOps Deployment)
- **Docker Compose 增强**：在编排中增加 `alist` 镜像。
  - `image: xhofe/alist:latest`
  - `volumes: - ./alist_data:/opt/alist/data`
  
### 4. AList 驱动兼容性校验 (Driver Compatibility)
- **强制 Open 协议**：后端调用 AList 搜索接口时，必须检查 mount_path 下的驱动类型是否为 `AliyunDriveOpen`。
- **错误代码拦截**：若 AList 返回 "failed to refresh token"，后端需解析出 "You should use the token with aliyundrive open" 关键字，并在控制台显著位置提示用户：“Token 类型错误，请重新获取阿里云盘 Open 专用 Token”。 

### 5. 云盘状态直观校验 (Connection Validation)
- **页面加载协议**：前端网盘搜索页面必须在 `onMounted` 生命周期内自动发起 `GET_DEFAULT_FILES` 请求。
- **UI 反馈**：若请求成功，必须直接渲染文件网格；若失败，必须替换为错误状态灯 (Error State Lamp)，用于直观校验连接。
- **交互规范**：文件展示列表应支持点击目录（`<router-link :to="{query: {path: ...}}">`），进入下一级目录进行浏览，直至找到可下载的电影文件。 
  
## 阶段 20：第三方服务健康监测 (Third-party Health Check)
**目标**：确保 AList 与阿里云盘的连接持久可用。

### 1. Token 异常预警
- **后端逻辑**：当调用 AList API 返回 `401 Unauthorized` 或 `500` 错误时，检查错误信息是否包含 "refresh token expired"。
- **控制台通知**：在 Vue 后台首页增加一个“系统状态”组件。若 Token 失效，将状态灯转红，并提示“请重新扫码更新阿里云盘 Token”。

### 2. 自动保存新 Token
- **配置同步**：确保 AList 的 `data/` 目录已挂载到宿主机。AList 自动续期产生的新 Token 会保存在数据库中，挂载卷能保证重启后依然使用最新的 Token。  


## 阶段 21：强制构建与验证协议 (Strict Build & Validation Protocol)
**目标**：确保每一行代码修改都能即时同步到运行中的容器，并经过自动化验证。

### 1. 强制重新构建 (Forced Rebuild)
- **前后端变更触发**：每当 OpenCode 修改了 `src/` (Vue) 或 `java/` (Spring Boot) 下的代码，**必须**自动执行相应的构建指令。
  - **前端**：执行 `npm run build` 并触发 `docker build -t video-ui .`。
  - **后端**：执行 `./mvnw clean package` 并触发 `docker build -t video-api .`。
- **容器重启**：构建完成后，必须执行 `docker-compose up -d --force-recreate [service_name]`，严禁只改代码不重启容器。

### 2. 自动化冒烟测试 (Automated Smoke Test)
- **连通性校验**：重启后，OpenCode 必须自动调用 `curl -I http://localhost:8080/health` (后端) 和检查 `80` 端口 (前端) 响应。
- **报错拦截**：如果日志中出现 `Uncaught ReferenceError` 或 `500 Internal Server Error`，OpenCode 必须自动撤回修改并重新诊断。

### 3. 环境一致性检查 (Consistency Check)
- **配置同步**：每次修改 `application.yml` 后，必须检查容器内的环境变量是否生效，防止 AList Token 等配置因缓存失效。