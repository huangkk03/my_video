package com.video.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transcode_tasks")
public class TranscodeTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_id", nullable = false)
    private Long videoId;

    @Column(name = "video_uuid", nullable = false, length = 36)
    private String videoUuid;

    @Column
    private Integer progress = 0;

    @Column(name = "ffmpeg_command", columnDefinition = "TEXT")
    private String ffmpegCommand;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    @Column(nullable = false, length = 20)
    private String status = "queued";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "download_status", length = 20)
    private String downloadStatus = "pending";

    @Column(name = "download_progress")
    private Integer downloadProgress = 0;

    @Column(name = "download_path", length = 500)
    private String downloadPath;

    @Column(name = "download_bytes")
    private Long downloadBytes = 0L;

    @Column(name = "total_bytes")
    private Long totalBytes = 0L;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}