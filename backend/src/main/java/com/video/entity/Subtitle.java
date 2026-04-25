package com.video.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "video_subtitles")
public class Subtitle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "video_uuid", nullable = false, length = 36)
    private String videoUuid;

    @Column(name = "language", nullable = false, length = 10)
    private String language;

    @Column(name = "open_subtitles_id", length = 50)
    private String openSubtitlesId;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "status", nullable = false, length = 20)
    private String status = "pending";

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "cd_number")
    private Integer cdNumber;

    @Column(name = "vote_count")
    private Integer voteCount;

    @Column(name = "downloaded_at")
    private LocalDateTime downloadedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_SEARCHING = "searching";
    public static final String STATUS_DOWNLOADED = "downloaded";
    public static final String STATUS_FAILED = "failed";
    public static final String STATUS_NOT_FOUND = "not_found";
}