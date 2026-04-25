package com.video.controller;

import com.video.dto.ShareSearchResult;
import com.video.service.ShareSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/share")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ShareSearchController {

    private final ShareSearchService shareSearchService;

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<ShareSearchResult> results = shareSearchService.searchAliyunShare(keyword, limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("results", results);
            response.put("total", results.size());
            response.put("message", null);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Search failed for keyword: {}", keyword, e);

            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("results", Collections.emptyList());
            error.put("total", 0);
            error.put("message", "搜索失败: " + e.getMessage());

            return ResponseEntity.status(500).body(error);
        }
    }
}
