package com.video.controller;

import com.video.entity.DownloadQueue;
import com.video.service.DownloadQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/download-queue")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DownloadQueueController {

    private final DownloadQueueService downloadQueueService;

    @GetMapping
    public ResponseEntity<?> getQueueList() {
        try {
            List<DownloadQueue> tasks = downloadQueueService.getQueueList();
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            log.error("Get queue list failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingTasks() {
        try {
            List<DownloadQueue> tasks = downloadQueueService.getPendingTasks();
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            log.error("Get pending tasks failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/{taskId}")
    public ResponseEntity<?> getTask(@PathVariable String taskId) {
        try {
            return downloadQueueService.getTask(taskId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (Exception e) {
            log.error("Get task failed: {}", taskId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/add")
    public ResponseEntity<?> addToQueue(
            @RequestParam String sourceUrl,
            @RequestParam String sourceName,
            @RequestParam(required = false, defaultValue = "0") Long sourceSize,
            @RequestParam(required = false, defaultValue = "0") int priority) {
        try {
            DownloadQueue task = downloadQueueService.addToQueue(sourceUrl, sourceName, sourceSize, priority);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("taskId", task.getTaskId());
            result.put("status", task.getStatus());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Add to queue failed: {}", sourceUrl, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @DeleteMapping("/{taskId}")
    public ResponseEntity<?> cancelTask(@PathVariable String taskId) {
        try {
            boolean success = downloadQueueService.cancelTask(taskId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            if (!success) {
                result.put("message", "Task not found or already completed/failed");
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Cancel task failed: {}", taskId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/{taskId}/retry")
    public ResponseEntity<?> retryTask(@PathVariable String taskId) {
        try {
            boolean success = downloadQueueService.retryTask(taskId);
            Map<String, Object> result = new HashMap<>();
            result.put("success", success);
            if (!success) {
                result.put("message", "Task not found or not in failed/cancelled status");
            }
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Retry task failed: {}", taskId, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("pending", downloadQueueService.getPendingCount());
            stats.put("downloading", downloadQueueService.getDownloadingCount());
            stats.put("total", downloadQueueService.getQueueSize());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Get stats failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
}
