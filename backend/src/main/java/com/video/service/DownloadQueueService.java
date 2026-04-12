package com.video.service;

import com.video.entity.DownloadQueue;
import com.video.entity.Video;
import com.video.repository.DownloadQueueRepository;
import com.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadQueueService {

    private final DownloadQueueRepository downloadQueueRepository;
    private final VideoRepository videoRepository;
    private final TranscodeService transcodeService;
    private final CloudStorageService cloudStorageService;

    @Value("${video.download.path:/data/downloads/incoming}")
    private String downloadPath;

    @Value("${video.download.retry-max:3}")
    private int maxRetries;

    public DownloadQueue addToQueue(String sourceUrl, String sourceName, Long sourceSize, int priority, Long folderId) {
        DownloadQueue queue = new DownloadQueue();
        queue.setTaskId(UUID.randomUUID().toString());
        queue.setSourceUrl(sourceUrl);
        queue.setSourceName(sourceName);
        queue.setSourceSize(sourceSize);
        queue.setPriority(priority);
        queue.setFolderId(folderId);
        queue.setStatus("pending");
        return downloadQueueRepository.save(queue);
    }

    public List<DownloadQueue> getQueueList() {
        return downloadQueueRepository.findAll();
    }

    public List<DownloadQueue> getPendingTasks() {
        return downloadQueueRepository.findByStatusOrderByPriorityAscCreatedAtAsc("pending");
    }

    public Optional<DownloadQueue> getTask(String taskId) {
        return downloadQueueRepository.findByTaskId(taskId);
    }

    @Transactional
    public boolean cancelTask(String taskId) {
        Optional<DownloadQueue> taskOpt = downloadQueueRepository.findByTaskId(taskId);
        if (!taskOpt.isPresent()) {
            return false;
        }
        DownloadQueue task = taskOpt.get();
        if ("pending".equals(task.getStatus()) || "downloading".equals(task.getStatus())) {
            task.setStatus("cancelled");
            task.setErrorMessage("User cancelled");
            downloadQueueRepository.save(task);
            return true;
        }
        return false;
    }

    @Transactional
    public boolean retryTask(String taskId) {
        Optional<DownloadQueue> taskOpt = downloadQueueRepository.findByTaskId(taskId);
        if (!taskOpt.isPresent()) {
            return false;
        }
        DownloadQueue task = taskOpt.get();
        if ("failed".equals(task.getStatus()) || "cancelled".equals(task.getStatus())) {
            task.setStatus("pending");
            task.setRetryCount(0);
            task.setErrorMessage(null);
            task.setProgress(0);
            downloadQueueRepository.save(task);
            return true;
        }
        return false;
    }

    @Async("downloadExecutor")
    public CompletableFuture<Void> processDownloadQueue() {
        log.info("Download queue processor started");

        while (true) {
            try {
                List<DownloadQueue> tasks = downloadQueueRepository
                        .findByStatusOrderByPriorityAscCreatedAtAsc("pending", PageRequest.of(0, 1));
                Optional<DownloadQueue> taskOpt = tasks.isEmpty() ? Optional.empty() : Optional.of(tasks.get(0));

                if (!taskOpt.isPresent()) {
                    Thread.sleep(5000);
                    continue;
                }

                DownloadQueue task = taskOpt.get();
                processTask(task);

            } catch (Exception e) {
                log.error("Error in download queue processor: {}", e.getMessage(), e);
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        return CompletableFuture.completedFuture(null);
    }

    private void processTask(DownloadQueue task) {
        log.info("Processing download task: {} - {}", task.getTaskId(), task.getSourceName());

        task.setStatus("downloading");
        task.setStartedAt(LocalDateTime.now());
        downloadQueueRepository.save(task);

        try {
            String rawUrl;
            if (task.getSourceUrl().contains("/api/fs/get")) {
                rawUrl = task.getSourceUrl();
            } else {
                rawUrl = cloudStorageService.getFileRawUrl(task.getSourceUrl());
            }

            if (rawUrl == null) {
                throw new RuntimeException("Cannot get file URL from AList");
            }

            Path targetPath = downloadFile(rawUrl, task.getSourceName());
            task.setSavePath(targetPath.toString());
            task.setProgress(100);

            task.setStatus("transcoding");
            downloadQueueRepository.save(task);

            transcodeService.startTranscode(targetPath.toString(), task.getFolderId());

            task.setStatus("completed");
            task.setCompletedAt(LocalDateTime.now());
            downloadQueueRepository.save(task);

            log.info("Download task completed: {}", task.getTaskId());

        } catch (Exception e) {
            log.error("Download task failed: {}", task.getTaskId(), e);
            task.setRetryCount(task.getRetryCount() + 1);

            if (task.getRetryCount() >= maxRetries) {
                task.setStatus("failed");
                task.setErrorMessage("Max retries exceeded: " + e.getMessage());
                task.setCompletedAt(LocalDateTime.now());
            } else {
                task.setStatus("pending");
                task.setErrorMessage("Retry " + task.getRetryCount() + ": " + e.getMessage());
            }
            downloadQueueRepository.save(task);
        }
    }

    private Path downloadFile(String fileUrl, String fileName) throws Exception {
        Path targetDir = Paths.get(downloadPath);
        Files.createDirectories(targetDir);

        Path targetPath = targetDir.resolve(fileName);

        URL url = new URL(fileUrl);
        try (InputStream in = url.openStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("File downloaded: {} -> {}", fileName, targetPath);
        return targetPath;
    }

    public long getPendingCount() {
        return downloadQueueRepository.countByStatus("pending");
    }

    public long getDownloadingCount() {
        return downloadQueueRepository.countByStatus("downloading");
    }

    public long getQueueSize() {
        return downloadQueueRepository.countByStatusIn(Arrays.asList("pending", "downloading", "transcoding"));
    }
}
