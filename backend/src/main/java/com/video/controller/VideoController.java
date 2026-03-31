package com.video.controller;

import com.video.dto.VideoProgressRequest;
import com.video.dto.VideoUploadResponse;
import com.video.entity.Video;
import com.video.service.CloudStorageService;
import com.video.service.TranscodeService;
import com.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileUrlResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/videos")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class VideoController {
    
    private final VideoService videoService;
    private final TranscodeService transcodeService;
    private final CloudStorageService cloudStorageService;
    
    @PostMapping("/upload")
    public ResponseEntity<VideoUploadResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title) {
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(VideoUploadResponse.of(null, "请选择要上传的视频文件"));
        }
        
        if (file.getSize() > 500 * 1024 * 1024) {
            return ResponseEntity.badRequest()
                .body(VideoUploadResponse.of(null, "文件大小不能超过500MB"));
        }
        
        String filename = file.getOriginalFilename();
        if (filename != null && !isVideoFile(filename)) {
            return ResponseEntity.badRequest()
                .body(VideoUploadResponse.of(null, "不支持的视频格式"));
        }
        
        if (title != null && title.contains("..")) {
            return ResponseEntity.badRequest()
                .body(VideoUploadResponse.of(null, "无效的标题"));
        }
        
        VideoUploadResponse response = videoService.uploadVideo(file, title);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping
    public ResponseEntity<Page<Video>> getVideoList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Video> videos = videoService.getVideoList(page, size);
        return ResponseEntity.ok(videos);
    }
    
    @GetMapping("/{uuid}")
    public ResponseEntity<Video> getVideo(@PathVariable String uuid) {
        Video video = videoService.getVideoByUuid(uuid);
        if (video == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(video);
    }
    
    @GetMapping("/{uuid}/stream")
    public ResponseEntity<Resource> streamVideo(@PathVariable String uuid) {
        Video video = videoService.getVideoByUuid(uuid);
        if (video == null || video.getHlsPath() == null) {
            return ResponseEntity.notFound().build();
        }
        
        try {
            Path hlsPath = Paths.get(video.getHlsPath());
            Resource resource = new FileUrlResource(hlsPath.toString());
            
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.apple.mpegurl"));
            headers.setContentDispositionFormData("attachment", 
                URLEncoder.encode(video.getTitle(), "UTF-8") + ".m3u8");
            
            long fileSize = resource.contentLength();
            headers.setContentLength(fileSize);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
                
        } catch (Exception e) {
            log.error("Failed to stream video: {}", uuid, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{uuid}/ts/{segment}")
    public ResponseEntity<Resource> streamSegment(
            @PathVariable String uuid,
            @PathVariable String segment) {
        
        try {
            String storagePath = transcodeService.getVideoStoragePath(uuid);
            Path segmentPath = Paths.get(storagePath, segment);
            Resource resource = new FileUrlResource(segmentPath.toString());
            
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(resource.contentLength());
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
                
        } catch (Exception e) {
            log.error("Failed to stream segment: {}/{}", uuid, segment, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{uuid}/hls/{pathSegment}/**")
    public ResponseEntity<Resource> streamHlsSegment(
            @PathVariable String uuid,
            @PathVariable String pathSegment,
            javax.servlet.http.HttpServletRequest request) {
        
        String remainingPath = request.getRequestURI().substring(
            request.getRequestURI().indexOf(pathSegment) + pathSegment.length() + 1);
        
        try {
            String storagePath = transcodeService.getVideoStoragePath(uuid);
            Path fullPath = Paths.get(storagePath, pathSegment, remainingPath);
            Resource resource = new FileUrlResource(fullPath.toString());
            
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(resource.contentLength());
            headers.set("Access-Control-Allow-Origin", "*");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
                
        } catch (Exception e) {
            log.error("Failed to stream HLS segment: {}", request.getRequestURI(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @GetMapping("/{uuid}/index{segment:\\d+\\.ts}")
    public ResponseEntity<Resource> streamIndexSegment(
            @PathVariable String uuid,
            @PathVariable String segment) {
        
        try {
            String storagePath = transcodeService.getVideoStoragePath(uuid);
            Path segmentPath = Paths.get(storagePath, "index" + segment);
            Resource resource = new FileUrlResource(segmentPath.toString());
            
            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(resource.contentLength());
            headers.set("Access-Control-Allow-Origin", "*");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(resource);
                
        } catch (Exception e) {
            log.error("Failed to stream index segment: {}/index{}", uuid, segment, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @PutMapping("/{uuid}/progress")
    public ResponseEntity<Void> updateProgress(
            @PathVariable String uuid,
            @RequestBody VideoProgressRequest request) {
        videoService.updateProgress(uuid, request.getPosition());
        return ResponseEntity.ok().build();
    }
    
    @DeleteMapping("/{uuid}")
    public ResponseEntity<Void> deleteVideo(@PathVariable String uuid) {
        videoService.deleteVideo(uuid);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{uuid}/transcode-progress")
    public ResponseEntity<?> getTranscodeProgress(@PathVariable String uuid) {
        com.video.entity.TranscodeTask task = videoService.getTranscodeTask(uuid);
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        java.util.HashMap<String, Object> result = new java.util.HashMap<>();
        result.put("progress", task.getProgress() != null ? task.getProgress() : 0);
        result.put("status", task.getStatus());
        result.put("errorMessage", task.getErrorMessage() != null ? task.getErrorMessage() : "");
        return ResponseEntity.ok(result);
    }
    
    private boolean isVideoFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".mkv") || lower.endsWith(".mp4") || 
               lower.endsWith(".avi") || lower.endsWith(".mov") ||
               lower.endsWith(".wmv") || lower.endsWith(".flv");
    }
    
    @GetMapping("/aliyun/search")
    public ResponseEntity<?> searchAliyunFiles(@RequestParam String query) {
        try {
            List<CloudStorageService.CloudFile> files = cloudStorageService.searchFiles(query);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            log.error("Cloud search failed: {}", query, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @PostMapping("/aliyun/download")
    public ResponseEntity<?> downloadAliyunFile(
            @RequestParam String path,
            @RequestParam(required = false) String title) {
        try {
            cloudStorageService.downloadAndProcess(path);
            Map<String, String> result = new HashMap<>();
            result.put("status", "downloading");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Cloud download failed: {}", path, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @PostMapping("/aliyun/refresh-token")
    public ResponseEntity<?> refreshAliyunToken() {
        Map<String, String> result = new HashMap<>();
        result.put("status", "not needed - using AList");
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/aliyun/test")
    public ResponseEntity<?> testAliyunConnection() {
        try {
            Map<String, Object> result = cloudStorageService.testConnection();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Cloud test failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @PostMapping("/{uuid}/rescrap")
    public ResponseEntity<?> rescrapVideo(@PathVariable String uuid) {
        try {
            videoService.rescrapVideo(uuid);
            Map<String, String> result = new HashMap<>();
            result.put("status", "rescrap initiated");
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Rescrap failed: {}", uuid, e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}