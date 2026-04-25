package com.video.service;

import com.video.entity.DownloadQueue;
import com.video.entity.TranscodeTask;
import com.video.entity.Video;
import com.video.repository.DownloadQueueRepository;
import com.video.repository.TranscodeTaskRepository;
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
    private final TranscodeTaskRepository transcodeTaskRepository;
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

            try {
                transcodeService.startTranscode(targetPath.toString(), task.getFolderId());
                // Wait for transcode to complete by checking video status
                String videoUuid = findVideoUuidByPath(targetPath.toString());
                if (videoUuid != null) {
                    waitForTranscodeCompletion(videoUuid, task);
                }
            } catch (Exception e) {
                log.error("Transcode failed for task: {}", task.getTaskId(), e);
                task.setStatus("failed");
                task.setErrorMessage("Transcode failed: " + e.getMessage());
                task.setCompletedAt(LocalDateTime.now());
                downloadQueueRepository.save(task);
                return;
            }

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

    private String findVideoUuidByPath(String filePath) {
        List<Video> videos = videoRepository.findAll();
        for (Video video : videos) {
            if (filePath.equals(video.getOriginalPath())) {
                return video.getUuid();
            }
        }
        return null;
    }

    private void waitForTranscodeCompletion(String videoUuid, DownloadQueue task) throws Exception {
        int maxWaitSeconds = 28800; // 8 hours max wait for large files
        int waitedSeconds = 0;
        int checkIntervalSeconds = 10;
        int lastProgress = -1;
        int progressFrozenSeconds = 0;
        int maxProgressFrozenSeconds = 600; // 10 minutes of no progress = potentially stuck

        log.info("Starting to wait for transcode completion for video: {}", videoUuid);

        while (waitedSeconds < maxWaitSeconds) {
            Thread.sleep(checkIntervalSeconds * 1000);
            waitedSeconds += checkIntervalSeconds;

            Optional<Video> videoOpt = videoRepository.findByUuid(videoUuid);
            if (!videoOpt.isPresent()) {
                throw new RuntimeException("Video not found: " + videoUuid);
            }

            Video video = videoOpt.get();

            // Get progress from TranscodeTask
            Optional<com.video.entity.TranscodeTask> transcodeTaskOpt =
                transcodeTaskRepository.findByVideoUuid(videoUuid);
            int currentProgress = 0;
            if (transcodeTaskOpt.isPresent()) {
                com.video.entity.TranscodeTask transcodeTask = transcodeTaskOpt.get();
                currentProgress = transcodeTask.getProgress() != null ? transcodeTask.getProgress() : 0;
                task.setProgress(currentProgress);

                if ("failed".equals(transcodeTask.getStatus())) {
                    task.setErrorMessage("Transcode failed: " +
                        (transcodeTask.getErrorMessage() != null ? transcodeTask.getErrorMessage() : "unknown error"));
                    downloadQueueRepository.save(task);
                    throw new RuntimeException(task.getErrorMessage());
                }

                if ("completed".equals(transcodeTask.getStatus())) {
                    log.info("Transcode completed for video: {}", videoUuid);
                    return;
                }
            }

            if ("completed".equals(video.getStatus())) {
                log.info("Transcode completed for video: {}", videoUuid);
                return;
            } else if ("failed".equals(video.getStatus())) {
                throw new RuntimeException("Transcode failed for video: " + videoUuid);
            }

            // Track if progress is frozen
            if (currentProgress == lastProgress) {
                progressFrozenSeconds += checkIntervalSeconds;
                if (progressFrozenSeconds >= maxProgressFrozenSeconds) {
                    log.warn("Transcode progress frozen at {}% for {}s for video: {}. Continuing to wait as process may still be running.",
                        currentProgress, progressFrozenSeconds, videoUuid);
                    progressFrozenSeconds = 0; // Reset to avoid repeated warnings
                }
            } else {
                progressFrozenSeconds = 0;
                lastProgress = currentProgress;
            }

            // Check if HLS output file exists and is being written
            String hlsPath = video.getHlsPath();
            if (hlsPath != null && !hlsPath.isEmpty()) {
                java.nio.file.Path path = java.nio.file.Paths.get(hlsPath);
                if (java.nio.file.Files.exists(path)) {
                    long fileSize = java.nio.file.Files.size(path);
                    log.debug("Transcode file size: {} bytes for video: {}", fileSize, videoUuid);
                }
            }

            log.info("Waiting for transcode... {}s (progress: {}%, frozen: {}s)", waitedSeconds, currentProgress, progressFrozenSeconds);
        }

        // Final check - if video is still transcoding but we've hit max wait, check if process might still be running
        Optional<Video> finalVideoOpt = videoRepository.findByUuid(videoUuid);
        if (finalVideoOpt.isPresent()) {
            Video finalVideo = finalVideoOpt.get();
            if ("transcoding".equals(finalVideo.getStatus())) {
                log.warn("Transcode hit max wait time ({}s) but video status is still 'transcoding'. " +
                    "This may be a large file taking longer than expected. Will continue without failing.",
                    maxWaitSeconds);
                // Instead of throwing, we return and let the transcode continue in background
                // The video will be marked complete when FFmpeg finishes
                return;
            }
        }

        throw new RuntimeException("Transcode timed out after " + maxWaitSeconds + " seconds");
    }
}
