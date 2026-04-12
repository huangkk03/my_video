package com.video.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "download_queue")
public class DownloadQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_id", nullable = false, unique = true, length = 36)
    private String taskId;

    @Column(name = "source_url", nullable = false, length = 2000)
    private String sourceUrl;

    @Column(name = "source_name", nullable = false, length = 500)
    private String sourceName;

    @Column(name = "source_size")
    private Long sourceSize = 0L;

    @Column(name = "save_path", length = 1000)
    private String savePath;

    @Column(nullable = false, length = 20)
    private String status = "pending";

    @Column
    private Integer progress = 0;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column
    private Integer priority = 0;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "video_uuid", length = 36)
    private String videoUuid;

    @Column(name = "folder_id")
    private Long folderId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
