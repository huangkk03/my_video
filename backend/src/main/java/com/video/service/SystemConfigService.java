package com.video.service;

import com.video.entity.SystemConfig;
import com.video.repository.SystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemConfigService {
    
    private final SystemConfigRepository systemConfigRepository;
    
    public static final String TMDB_API_KEY = "tmdb_api_key";
    public static final String TMDB_LANGUAGE = "tmdb_language";
    public static final String ALIST_URL = "alist_url";
    public static final String ALIST_USERNAME = "alist_username";
    public static final String ALIST_PASSWORD = "alist_password";
    public static final String ALIST_TOKEN = "alist_token";
    
    public String getConfig(String key) {
        return systemConfigRepository.findByConfigKey(key)
            .map(SystemConfig::getConfigValue)
            .orElse(null);
    }
    
    public String getConfig(String key, String defaultValue) {
        return systemConfigRepository.findByConfigKey(key)
            .map(SystemConfig::getConfigValue)
            .orElse(defaultValue);
    }
    
    public Map<String, String> getAllConfigs() {
        List<SystemConfig> configs = systemConfigRepository.findAll();
        Map<String, String> result = new HashMap<>();
        for (SystemConfig config : configs) {
            result.put(config.getConfigKey(), config.getConfigValue());
        }
        return result;
    }
    
    @Transactional
    public SystemConfig setConfig(String key, String value, String description) {
        Optional<SystemConfig> existing = systemConfigRepository.findByConfigKey(key);
        SystemConfig config;
        if (existing.isPresent()) {
            config = existing.get();
            config.setConfigValue(value);
        } else {
            config = new SystemConfig();
            config.setConfigKey(key);
            config.setConfigValue(value);
        }
        if (description != null) {
            config.setDescription(description);
        }
        config.setUpdatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
        return systemConfigRepository.save(config);
    }
    
    @Transactional
    public Map<String, String> setConfigs(Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            setConfig(entry.getKey(), entry.getValue(), null);
        }
        return configs;
    }
    
    public void initDefaultConfigs() {
        if (!systemConfigRepository.findByConfigKey(TMDB_LANGUAGE).isPresent()) {
            setConfig(TMDB_LANGUAGE, "zh-CN", "TMDB API Language");
        }
    }
}
