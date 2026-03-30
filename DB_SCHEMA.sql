-- MKV Video Platform Database Schema

-- 视频元数据表
CREATE TABLE IF NOT EXISTS video_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE COMMENT '视频唯一标识',
    title VARCHAR(255) NOT NULL COMMENT '视频标题',
    original_filename VARCHAR(500) COMMENT '原始文件名',
    original_path VARCHAR(1000) COMMENT '原始视频存储路径',
    hls_path VARCHAR(1000) COMMENT 'HLS输出路径',
    thumbnail_path VARCHAR(1000) COMMENT '缩略图路径',
    duration BIGINT DEFAULT 0 COMMENT '视频时长(毫秒)',
    file_size BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
    width INT DEFAULT 0 COMMENT '视频宽度',
    height INT DEFAULT 0 COMMENT '视频高度',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending(待处理), transcoding(转码中), completed(完成), failed(失败)',
    current_position BIGINT DEFAULT 0 COMMENT '当前播放位置(毫秒)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 转码任务表
CREATE TABLE IF NOT EXISTS transcode_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id BIGINT NOT NULL COMMENT '关联video_metadata.id',
    video_uuid VARCHAR(36) NOT NULL COMMENT '视频UUID',
    progress INT DEFAULT 0 COMMENT '转码进度(0-100)',
    ffmpeg_command TEXT COMMENT 'FFmpeg命令',
    error_message TEXT COMMENT '错误信息',
    started_at TIMESTAMP NULL COMMENT '开始时间',
    completed_at TIMESTAMP NULL COMMENT '完成时间',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    max_retries INT DEFAULT 3 COMMENT '最大重试次数',
    status VARCHAR(20) DEFAULT 'queued' COMMENT '状态: queued(队列中), processing(处理中), completed(完成), failed(失败)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (video_id) REFERENCES video_metadata(id) ON DELETE CASCADE,
    INDEX idx_video_uuid (video_uuid),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 播放进度记录表 (可选，用于更详细的进度追踪)
CREATE TABLE IF NOT EXISTS playback_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id BIGINT NOT NULL,
    user_identifier VARCHAR(100) COMMENT '用户标识(可基于IP或Cookie)',
    position BIGINT DEFAULT 0 COMMENT '播放位置(毫秒)',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (video_id) REFERENCES video_metadata(id) ON DELETE CASCADE,
    INDEX idx_video_user (video_id, user_identifier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 分类表
CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL COMMENT '分类名称',
    slug VARCHAR(100) NOT NULL UNIQUE COMMENT '分类别名',
    description VARCHAR(500) COMMENT '分类描述',
    parent_id BIGINT DEFAULT NULL COMMENT '父分类ID',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_slug (slug),
    INDEX idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户表
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    nickname VARCHAR(100) COMMENT '昵称',
    avatar VARCHAR(500) COMMENT '头像URL',
    role VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色: admin(管理员), user(普通用户)',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active(活跃), disabled(禁用)',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 系统配置表
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    config_type VARCHAR(20) DEFAULT 'string' COMMENT '类型: string, number, boolean, json',
    description VARCHAR(500) COMMENT '描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 更新视频元数据表，添加分类、刮削字段
ALTER TABLE video_metadata 
    ADD COLUMN category_id BIGINT DEFAULT NULL COMMENT '分类ID',
    ADD COLUMN overview TEXT COMMENT '简介/剧情',
    ADD COLUMN poster_path VARCHAR(1000) COMMENT '海报图路径',
    ADD COLUMN backdrop_path VARCHAR(1000) COMMENT '背景图路径',
    ADD COLUMN tmdb_id BIGINT DEFAULT NULL COMMENT 'TMDB ID',
    ADD COLUMN imdb_id VARCHAR(20) COMMENT 'IMDB ID',
    ADD COLUMN douban_id VARCHAR(20) COMMENT '豆瓣ID',
    ADD COLUMN rating DOUBLE DEFAULT NULL COMMENT '评分',
    ADD COLUMN release_year INT DEFAULT NULL COMMENT '上映年份',
    ADD COLUMN genres VARCHAR(500) COMMENT '类型(多个用逗号分隔)',
    ADD COLUMN actors TEXT COMMENT '演员(JSON数组)',
    ADD COLUMN director VARCHAR(200) COMMENT '导演',
    ADD COLUMN scraping_status VARCHAR(20) DEFAULT 'pending' COMMENT '刮削状态: pending, success, failed',
    ADD COLUMN FOREIGN KEY (category_id) REFERENCES category(id) ON DELETE SET NULL,
    ADD INDEX idx_category_id (category_id),
    ADD INDEX idx_title (title);

-- 导入任务表（用于阿里云盘导入）
CREATE TABLE IF NOT EXISTS import_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id BIGINT DEFAULT NULL COMMENT '关联视频ID',
    source_type VARCHAR(20) NOT NULL COMMENT '来源: aliyun, local, upload',
    source_id VARCHAR(100) COMMENT '来源ID',
    source_name VARCHAR(500) COMMENT '来源文件名',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending, downloading, transcoding, completed, failed',
    progress INT DEFAULT 0 COMMENT '进度(0-100)',
    error_message TEXT COMMENT '错误信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_video_id (video_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 插入默认分类
INSERT INTO category (name, slug, description, sort_order) VALUES
('电影', 'movies', '电影分类', 1),
('电视剧', 'tv', '电视剧分类', 2),
('动漫', 'anime', '动漫分类', 3),
('纪录片', 'documentary', '纪录片分类', 4),
('综艺', 'variety', '综艺分类', 5);

-- 插入默认管理员账号 (密码: admin123)
INSERT INTO user (username, password, nickname, role) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'admin');