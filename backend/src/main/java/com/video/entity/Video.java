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

    @Column(name = "source_type", length = 20)
    private String sourceType = "local_file";

    @Column(name = "alist_path", length = 1000)
    private String alistPath;
    
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
    
    @Column(name = "category_id")
    private Long categoryId;
    
    @Column(columnDefinition = "TEXT")
    private String overview;
    
    @Column(name = "poster_path", length = 1000)
    private String posterPath;
    
    @Column(name = "backdrop_path", length = 1000)
    private String backdropPath;
    
    @Column(name = "tmdb_id")
    private Long tmdbId;
    
    @Column(name = "imdb_id", length = 20)
    private String imdbId;
    
    @Column(name = "douban_id", length = 20)
    private String doubanId;
    
    @Column
    private Double rating;
    
    @Column(name = "release_year")
    private Integer releaseYear;
    
    @Column(length = 500)
    private String genres;
    
    @Column(columnDefinition = "TEXT")
    private String actors;
    
    @Column(name = "director", length = 200)
    private String director;
    
    @Column(name = "actor_list_json", columnDefinition = "TEXT")
    private String actorListJson;
    
    @Column(name = "scraping_status", length = 20)
    private String scrapingStatus = "pending";
    
    @Column(name = "series_id")
    private Long seriesId;
    
    @Column(name = "season_id")
    private Long seasonId;
    
    @Column(name = "episode_number")
    private Integer episodeNumber;
    
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