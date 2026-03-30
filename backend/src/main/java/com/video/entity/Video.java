package com.video.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "video_metadata")
public class Video {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 36)
    private String uuid;
    
    @Column(nullable = false)
    private String title;
    
    @Column(name = "original_filename", length = 500)
    private String originalFilename;
    
    @Column(name = "original_path", length = 1000)
    private String originalPath;
    
    @Column(name = "hls_path", length = 1000)
    private String hlsPath;
    
    @Column(name = "thumbnail_path", length = 1000)
    private String thumbnailPath;
    
    @Column(name = "duration")
    private Long duration = 0L;
    
    @Column(name = "file_size")
    private Long fileSize = 0L;
    
    @Column
    private Integer width = 0;
    
    @Column
    private Integer height = 0;
    
    @Column(nullable = false, length = 20)
    private String status = "pending";
    
    @Column(name = "current_position")
    private Long currentPosition = 0L;
    
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
}