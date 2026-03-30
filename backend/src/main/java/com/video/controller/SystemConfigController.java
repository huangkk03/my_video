package com.video.controller;

import com.video.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SystemConfigController {
    
    private final SystemConfigService systemConfigService;
    
    @GetMapping
    public ResponseEntity<Map<String, String>> getAllConfigs() {
        return ResponseEntity.ok(systemConfigService.getAllConfigs());
    }
    
    @GetMapping("/{key}")
    public ResponseEntity<String> getConfig(@PathVariable String key) {
        String value = systemConfigService.getConfig(key);
        return value != null ? ResponseEntity.ok(value) : ResponseEntity.notFound().build();
    }
    
    @PostMapping
    public ResponseEntity<Map<String, String>> setConfigs(@RequestBody Map<String, String> configs) {
        systemConfigService.setConfigs(configs);
        return ResponseEntity.ok(configs);
    }
    
    @PutMapping("/{key}")
    public ResponseEntity<Void> setConfig(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        String value = body.get("value");
        String description = body.get("description");
        systemConfigService.setConfig(key, value, description);
        return ResponseEntity.ok().build();
    }
}
