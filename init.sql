-- MKV Video Platform Database Initialization
-- This script runs automatically when MySQL container starts

CREATE DATABASE IF NOT EXISTS video_platform;
USE video_platform;

-- Video metadata table
CREATE TABLE IF NOT EXISTS video_metadata (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE COMMENT 'Video unique identifier',
    title VARCHAR(255) NOT NULL COMMENT 'Video title',
    original_filename VARCHAR(500) COMMENT 'Original filename',
    original_path VARCHAR(1000) COMMENT 'Original video storage path',
    hls_path VARCHAR(1000) COMMENT 'HLS output path',
    thumbnail_path VARCHAR(1000) COMMENT 'Thumbnail path',
    duration BIGINT DEFAULT 0 COMMENT 'Duration in milliseconds',
    file_size BIGINT DEFAULT 0 COMMENT 'File size in bytes',
    width INT DEFAULT 0 COMMENT 'Video width',
    height INT DEFAULT 0 COMMENT 'Video height',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'Status: pending, transcoding, completed, failed',
    current_position BIGINT DEFAULT 0 COMMENT 'Current playback position',
    category_id BIGINT DEFAULT NULL COMMENT 'Category ID',
    overview TEXT COMMENT 'Overview/description',
    poster_path VARCHAR(1000) COMMENT 'Poster image path',
    backdrop_path VARCHAR(1000) COMMENT 'Backdrop image path',
    tmdb_id BIGINT DEFAULT NULL COMMENT 'TMDB ID',
    imdb_id VARCHAR(20) DEFAULT NULL COMMENT 'IMDB ID',
    douban_id VARCHAR(20) DEFAULT NULL COMMENT 'Douban ID',
    rating DOUBLE DEFAULT NULL COMMENT 'Rating',
    release_year INT DEFAULT NULL COMMENT 'Release year',
    genres VARCHAR(500) DEFAULT NULL COMMENT 'Genres (comma separated)',
    actors TEXT DEFAULT NULL COMMENT 'Actors (JSON array)',
    director VARCHAR(200) DEFAULT NULL COMMENT 'Director',
    scraping_status VARCHAR(20) DEFAULT 'pending' COMMENT 'Scraping status: pending, success, failed',
    series_id BIGINT DEFAULT NULL COMMENT 'Series ID',
    season_id BIGINT DEFAULT NULL COMMENT 'Season ID',
    episode_number INT DEFAULT NULL COMMENT 'Episode number',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_uuid (uuid),
    INDEX idx_category_id (category_id),
    INDEX idx_series_id (series_id),
    INDEX idx_season_id (season_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Transcode tasks table
CREATE TABLE IF NOT EXISTS transcode_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id BIGINT NOT NULL,
    video_uuid VARCHAR(36) NOT NULL,
    progress INT DEFAULT 0 COMMENT 'Transcode progress 0-100',
    ffmpeg_command TEXT COMMENT 'FFmpeg command',
    error_message TEXT COMMENT 'Error message',
    started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    retry_count INT DEFAULT 0,
    max_retries INT DEFAULT 3,
    status VARCHAR(20) DEFAULT 'queued' COMMENT 'Status: queued, processing, completed, failed',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (video_id) REFERENCES video_metadata(id) ON DELETE CASCADE,
    INDEX idx_video_uuid (video_uuid),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Playback progress table
CREATE TABLE IF NOT EXISTS playback_progress (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    video_id BIGINT NOT NULL,
    user_identifier VARCHAR(100) COMMENT 'User identifier',
    position BIGINT DEFAULT 0 COMMENT 'Playback position',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (video_id) REFERENCES video_metadata(id) ON DELETE CASCADE,
    INDEX idx_video_user (video_id, user_identifier)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Series table
CREATE TABLE IF NOT EXISTS series (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL COMMENT 'Series name',
    slug VARCHAR(255) UNIQUE COMMENT 'URL friendly name',
    poster_path VARCHAR(1000) COMMENT 'Poster image path',
    backdrop_path VARCHAR(1000) COMMENT 'Backdrop image path',
    overview TEXT COMMENT 'Series overview',
    tmdb_id BIGINT DEFAULT NULL COMMENT 'TMDB series ID',
    category_id BIGINT DEFAULT NULL COMMENT 'Category ID',
    sort_order INT DEFAULT 0 COMMENT 'Sort order',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_category_id (category_id),
    INDEX idx_tmdb_id (tmdb_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Season table
CREATE TABLE IF NOT EXISTS season (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    series_id BIGINT NOT NULL COMMENT 'Series ID',
    season_number INT NOT NULL COMMENT 'Season number',
    name VARCHAR(255) COMMENT 'Season name (e.g. Season 1)',
    poster_path VARCHAR(1000) COMMENT 'Season poster path',
    overview TEXT COMMENT 'Season overview',
    tmdb_id BIGINT DEFAULT NULL COMMENT 'TMDB season ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (series_id) REFERENCES series(id) ON DELETE CASCADE,
    UNIQUE KEY uk_series_season (series_id, season_number),
    INDEX idx_series_id (series_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- System configuration table
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT 'Config key',
    config_value TEXT COMMENT 'Config value',
    description VARCHAR(500) COMMENT 'Description',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Import tasks table - tracks cloud media import flow
CREATE TABLE IF NOT EXISTS import_tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id VARCHAR(36) NOT NULL UNIQUE COMMENT 'Unique task ID',
    source_name VARCHAR(500) COMMENT 'Source file name',
    source_path VARCHAR(1000) COMMENT 'AList file path',
    source_size BIGINT DEFAULT 0 COMMENT 'Source file size',
    status VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'Status: pending, downloading, scraping, transcoding, completed, failed',
    progress INT DEFAULT 0 COMMENT 'Progress 0-100',
    message VARCHAR(500) COMMENT 'Status message',
    video_uuid VARCHAR(36) COMMENT 'Associated video UUID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id),
    INDEX idx_status (status),
    INDEX idx_video_uuid (video_uuid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert default configs
INSERT INTO system_config (config_key, config_value, description) VALUES 
('alist.storage_password', '', 'AList 存储密码'),
('tmdb.api_key', '', 'TMDB API Key'),
('tmdb.language', 'zh-CN', 'TMDB API Language'),
('aliyundrive.refresh_token', '', '阿里云盘 Refresh Token'),
('aliyundrive.root_folder_id', 'root', '阿里云盘根目录 ID'),
('transcode_max_concurrent', '2', '最大并发转码数'),
('transcode_quality', '23', '视频质量 CRF 值');

-- Category table
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

-- User table
CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    nickname VARCHAR(100) COMMENT '昵称',
    avatar VARCHAR(500) COMMENT '头像URL',
    role VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色: admin, user',
    status VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态: active, disabled',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Insert default categories
INSERT INTO category (name, slug, description, sort_order) VALUES
('电影', 'movies', '电影分类', 1),
('电视剧', 'tv', '电视剧分类', 2),
('动漫', 'anime', '动漫分类', 3),
('纪录片', 'documentary', '纪录片分类', 4),
('综艺', 'variety', '综艺分类', 5);

-- Insert default admin (password: admin123)
INSERT INTO user (username, password, nickname, role) VALUES 
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 'admin');
