package com.video.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "season", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"series_id", "season_number"})
})
public class Season {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "series_id", nullable = false)
    private Long seriesId;
    
    @Column(name = "season_number", nullable = false)
    private Integer seasonNumber;
    
    @Column
    private String name;
    
    @Column(name = "poster_path", length = 1000)
    private String posterPath;
    
    @Column(columnDefinition = "TEXT")
    private String overview;
    
    @Column(name = "tmdb_id")
    private Long tmdbId;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (name == null || name.isEmpty()) {
            name = "第 " + seasonNumber + " 季";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
