package com.video.entity;

import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "series")
public class Series {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;
    
    @Column(unique = true)
    private String slug;
    
    @Column(name = "poster_path", length = 1000)
    private String posterPath;
    
    @Column(name = "backdrop_path", length = 1000)
    private String backdropPath;
    
    @Column(columnDefinition = "LONGTEXT")
    private String overview;
    
    @Column(name = "tmdb_id")
    private Long tmdbId;
    
    @Column(name = "category_id")
    private Long categoryId;
    
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (slug == null || slug.isEmpty()) {
            String baseSlug = name != null ? name.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "-").toLowerCase() : "series";
            if (baseSlug.length() > 200) {
                baseSlug = baseSlug.substring(0, 200);
            }
            slug = baseSlug + "-" + System.currentTimeMillis();
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
