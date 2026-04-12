package com.video.service;

import com.video.entity.TranscodeTask;
import com.video.entity.Video;
import com.video.repository.TranscodeTaskRepository;
import com.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscodeService {
    
    private static final String VIDEO_STORAGE_PATH = "/data/videos";
    private static final String FFMPEG_COMMAND = "ffmpeg";
    private static final String FFPROBE_COMMAND = "ffprobe";
    
    private final VideoRepository videoRepository;
    private final TranscodeTaskRepository transcodeTaskRepository;
    
    @Async("transcodeExecutor")
    public CompletableFuture<Void> transcodeVideo(String videoUuid) {
        log.info("Starting transcode task for video: {}", videoUuid);
        
        Video video = videoRepository.findByUuid(videoUuid).orElse(null);
        if (video == null) {
            log.error("Video not found: {}", videoUuid);
            return CompletableFuture.completedFuture(null);
        }
        
        TranscodeTask task = transcodeTaskRepository.findByVideoUuid(videoUuid).orElse(null);
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
            "-i", originalPath.toString(),
            "-codec:v", "libx264",
            "-preset", "fast",
            "-crf", "23",
            "-codec:a", "aac",
            "-b:a", "128k",
            "-f", "hls",
            "-hls_time", "10",
            "-hls_list_size", "0",
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
                    if (progress > 0 && progress < 100) {
                        task.setProgress(progress);
                        transcodeTaskRepository.save(task);
                    }
                }
            }
        }
        
        int exitCode = process.waitFor();
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

        long durationMs = getVideoDurationFromUrl(sourceUrl);
        video.setDuration(durationMs);

        List<String> hlsCommand = new ArrayList<>(Arrays.asList(
            FFMPEG_COMMAND,
            "-reconnect", "1",
            "-reconnect_streamed", "1",
            "-reconnect_delay_max", "5",
            "-i", sourceUrl,
            "-codec:v", "libx264",
            "-preset", "fast",
            "-crf", "23",
            "-codec:a", "aac",
            "-b:a", "128k",
            "-f", "hls",
            "-hls_time", "10",
            "-hls_list_size", "0",
            "-http_seekable", "1",
            hlsPath.toString()
        ));

        task.setFfmpegCommand(String.join(" ", hlsCommand));
        transcodeTaskRepository.save(task);

        log.info("Executing FFmpeg stream transcode from URL: {}",
                 sourceUrl.replaceAll("sign=[^&]+", "sign=HIDDEN"));

        ProcessBuilder pb = new ProcessBuilder(hlsCommand);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            long lastProgressUpdate = System.currentTimeMillis();
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg: {}", line);

                if (System.currentTimeMillis() - lastProgressUpdate > 2000) {
                    lastProgressUpdate = System.currentTimeMillis();
                    int progress = parseProgress(line, durationMs);
                    if (progress > 0 && progress < 100) {
                        task.setProgress(progress);
                        transcodeTaskRepository.save(task);
                    }
                }
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg exited with code: " + exitCode);
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

    private long getVideoDurationFromUrl(String urlStr) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                FFPROBE_COMMAND, "-i", urlStr, "-v", "quiet", "-print_format", "json",
                "-show_format", "-show_streams"
            );
            pb.redirectErrorStream(true);
            Process p = pb.start();

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            p.waitFor();

            String output = sb.toString();
            Pattern pattern = Pattern.compile("\"duration\"\\s*:\\s*([\\d.]+)");
            Matcher matcher = pattern.matcher(output);
            if (matcher.find()) {
                return (long) (Double.parseDouble(matcher.group(1)) * 1000);
            }
        } catch (Exception e) {
            log.warn("Failed to get video duration from URL: {}", e.getMessage());
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

    private long getVideoDuration(String videoPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(FFMPEG_COMMAND, "-i", videoPath, "-v", "quiet", "-print_format", "json", "-show_format", "-show_streams");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
            }
            p.waitFor();
            
            // Parse duration from FFmpeg output
            String output = sb.toString();
            int timeIndex = output.indexOf("Duration:");
            if (timeIndex > 0) {
                String timeStr = output.substring(timeIndex + 9, output.indexOf(",", timeIndex));
                String[] parts = timeStr.split(":");
                if (parts.length == 3) {
                    int hours = Integer.parseInt(parts[0].trim());
                    int minutes = Integer.parseInt(parts[1].trim());
                    double seconds = Double.parseDouble(parts[2].trim());
                    return (long) ((hours * 3600 + minutes * 60 + seconds) * 1000);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get video duration: {}", e.getMessage());
        }
        return 0;
    }
    
    private int parseProgress(String line, long totalDurationMs) {
        if (totalDurationMs <= 0) return 0;
        
        // Parse time from FFmpeg output like "time=00:05:30.50"
        int timeIndex = line.indexOf("time=");
        if (timeIndex > 0) {
            String timeStr = line.substring(timeIndex + 5, line.indexOf(" ", timeIndex));
            if (timeStr.contains(":")) {
                String[] parts = timeStr.split(":");
                try {
                    if (parts.length == 3) {
                        int hours = Integer.parseInt(parts[0]);
                        int minutes = Integer.parseInt(parts[1]);
                        double seconds = Double.parseDouble(parts[2]);
                        long currentMs = (long) ((hours * 3600 + minutes * 60 + seconds) * 1000);
                        return (int) Math.min(99, (currentMs * 100) / totalDurationMs);
                    }
                } catch (Exception e) {
                    // Ignore parse errors
                }
            }
        }
        return 0;
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

        TranscodeTask task = transcodeTaskRepository.findByVideoUuid(videoUuid).orElse(null);
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

            log.info("Stream transcode completed for video: {}", videoUuid);

        } catch (Exception e) {
            log.error("Stream transcode failed for video: {}", videoUuid, e);
            handleTranscodeFailure(video, task, e);
        }

        return CompletableFuture.completedFuture(null);
    }
}