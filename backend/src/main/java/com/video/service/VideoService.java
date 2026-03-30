package com.video.service;

import com.video.dto.VideoProgressRequest;
import com.video.dto.VideoUploadResponse;
import com.video.entity.TranscodeTask;
import com.video.entity.Video;
import com.video.repository.TranscodeTaskRepository;
import com.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {
    
    private static final String VIDEO_STORAGE_PATH = "/data/videos";
    
    private final VideoRepository videoRepository;
    private final TranscodeTaskRepository transcodeTaskRepository;
    private final TranscodeService transcodeService;
    private final ScrapingAggregationService scrapingService;
    
    @Value("${video.base-url:http://localhost:8080}")
    private String baseUrl;
    
    public VideoUploadResponse uploadVideo(MultipartFile file, String title) {
        try {
            String uuid = UUID.randomUUID().toString();
            String originalFilename = file.getOriginalFilename();
            String videoTitle = title != null && !title.isEmpty() ? title : 
                (originalFilename != null ? originalFilename.substring(0, originalFilename.lastIndexOf('.')) : "Untitled");
            
            Path videoDir = Paths.get(VIDEO_STORAGE_PATH, uuid);
            Files.createDirectories(videoDir);
            
            Path originalPath = videoDir.resolve("original").resolve(originalFilename);
            Files.createDirectories(originalPath.getParent());
            Files.copy(file.getInputStream(), originalPath, StandardCopyOption.REPLACE_EXISTING);
            
            Video video = new Video();
            video.setUuid(uuid);
            video.setTitle(videoTitle);
            video.setOriginalFilename(originalFilename);
            video.setOriginalPath(originalPath.toString());
            video.setFileSize(file.getSize());
            video.setStatus("pending");
            
            video = videoRepository.save(video);
            
            transcodeService.transcodeVideo(uuid);
            
            return VideoUploadResponse.of(uuid, "上传成功，转码任务已创建");
            
        } catch (Exception e) {
            log.error("Failed to upload video", e);
            throw new RuntimeException("视频上传失败: " + e.getMessage());
        }
    }
    
    public Page<Video> getVideoList(int page, int size) {
        return videoRepository.findAll(PageRequest.of(page, size));
    }
    
    public Video getVideoByUuid(String uuid) {
        return videoRepository.findByUuid(uuid).orElse(null);
    }
    
    public void updateProgress(String uuid, Long position) {
        Video video = videoRepository.findByUuid(uuid).orElse(null);
        if (video != null) {
            video.setCurrentPosition(position);
            videoRepository.save(video);
        }
    }
    
    public void deleteVideo(String uuid) {
        Video video = videoRepository.findByUuid(uuid).orElse(null);
        if (video != null) {
            try {
                Path videoDir = Paths.get(VIDEO_STORAGE_PATH, uuid);
                Files.walk(videoDir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (Exception e) {
                            log.warn("Failed to delete: {}", path);
                        }
                    });
            } catch (Exception e) {
                log.warn("Failed to delete video files: {}", uuid);
            }
            videoRepository.delete(video);
        }
    }
    
    public TranscodeTask getTranscodeTask(String uuid) {
        return transcodeTaskRepository.findByVideoUuid(uuid).orElse(null);
    }
    
    public void rescrapVideo(String uuid) {
        Video video = videoRepository.findByUuid(uuid).orElse(null);
        if (video == null) {
            throw new RuntimeException("Video not found: " + uuid);
        }
        
        try {
            ScrapingAggregationService.MetadataResult result = 
                scrapingService.searchMetadata(video.getTitle()).get();
            
            if (result != null) {
                if (result.getTitle() != null) video.setTitle(result.getTitle());
                if (result.getOverview() != null) video.setOverview(result.getOverview());
                if (result.getPosterUrl() != null) video.setPosterPath(result.getPosterUrl());
                if (result.getRating() != null) video.setRating(result.getRating());
                if (result.getReleaseDate() != null && result.getReleaseDate().length() >= 4) {
                    try {
                        video.setReleaseYear(Integer.parseInt(result.getReleaseDate().substring(0, 4)));
                    } catch (Exception ignored) {}
                }
                video.setScrapingStatus("success");
            } else {
                video.setScrapingStatus("failed");
            }
        } catch (Exception e) {
            log.error("Scraping failed for video: {}", uuid, e);
            video.setScrapingStatus("failed");
        }
        
        videoRepository.save(video);
    }
    
    public UploadResult processVideoFile(java.io.File videoFile, String title) {
        try {
            String uuid = UUID.randomUUID().toString();
            String originalFilename = videoFile.getName();
            String videoTitle = title != null && !title.isEmpty() ? title : 
                originalFilename.substring(0, originalFilename.lastIndexOf('.'));
            
            Path videoDir = Paths.get(VIDEO_STORAGE_PATH, uuid);
            Files.createDirectories(videoDir);
            
            Path originalPath = videoDir.resolve("original").resolve(originalFilename);
            Files.createDirectories(originalPath.getParent());
            Files.copy(videoFile.toPath(), originalPath, StandardCopyOption.REPLACE_EXISTING);
            
            Video video = new Video();
            video.setUuid(uuid);
            video.setTitle(videoTitle);
            video.setOriginalFilename(originalFilename);
            video.setOriginalPath(originalPath.toString());
            video.setFileSize(Files.size(originalPath));
            video.setStatus("pending");
            
            video = videoRepository.save(video);
            
            transcodeService.transcodeVideo(uuid);
            
            return new UploadResult(uuid, "处理成功");
            
        } catch (Exception e) {
            log.error("Failed to process video file", e);
            throw new RuntimeException("视频处理失败: " + e.getMessage());
        }
    }
    
    public static class UploadResult {
        private final String uuid;
        private final String message;
        
        public UploadResult(String uuid, String message) {
            this.uuid = uuid;
            this.message = message;
        }
        
        public String getUuid() { return uuid; }
        public String getMessage() { return message; }
    }
}