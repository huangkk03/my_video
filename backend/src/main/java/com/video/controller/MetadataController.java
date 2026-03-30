package com.video.controller;

import com.video.service.ScrapingAggregationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/metadata")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class MetadataController {
    
    private final ScrapingAggregationService scrapingAggregationService;
    
    @GetMapping("/search")
    public ResponseEntity<?> searchMetadata(@RequestParam String query) {
        try {
            ScrapingAggregationService.MetadataResult result = 
                scrapingAggregationService.searchMetadata(query).get();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Metadata search failed: {}", query, e);
            return ResponseEntity.status(500).body(new Error(e.getMessage()));
        }
    }
    
    public static class Error {
        private final String error;
        public Error(String error) { this.error = error; }
        public String getError() { return error; }
    }
}
