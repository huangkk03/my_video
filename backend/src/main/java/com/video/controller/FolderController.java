package com.video.controller;

import com.video.entity.Folder;
import com.video.entity.Video;
import com.video.service.FolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class FolderController {
    
    @Autowired
    private FolderService folderService;
    
    @GetMapping("/folders")
    public ResponseEntity<List<FolderService.FolderTreeNode>> getFolderTree() {
        return ResponseEntity.ok(folderService.getFolderTree());
    }
    
    @GetMapping("/folders/{id}")
    public ResponseEntity<Folder> getFolder(@PathVariable Long id) {
        Folder folder = folderService.getFolderById(id);
        if (folder == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(folder);
    }
    
    @GetMapping("/folders/{id}/videos")
    public ResponseEntity<Map<String, Object>> getVideosInFolder(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Video> videoPage = folderService.getVideosInFolder(id, PageRequest.of(page, size));
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", videoPage.getContent());
        result.put("totalElements", videoPage.getTotalElements());
        result.put("totalPages", videoPage.getTotalPages());
        result.put("currentPage", page);
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/folders/ungrouped/videos")
    public ResponseEntity<Map<String, Object>> getUngroupedVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Video> videoPage = folderService.getVideosInFolder(null, PageRequest.of(page, size));
        
        Map<String, Object> result = new HashMap<>();
        result.put("content", videoPage.getContent());
        result.put("totalElements", videoPage.getTotalElements());
        result.put("totalPages", videoPage.getTotalPages());
        result.put("currentPage", page);
        
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/folders/ungrouped/count")
    public ResponseEntity<Map<String, Object>> getUngroupedCount() {
        Map<String, Object> result = new HashMap<>();
        result.put("count", folderService.getUngroupedVideoCount());
        return ResponseEntity.ok(result);
    }
    
    @PostMapping("/folders")
    public ResponseEntity<Folder> createFolder(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        Long parentId = request.get("parentId") != null ? ((Number) request.get("parentId")).longValue() : null;
        
        try {
            Folder folder = folderService.createFolder(name, parentId);
            return ResponseEntity.ok(folder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/folders/{id}")
    public ResponseEntity<Folder> updateFolder(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String name = request.get("name");
        
        try {
            Folder folder = folderService.updateFolder(id, name);
            return ResponseEntity.ok(folder);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @DeleteMapping("/folders/{id}")
    public ResponseEntity<Void> deleteFolder(@PathVariable Long id) {
        try {
            folderService.deleteFolder(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PutMapping("/videos/{uuid}/folder")
    public ResponseEntity<Video> moveVideoToFolder(
            @PathVariable String uuid,
            @RequestBody Map<String, Object> request) {
        Long folderId = request.get("folderId") != null ? ((Number) request.get("folderId")).longValue() : null;
        
        try {
            Video video = folderService.moveVideoToFolder(uuid, folderId);
            return ResponseEntity.ok(video);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @PostMapping("/videos/batch-move")
    public ResponseEntity<Map<String, Object>> batchMoveToFolder(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> uuids = (List<String>) request.get("uuids");
        Long folderId = request.get("folderId") != null ? ((Number) request.get("folderId")).longValue() : null;
        
        int count = folderService.batchMoveToFolder(uuids, folderId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("movedCount", count);
        
        return ResponseEntity.ok(result);
    }
}