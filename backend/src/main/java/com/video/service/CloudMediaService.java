package com.video.service;

import com.video.entity.ImportTask;
import com.video.repository.ImportTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudMediaService {
    
    private final CloudStorageService cloudStorageService;
    private final VideoService videoService;
    private final ScrapingAggregationService scrapingAggregationService;
    private final ImportTaskRepository importTaskRepository;
    
    @Value("${video.storage-path:/data/videos}")
    private String videoStoragePath;
    
    public ImportTask startImport(String fileName, String filePath, Long fileSize) {
        String taskId = UUID.randomUUID().toString();
        
        ImportTask task = new ImportTask();
        task.setTaskId(taskId);
        task.setSourceName(fileName);
        task.setSourcePath(filePath);
        task.setSourceSize(fileSize);
        task.setStatus("pending");
        task.setProgress(0);
        task.setMessage("Preparing import...");
        
        task = importTaskRepository.save(task);
        processImportAsync(taskId);
        
        return task;
    }
    
    @Async
    public void processImportAsync(String taskId) {
        ImportTask task = importTaskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) {
            log.error("Task not found: {}", taskId);
            return;
        }
        
        try {
            task.setStatus("downloading");
            task.setMessage("Downloading...");
            task.setProgress(10);
            importTaskRepository.save(task);
            
            Path downloadDir = Paths.get(videoStoragePath, "downloads", "incoming");
            Files.createDirectories(downloadDir);
            
            String fileName = task.getSourceName();
            Path targetPath = downloadDir.resolve(fileName);
            
            String downloadUrl = cloudStorageService.getFileRawUrl(task.getSourcePath());
            if (downloadUrl == null) {
                throw new RuntimeException("Failed to get download URL from AList");
            }
            downloadFile(downloadUrl, targetPath, task);
            
            task.setProgress(40);
            task.setMessage("Download complete, scraping metadata...");
            importTaskRepository.save(task);
            
            task.setStatus("scraping");
            task.setMessage("Scraping metadata...");
            importTaskRepository.save(task);
            
            String title = fileName.substring(0, fileName.lastIndexOf('.'));
            title = title.replaceAll("[._]", " ");
            
            scrapMetadata(task, title, targetPath);
            
            task.setProgress(60);
            task.setMessage("Scraping complete, starting transcoding...");
            importTaskRepository.save(task);
            
            task.setStatus("transcoding");
            task.setMessage("Transcoding...");
            importTaskRepository.save(task);
            
            String videoUuid = startTranscoding(task, targetPath, title);
            
            task.setVideoUuid(videoUuid);
            task.setProgress(100);
            task.setStatus("completed");
            task.setMessage("Import complete");
            importTaskRepository.save(task);
            
        } catch (Exception e) {
            log.error("Import failed for task: {}", taskId, e);
            task.setStatus("failed");
            task.setMessage("Failed: " + e.getMessage());
            importTaskRepository.save(task);
        }
    }
    
    private void downloadFile(String downloadUrl, Path targetPath, ImportTask task) throws Exception {
        URL url = new URL(downloadUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        long totalBytes = conn.getContentLengthLong();
        long downloadedBytes = 0;
        
        try (InputStream is = conn.getInputStream();
             FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            int lastProgress = 40;
            
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                downloadedBytes += bytesRead;
                
                if (totalBytes > 0) {
                    int prog = (int) ((downloadedBytes * 40 / totalBytes) + 10);
                    if (prog > lastProgress) {
                        task.setProgress(prog);
                        importTaskRepository.save(task);
                        lastProgress = prog;
                    }
                }
            }
        }
        
        log.info("Downloaded file to: {}", targetPath);
    }
    
    private void scrapMetadata(ImportTask task, String title, Path videoPath) throws Exception {
        CompletableFuture<ScrapingAggregationService.MetadataResult> future = 
            scrapingAggregationService.searchMetadata(title);
        
        ScrapingAggregationService.MetadataResult result = future.get();
        
        if (result != null) {
            String metadataInfo = "";
            if (result.getTitle() != null) {
                metadataInfo += "Title: " + result.getTitle();
            }
            if (result.getRating() != null) {
                metadataInfo += " | Rating: " + result.getRating();
            }
            task.setMessage("Scraping complete: " + metadataInfo);
            importTaskRepository.save(task);
        }
    }
    
    private String startTranscoding(ImportTask task, Path videoPath, String title) throws Exception {
        File videoFile = videoPath.toFile();
        VideoService.UploadResult uploadResult = videoService.processVideoFile(videoFile, title);
        return uploadResult.getUuid();
    }
    
    public List<ImportTask> getAllTasks() {
        return importTaskRepository.findAllByOrderByCreatedAtDesc();
    }
    
    public List<ImportTask> getActiveTasks() {
        return importTaskRepository.findByStatusNotOrderByCreatedAtDesc("completed");
    }
    
    public ImportTask getTask(String taskId) {
        return importTaskRepository.findByTaskId(taskId).orElse(null);
    }
}