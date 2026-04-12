package com.video.controller;

import com.video.entity.TranscodeTask;
import com.video.entity.Video;
import com.video.repository.TranscodeTaskRepository;
import com.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/transcode-queue")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TranscodeQueueController {

    private final TranscodeTaskRepository transcodeTaskRepository;
    private final VideoRepository videoRepository;

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            long pendingCount = transcodeTaskRepository.countByStatus("queued");
            long processingCount = transcodeTaskRepository.countByStatus("processing");
            
            List<TranscodeTask> processingTasks = transcodeTaskRepository.findByStatusOrderByCreatedAtAsc("processing");
            TranscodeTask currentTask = processingTasks.isEmpty() ? null : processingTasks.get(0);
            
            Video currentVideo = null;
            if (currentTask != null) {
                currentVideo = videoRepository.findByUuid(currentTask.getVideoUuid()).orElse(null);
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("pending", pendingCount);
            stats.put("processing", processingCount);
            stats.put("threadCount", 1);
            stats.put("queueEnabled", true);

            Map<String, Object> currentTaskInfo = null;
            if (currentTask != null && currentVideo != null) {
                currentTaskInfo = new HashMap<>();
                currentTaskInfo.put("videoUuid", currentTask.getVideoUuid());
                currentTaskInfo.put("title", currentVideo.getTitle());
                currentTaskInfo.put("progress", currentTask.getProgress());
                currentTaskInfo.put("startedAt", currentTask.getStartedAt());
            }
            stats.put("currentTask", currentTaskInfo);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Get transcode stats failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingTasks(
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<TranscodeTask> tasks = transcodeTaskRepository
                    .findByStatusOrderByCreatedAtAsc("queued", PageRequest.of(0, size));
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            log.error("Get pending tasks failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/processing")
    public ResponseEntity<?> getProcessingTasks() {
        try {
            List<TranscodeTask> tasks = transcodeTaskRepository.findByStatusOrderByCreatedAtAsc("processing");
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            log.error("Get processing tasks failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
