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
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_uuid (uuid)
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
('tmdb.language', 'zh-CN', 'TMDB API Language');
