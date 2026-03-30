package com.video.controller;

import com.video.entity.ImportTask;
import com.video.service.CloudMediaService;
import com.video.service.CloudStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/cloud")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CloudStorageController {
    
    private final CloudStorageService cloudStorageService;
    private final CloudMediaService cloudMediaService;
    
    @GetMapping("/test")
    public ResponseEntity<?> testConnection() {
        try {
            Map<String, Object> result = cloudStorageService.testConnection();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Cloud storage test failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @GetMapping("/storage/list")
    public ResponseEntity<?> getStorageList() {
        try {
            Map<String, Object> result = cloudStorageService.getStorageList();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Get storage list failed", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @GetMapping("/search")
    public ResponseEntity<?> searchFiles(@RequestParam String keyword) {
        try {
            List<CloudStorageService.CloudFile> files = cloudStorageService.searchFiles(keyword);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            log.error("Search failed: {}", keyword, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @GetMapping("/files")
    public ResponseEntity<?> listFiles(@RequestParam(required = false) String path) {
        try {
            List<CloudStorageService.CloudFile> files = cloudStorageService.listFiles(path);
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            log.error("List files failed: {}", path, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @PostMapping("/import")
    public ResponseEntity<?> importFile(
            @RequestParam String fileName,
            @RequestParam String filePath,
            @RequestParam Long fileSize) {
        try {
            ImportTask task = cloudMediaService.startImport(fileName, filePath, fileSize);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("taskId", task.getTaskId());
            result.put("status", task.getStatus());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Import failed: {}", filePath, e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    
    @GetMapping("/tasks")
    public ResponseEntity<?> getAllTasks() {
        try {
            List<ImportTask> tasks = cloudMediaService.getAllTasks();
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            log.error("Get tasks failed", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/tasks/active")
    public ResponseEntity<?> getActiveTasks() {
        try {
            List<ImportTask> tasks = cloudMediaService.getActiveTasks();
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            log.error("Get active tasks failed", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<?> getTask(@PathVariable String taskId) {
        try {
            ImportTask task = cloudMediaService.getTask(taskId);
            if (task == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(task);
        } catch (Exception e) {
            log.error("Get task failed: {}", taskId, e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
