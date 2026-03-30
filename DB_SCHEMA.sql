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