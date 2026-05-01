package com.video.service;

import com.video.entity.TranscodeTask;
import com.video.entity.Video;
import com.video.repository.TranscodeTaskRepository;
import com.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscodeService {

    @PersistenceContext
    private EntityManager entityManager;

    private final TransactionTemplate transactionTemplate;

    private static final String VIDEO_STORAGE_PATH = "/data/videos";
    private static final String DOWNLOAD_PATH = "/data/downloads";
    private static final String FFMPEG_COMMAND = "ffmpeg";
    private static final String FFPROBE_COMMAND = "ffprobe";
    private static final long MAX_TRANSCODE_HOURS = 8;
    private static final long DOWNLOAD_TIMEOUT_MILLIS = 4 * 60 * 60 * 1000L;

    private final VideoRepository videoRepository;
    private final TranscodeTaskRepository transcodeTaskRepository;
    private final SubtitleService subtitleService;
    private final CloudStorageService cloudStorageService;

    @Async("transcodeExecutor")
    public CompletableFuture<Void> transcodeVideo(String videoUuid) {
        log.info("Starting transcode task for video: {}", videoUuid);

        Video video = videoRepository.findByUuid(videoUuid).orElse(null);
        if (video == null) {
            log.error("Video not found: {}", videoUuid);
            return CompletableFuture.completedFuture(null);
        }

        TranscodeTask existingTask = transcodeTaskRepository.findByVideoUuid(videoUuid).orElse(null);
        if (existingTask != null) {
            if ("processing".equals(existingTask.getStatus())) {
                log.warn("Transcode already in progress for video: {}, skipping", videoUuid);
                return CompletableFuture.completedFuture(null);
            }
            if ("completed".equals(existingTask.getStatus()) && "completed".equals(video.getStatus())) {
                log.info("Transcode already completed for video: {}, skipping", videoUuid);
                return CompletableFuture.completedFuture(null);
            }
        }

        TranscodeTask task = existingTask;
        if (task == null) {
            task = createTranscodeTask(video);
        }
        
        try {
            task.setStatus("processing");
            task.setStartedAt(LocalDateTime.now());
            transcodeTaskRepository.save(task);
            
            video.setStatus("transcoding");
            videoRepository.save(video);
            
            executeTranscode(video, task);
            
            task.setStatus("completed");
            task.setProgress(100);
            task.setCompletedAt(LocalDateTime.now());
            transcodeTaskRepository.save(task);
            
            video.setStatus("completed");
            videoRepository.save(video);

            log.info("Transcode completed for video: {}", videoUuid);

            subtitleService.triggerSubtitleSearch(videoUuid);
            
        } catch (Exception e) {
            log.error("Transcode failed for video: {}", videoUuid, e);
            handleTranscodeFailure(video, task, e);
        }
        
        return CompletableFuture.completedFuture(null);
    }
    
    private TranscodeTask createTranscodeTask(Video video) {
        TranscodeTask task = new TranscodeTask();
        task.setVideoId(video.getId());
        task.setVideoUuid(video.getUuid());
        task.setProgress(0);
        task.setStatus("queued");
        task.setRetryCount(0);
        task.setMaxRetries(3);
        return transcodeTaskRepository.save(task);
    }
    
    private void executeTranscode(Video video, TranscodeTask task) throws Exception {
        Path videoDir = Paths.get(VIDEO_STORAGE_PATH, video.getUuid());
        Files.createDirectories(videoDir);
        
        Path originalPath = Paths.get(video.getOriginalPath());
        Path hlsPath = videoDir.resolve("index.m3u8");
        
        // Get video duration for progress calculation
        long durationMs = getVideoDuration(originalPath.toString());
        video.setDuration(durationMs);
        
        String[] command = {
            FFMPEG_COMMAND,
            "-fflags", "+genpts",
            "-async", "1",
            "-i", originalPath.toString(),
            "-codec:v", "libx264",
            "-pix_fmt", "yuv420p",
            "-profile:v", "high",
            "-level", "4.1",
            "-preset", "veryfast",
            "-crf", "23",
            "-c:a", "aac",
            "-ac", "2",
            "-b:a", "192k",
            "-ar", "48000",
            "-movflags", "+faststart",
            "-vsync", "cfr",
            "-f", "hls",
            "-hls_time", "6",
            "-hls_list_size", "0",
            "-hls_flags", "delete_segments+independent_segments",
            hlsPath.toString()
        };
        
        task.setFfmpegCommand(String.join(" ", command));
        transcodeTaskRepository.save(task);
        
        log.info("Executing FFmpeg command: {}", task.getFfmpegCommand());
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            long lastProgressUpdate = System.currentTimeMillis();
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg: {}", line);
                
                // Parse progress from FFmpeg output every 2 seconds
                if (System.currentTimeMillis() - lastProgressUpdate > 2000) {
                    lastProgressUpdate = System.currentTimeMillis();
                    int progress = parseProgress(line, durationMs);
                    if (progress >= 0) {
                        task.setProgress(progress);
                        transcodeTaskRepository.save(task);
                    }
                }
            }
        }

        boolean finished = process.waitFor(MAX_TRANSCODE_HOURS, TimeUnit.HOURS);
        if (!finished) {
            log.error("FFmpeg transcode timed out after {} hours for video: {}", MAX_TRANSCODE_HOURS, video.getUuid());
            process.destroyForcibly();
            throw new RuntimeException("FFmpeg transcode timed out after " + MAX_TRANSCODE_HOURS + " hours");
        }
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg exited with code: " + exitCode);
        }

        video.setHlsPath(hlsPath.toString());
        
        if (durationMs > 0) {
            video.setDuration(durationMs);
        }
        videoRepository.save(video);
        
        generateThumbnail(video);
    }
    
    private void generateThumbnail(Video video) {
        try {
            Path videoDir = Paths.get(VIDEO_STORAGE_PATH, video.getUuid());
            Path thumbnailPath = videoDir.resolve("thumbnail.jpg");
            
            String[] command = {
                FFMPEG_COMMAND,
                "-i", video.getOriginalPath(),
                "-ss", "00:00:05",
                "-vframes", "1",
                "-vf", "scale=320:-1",
                thumbnailPath.toString()
            };
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor();
            
            if (Files.exists(thumbnailPath)) {
                video.setThumbnailPath(thumbnailPath.toString());
                videoRepository.save(video);
            }
        } catch (Exception e) {
            log.warn("Failed to generate thumbnail: {}", e.getMessage());
        }
    }

    private void executeTranscodeFromUrl(String sourceUrl, Video video, TranscodeTask task) throws Exception {
        Path videoDir = Paths.get(VIDEO_STORAGE_PATH, video.getUuid());
        Files.createDirectories(videoDir);

        Path hlsPath = videoDir.resolve("index.m3u8");
        Path thumbnailPath = videoDir.resolve("thumbnail.jpg");
        Path mkvPath = Paths.get(DOWNLOAD_PATH, video.getUuid() + ".mkv");

        task.setDownloadStatus("downloading");
        task.setDownloadProgress(0);
        task.setDownloadPath(mkvPath.toString());
        transcodeTaskRepository.save(task);

        log.info("Starting download of video: {} to {}", video.getUuid(), mkvPath);

        long existingBytes = Files.exists(mkvPath) ? Files.size(mkvPath) : 0;
        long totalBytes = existingBytes;

        if (existingBytes > 0) {
            log.info("Resuming download from {} bytes", existingBytes);
        }

        try {
            long downloadedBytes = cloudStorageService.downloadFileWithProgress(
                sourceUrl,
                mkvPath,
                existingBytes,
                (Long receivedBytes) -> {
                    if (totalBytes > 0) {
                        int progress = (int) (((existingBytes + receivedBytes) * 50) / totalBytes);
                        progress = Math.min(49, Math.max(1, progress));
                        task.setDownloadProgress(progress);
                        task.setDownloadBytes(existingBytes + receivedBytes);
                        transcodeTaskRepository.save(task);
                    }
                },
                DOWNLOAD_TIMEOUT_MILLIS
            );

            task.setDownloadStatus("completed");
            task.setDownloadProgress(50);
            task.setDownloadBytes(downloadedBytes);
            task.setTotalBytes(downloadedBytes);
            transcodeTaskRepository.save(task);

            log.info("Download complete: {} bytes", downloadedBytes);

        } catch (Exception e) {
            task.setDownloadStatus("failed");
            task.setErrorMessage("Download failed: " + e.getMessage());
            transcodeTaskRepository.save(task);
            throw new RuntimeException("Failed to download video: " + e.getMessage(), e);
        }

        long durationMs = getVideoDuration(mkvPath.toString());
        log.info("Video duration: {} ms ({} seconds)", durationMs, durationMs / 1000);
        video.setDuration(durationMs);
        videoRepository.save(video);

        Path progressFile = videoDir.resolve("progress.txt");
        List<String> hlsCommand = new ArrayList<>(Arrays.asList(
            FFMPEG_COMMAND,
            "-i", mkvPath.toString(),
            "-codec:v", "libx264",
            "-pix_fmt", "yuv420p",
            "-profile:v", "high",
            "-level", "4.1",
            "-preset", "veryfast",
            "-crf", "23",
            "-c:a", "aac",
            "-ac", "2",
            "-b:a", "192k",
            "-ar", "48000",
            "-movflags", "+faststart",
            "-vsync", "cfr",
            "-f", "hls",
            "-hls_time", "6",
            "-hls_list_size", "0",
            "-hls_flags", "delete_segments+independent_segments",
            "-progress", "file://" + progressFile.toString(),
            hlsPath.toString()
        ));

        task.setFfmpegCommand(String.join(" ", hlsCommand));
        transcodeTaskRepository.save(task);

        log.info("Executing FFmpeg transcode on local file: {}", mkvPath);

        ProcessBuilder pb = new ProcessBuilder(hlsCommand);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        long lastProgressUpdate = System.currentTimeMillis();
        long lastOutTimeMs = 0;
        int consecutiveZeroProgress = 0;
        while (true) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

            if (System.currentTimeMillis() - lastProgressUpdate > 2000) {
                lastProgressUpdate = System.currentTimeMillis();

                if (Files.exists(progressFile)) {
                    try {
                        String content = new String(Files.readAllBytes(progressFile));
                        int progress = parseProgressFromFile(content, durationMs);
                        log.debug("Parsed progress: {} (durationMs={})", progress, durationMs);

                        if (progress >= 0 && progress != lastOutTimeMs) {
                            int overallProgress = 50 + (progress / 2);
                            task.setProgress(overallProgress);
                            transcodeTaskRepository.save(task);
                            lastOutTimeMs = progress;
                            consecutiveZeroProgress = 0;
                        } else if (progress == 0) {
                            consecutiveZeroProgress++;
                            if (consecutiveZeroProgress >= 3 && durationMs == 0) {
                                log.warn("Progress stuck at 0% and duration unknown. Setting progress to indicate active transcode.");
                                task.setProgress(51);
                                transcodeTaskRepository.save(task);
                                consecutiveZeroProgress = 0;
                            }
                        } else {
                            // Progress is frozen (not 0 but not changing either)
                            consecutiveZeroProgress++;
                            if (consecutiveZeroProgress >= 10) { // 10 * 2 seconds = 20 seconds of no progress
                                log.error("FFmpeg progress frozen at {}% for {} seconds. Killing process.", progress, consecutiveZeroProgress * 2);
                                process.destroyForcibly();
                                throw new RuntimeException("FFmpeg transcode stuck at " + progress + "% for too long");
                            }
                        }
                    } catch (Exception e) {
                        log.debug("Failed to read progress file: {}", e.getMessage());
                    }
                } else {
                    log.warn("Progress file does not exist yet: {}", progressFile);
                }
            }

            if (!process.isAlive()) {
                break;
            }
        }

        if (process.isAlive()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("FFmpeg: {}", line);
                }
            }
        } else {
            log.warn("FFmpeg process was killed, skipping output reading");
        }

        boolean finished = process.waitFor(MAX_TRANSCODE_HOURS, TimeUnit.HOURS);
        if (!finished) {
            log.error("FFmpeg transcode timed out after {} hours for video: {}", MAX_TRANSCODE_HOURS, video.getUuid());
            process.destroyForcibly();
            throw new RuntimeException("FFmpeg transcode timed out after " + MAX_TRANSCODE_HOURS + " hours");
        }
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg exited with code: " + exitCode);
        }

        if (Files.exists(mkvPath)) {
            try {
                Files.delete(mkvPath);
                log.info("Deleted downloaded mkv file: {}", mkvPath);
            } catch (Exception e) {
                log.warn("Failed to delete mkv file: {}", e.getMessage());
            }
        }

        String[] thumbCmd = {
            FFMPEG_COMMAND,
            "-i", hlsPath.toString(),
            "-ss", "00:00:05",
            "-vframes", "1",
            "-vf", "scale=320:-1",
            thumbnailPath.toString()
        };
        ProcessBuilder thumbPb = new ProcessBuilder(thumbCmd);
        thumbPb.redirectErrorStream(true);
        Process thumbProcess = thumbPb.start();
        thumbProcess.waitFor();

        video.setHlsPath(hlsPath.toString());
        if (Files.exists(thumbnailPath)) {
            video.setThumbnailPath(thumbnailPath.toString());
        }
        videoRepository.save(video);
    }

    private long getVideoDuration(String videoPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(FFPROBE_COMMAND, "-i", videoPath, "-v", "quiet", "-print_format", "json", "-show_format");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            int exitCode = p.waitFor();
            String output = sb.toString();

            if (exitCode != 0) {
                log.warn("ffprobe exited with code {}, output: {}", exitCode, output);
            }

            log.debug("ffprobe output: {}", output);

            Pattern pattern = Pattern.compile("\"duration\"\\s*:\\s*\"?([\\d.]+)\"?");
            Matcher matcher = pattern.matcher(output);
            if (matcher.find()) {
                double durationSec = Double.parseDouble(matcher.group(1));
                log.info("Parsed duration from JSON: {} seconds", durationSec);
                return (long) (durationSec * 1000);
            }

            Pattern durationPattern = Pattern.compile("Duration:\\s*(\\d+):(\\d+):(\\d+\\.\\d+)");
            Matcher durationMatcher = durationPattern.matcher(output);
            if (durationMatcher.find()) {
                int hours = Integer.parseInt(durationMatcher.group(1));
                int minutes = Integer.parseInt(durationMatcher.group(2));
                double seconds = Double.parseDouble(durationMatcher.group(3));
                long durationMs = (long) ((hours * 3600 + minutes * 60 + seconds) * 1000);
                log.info("Parsed duration from text: {} hours {} minutes {} seconds = {} ms",
                    hours, minutes, seconds, durationMs);
                return durationMs;
            }

            log.warn("Could not parse duration from ffprobe output");
        } catch (Exception e) {
            log.warn("Failed to get video duration: {}", e.getMessage());
        }
        return 0;
    }

    private void cleanupPartialFiles(String uuid) {
        try {
            Path videoDir = Paths.get(VIDEO_STORAGE_PATH, uuid);
            if (Files.exists(videoDir)) {
                try (Stream<Path> walk = Files.walk(videoDir)) {
                    walk.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                }
                log.info("Cleaned up partial files for video: {}", uuid);
            }
        } catch (Exception e) {
            log.warn("Failed to cleanup directory for video: {}", uuid, e);
        }
    }

    private int parseProgress(String line, long totalDurationMs) {
        // Parse time from FFmpeg output like "time=00:05:30.50"
        int timeIndex = line.indexOf("time=");
        if (timeIndex < 0) return -1;

        String timeStr = line.substring(timeIndex + 5, line.indexOf(" ", timeIndex));
        if (!timeStr.contains(":")) return -1;

        String[] parts = timeStr.split(":");
        if (parts.length != 3) return -1;

        try {
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            double seconds = Double.parseDouble(parts[2]);
            long currentMs = (long) ((hours * 3600 + minutes * 60 + seconds) * 1000);

            // Use default duration of 2 hours if total duration is unknown
            long effectiveDuration = totalDurationMs > 0 ? totalDurationMs : 7200000L;

            int progress = (int) ((currentMs * 100) / effectiveDuration);
            return Math.min(100, Math.max(0, progress));
        } catch (Exception e) {
            return -1;
        }
    }

    private int parseProgressFromFile(String content, long totalDurationMs) {
        try {
            String[] lines = content.split("\n");
            long outTimeMs = 0;
            long totalMs = totalDurationMs; // Use the passed duration as fallback

            for (String line : lines) {
                String trimmedLine = line.trim();
                if (trimmedLine.startsWith("out_time_ms=")) {
                    String value = trimmedLine.substring("out_time_ms=".length()).trim();
                    outTimeMs = Long.parseLong(value) / 1000;
                } else if (trimmedLine.startsWith("out_time=")) {
                    String value = trimmedLine.substring("out_time=".length()).trim();
                    if (value.contains(":")) {
                        String[] parts = value.split(":");
                        if (parts.length == 3) {
                            double hours = Double.parseDouble(parts[0]);
                            double minutes = Double.parseDouble(parts[1]);
                            double seconds = Double.parseDouble(parts[2]);
                            outTimeMs = (long) ((hours * 3600 + minutes * 60 + seconds) * 1000);
                        }
                    }
                } else if (trimmedLine.startsWith("total_duration=")) {
                    String value = trimmedLine.substring("total_duration=".length()).trim();
                    totalMs = (long) (Double.parseDouble(value) * 1000);
                }
            }

            log.debug("Progress parse: outTimeMs={}, totalMs={}", outTimeMs, totalMs);

            if (outTimeMs > 0) {
                if (totalMs > 0) {
                    int progress = (int) ((outTimeMs * 100) / totalMs);
                    return Math.min(100, Math.max(0, progress));
                } else {
                    log.warn("Total duration is 0, cannot calculate progress percentage");
                    return 0;
                }
            }
        } catch (Exception e) {
            log.debug("Failed to parse progress file: {}", e.getMessage());
        }
        return -1;
    }
    
    private void handleTranscodeFailure(Video video, TranscodeTask task, Exception e) {
        log.error("Stream transcode failed, cleaning up partial files...");
        cleanupPartialFiles(video.getUuid());

        task.setStatus("failed");
        task.setErrorMessage(e.getMessage());
        transcodeTaskRepository.save(task);

        video.setStatus("failed");
        videoRepository.save(video);
    }
    
    public String getVideoStoragePath(String uuid) {
        return Paths.get(VIDEO_STORAGE_PATH, uuid).toString();
    }
    
    @Async("transcodeExecutor")
    public void startTranscode(String filePath) {
        startTranscode(filePath, null);
    }

    @Async("transcodeExecutor")
    public void startTranscode(String filePath, Long folderId) {
        try {
            Path path = Paths.get(filePath);
            String fileName = path.getFileName().toString();
            String uuid = UUID.randomUUID().toString();
            
            Video video = new Video();
            video.setUuid(uuid);
            video.setTitle(fileName);
            video.setOriginalPath(filePath);
            video.setStatus("transcoding");
            video.setCreatedAt(LocalDateTime.now());
            if (folderId != null) {
                video.setFolderId(folderId);
            }
            video = videoRepository.save(video);
            
            transcodeVideo(video.getUuid()).get();
            
        } catch (Exception e) {
            log.error("Failed to start transcode for: {}", filePath, e);
        }
    }

    @Async("transcodeExecutor")
    public CompletableFuture<Void> transcodeVideoFromUrl(String videoUuid, String sourceUrl) {
        log.info("Starting stream transcode for video: {} from URL", videoUuid);

        Video video = videoRepository.findByUuid(videoUuid).orElse(null);
        if (video == null) {
            log.error("Video not found: {}", videoUuid);
            return CompletableFuture.completedFuture(null);
        }

        TranscodeTask existingTask = transcodeTaskRepository.findByVideoUuid(videoUuid).orElse(null);
        if (existingTask != null) {
            if ("processing".equals(existingTask.getStatus())) {
                log.warn("Transcode already in progress for video: {}, skipping", videoUuid);
                return CompletableFuture.completedFuture(null);
            }
            if ("completed".equals(existingTask.getStatus()) && "completed".equals(video.getStatus())) {
                log.info("Transcode already completed for video: {}, skipping", videoUuid);
                return CompletableFuture.completedFuture(null);
            }
        }

        TranscodeTask task = existingTask;
        if (task == null) {
            task = createTranscodeTask(video);
        }

        try {
            task.setStatus("processing");
            task.setStartedAt(LocalDateTime.now());
            transcodeTaskRepository.save(task);

            video.setStatus("transcoding");
            videoRepository.save(video);

            executeTranscodeFromUrl(sourceUrl, video, task);

            task.setStatus("completed");
            task.setProgress(100);
            task.setCompletedAt(LocalDateTime.now());
            transcodeTaskRepository.save(task);

            video.setStatus("completed");
            videoRepository.save(video);
            entityManager.flush();
            entityManager.refresh(video);

            updateVideoMetadataDirectly(videoUuid, "completed", video.getHlsPath(), video.getThumbnailPath());

            log.info("Stream transcode completed for video: {}", videoUuid);

        } catch (Exception e) {
            log.error("Stream transcode failed for video: {}", videoUuid, e);
            handleTranscodeFailure(video, task, e);
        }

        return CompletableFuture.completedFuture(null);
    }

    public void retranscode(String videoUuid) {
        Video video = videoRepository.findByUuid(videoUuid).orElse(null);
        if (video == null) {
            throw new RuntimeException("Video not found: " + videoUuid);
        }

        Path videoDir = Paths.get(VIDEO_STORAGE_PATH, videoUuid);
        try {
            if (Files.exists(videoDir)) {
                Files.walk(videoDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception e) {
                            log.warn("Failed to delete: {}", path);
                        }
                    });
                log.info("Deleted old HLS files for video: {}", videoUuid);
            }
        } catch (Exception e) {
            log.error("Failed to clean up old files for video: {}", videoUuid, e);
        }

        TranscodeTask task = transcodeTaskRepository.findByVideoUuid(videoUuid).orElse(null);
        if (task != null) {
            task.setStatus("queued");
            task.setProgress(0);
            task.setErrorMessage(null);
            task.setStartedAt(null);
            task.setCompletedAt(null);
            transcodeTaskRepository.save(task);
        }

        video.setStatus("pending");
        video.setHlsPath(null);
        videoRepository.save(video);

        transcodeVideo(videoUuid);
        log.info("Triggered retranscode for video: {}", videoUuid);
    }

    private void updateVideoMetadataDirectly(String videoUuid, String status, String hlsPath, String thumbnailPath) {
        try {
            transactionTemplate.executeWithoutResult(status1 -> {
                String sql = "UPDATE video_metadata SET status = ?, hls_path = ?, thumbnail_path = ?, updated_at = NOW() WHERE uuid = ?";
                entityManager.createNativeQuery(sql)
                    .setParameter(1, status)
                    .setParameter(2, hlsPath)
                    .setParameter(3, thumbnailPath)
                    .setParameter(4, videoUuid)
                    .executeUpdate();
            });
            log.info("Directly updated video_metadata for uuid: {}, status: {}, hls_path: {}", videoUuid, status, hlsPath);
        } catch (Exception e) {
            log.error("Failed to directly update video_metadata for uuid: {}", videoUuid, e);
        }
    }
}