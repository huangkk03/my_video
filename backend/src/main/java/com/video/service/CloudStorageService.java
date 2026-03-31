package com.video.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

@Service
@Slf4j
public class CloudStorageService {

    @Value("${alist.url:http://alist:5244}")
    private String defaultAlistUrl;

    @Value("${download.path:/data/downloads/incoming}")
    private String downloadPath;

    @Autowired
    private SystemConfigService systemConfigService;
    
    public String getAlistUsername() {
        return systemConfigService.getConfig(SystemConfigService.ALIST_USERNAME);
    }

    public String getAlistPassword() {
        return systemConfigService.getConfig(SystemConfigService.ALIST_PASSWORD);
    }

    public String getAlistUrl() {
        String url = systemConfigService.getConfig(SystemConfigService.ALIST_URL);
        if (url == null || url.isEmpty()) {
            url = defaultAlistUrl;
        }
        return url;
    }

    public String getAlistToken() {
        String password = getAlistPassword();
        
        if (password != null && password.startsWith("alist-")) {
            log.info("Using admin token directly, length: {}", password.length());
            return password;
        }
        
        String username = getAlistUsername();
        
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            log.warn("AList username or password not configured");
            return null;
        }
        
        try {
            String url = getAlistUrl();
            log.info("Attempting to login to AList at: {}", url + "/api/auth/login");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, String> body = new HashMap<>();
            body.put("username", username);
            body.put("password", password);
            
            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(url + "/api/auth/login", request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map data = response.getBody();
                Integer code = (Integer) data.get("code");
                if (code != null && code == 200) {
                    Map responseData = (Map) data.get("data");
                    if (responseData != null) {
                        String token = (String) responseData.get("token");
                        log.info("Successfully obtained AList token via login, length: {}", token != null ? token.length() : 0);
                        return token;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to login to AList: {}, ex: {}", e.getMessage(), e.getClass().getName());
        }
        
        return null;
    }
    
    @Autowired
    private TranscodeService transcodeService;

    private final RestTemplate restTemplate = new RestTemplate();

    @Data
    @AllArgsConstructor
    public static class CloudFile {
        private String name;
        private String path;
        private boolean isFolder;
        private Long size;
        private String modified;
        
        // Add getter/setter for Jackson serialization
        public boolean getIsFolder() {
            return isFolder;
        }
        
        public void setIsFolder(boolean isFolder) {
            this.isFolder = isFolder;
        }
    }

    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();
        try {
            String token = getAlistToken();
            String url = getAlistUrl();
            log.info("testConnection - token length: {}, url: {}", token != null ? token.length() : 0, url);
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            HttpEntity<?> request = new HttpEntity<>(headers);
            log.info("Making request to: {}", url + "/api/me");
            
            // Use exchange instead of getForEntity for more control
            ResponseEntity<Map> response = restTemplate.exchange(
                url + "/api/me", 
                HttpMethod.GET, 
                request, 
                Map.class
            );
            log.info("Response status: {}, body: {}", response.getStatusCode(), response.getBody());
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map data = response.getBody();
                Integer code = (Integer) data.get("code");
                if (code != null && code == 200) {
                    result.put("success", true);
                    result.put("data", data);
                } else {
                    result.put("success", false);
                    result.put("message", data.get("message") != null ? data.get("message").toString() : "认证失败");
                }
            } else {
                result.put("success", false);
                result.put("message", "连接失败");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    public Map<String, Object> getStorageList() {
        Map<String, Object> result = new HashMap<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", getAlistToken());
            HttpEntity<?> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.getForEntity(getAlistUrl() + "/api/storage/list", Map.class);
            result.put("success", response.getStatusCode() == HttpStatus.OK);
            result.put("data", response.getBody());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    public List<CloudFile> searchFiles(String keyword) {
        List<CloudFile> files = new ArrayList<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", getAlistToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = new HashMap<>();
            body.put("path", "/");
            body.put("password", "");
            body.put("keyword", keyword);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(getAlistUrl() + "/api/fs/search", request, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map data = (Map) response.getBody().get("data");
                if (data != null) {
                    List<Map> content = (List<Map>) data.get("content");
                    if (content != null) {
                        for (Map item : content) {
                            Boolean isFolder = item.get("is_dir") != null ? (Boolean) item.get("is_dir") : false;
                            String itemPath = item.get("parent") != null ? item.get("parent") + "/" + item.get("name") : "/" + item.get("name");
                            files.add(new CloudFile(
                                (String) item.get("name"),
                                itemPath,
                                isFolder,
                                item.get("size") != null ? ((Number) item.get("size")).longValue() : 0L,
                                (String) item.get("modified")
                            ));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Search files error: {}", e.getMessage());
        }
        return files;
    }

    public List<CloudFile> listFiles(String path) {
        List<CloudFile> files = new ArrayList<>();
        try {
            if (path == null || path.isEmpty()) {
                path = "/";
            }
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", getAlistToken());
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            Map<String, Object> body = new HashMap<>();
            body.put("path", path);
            body.put("password", "");
            body.put("page", 1);
            body.put("per_page", 0);
            body.put("refresh", false);
            
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(getAlistUrl() + "/api/fs/list", request, Map.class);
            
            log.info("List files response: {}", response.getBody());
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map data = (Map) response.getBody().get("data");
                if (data != null) {
                    List<Map> content = (List<Map>) data.get("content");
                    if (content != null) {
                        for (Map item : content) {
                            Boolean isFolder = item.get("is_dir") != null ? (Boolean) item.get("is_dir") : false;
                            
                            // AList v3 returns path differently, we need to construct it
                            String itemPath = path.equals("/") ? "/" + item.get("name") : path + "/" + item.get("name");
                            
                            files.add(new CloudFile(
                                (String) item.get("name"),
                                itemPath,
                                isFolder,
                                item.get("size") != null ? ((Number) item.get("size")).longValue() : 0L,
                                (String) item.get("modified")
                            ));
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("List files error: {}", e.getMessage(), e);
        }
        return files;
    }

    public String getFileRawUrl(String path) {
        String apiUrl = getAlistUrl() + "/api/fs/get";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", getAlistToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = new HashMap<>();
        body.put("path", path);
        body.put("password", ""); // AList v3 usually requires password field even if empty
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map data = (Map) response.getBody().get("data");
                return data != null ? (String) data.get("raw_url") : null;
            }
        } catch (Exception e) {
            log.error("Failed to get AList URL: {}", e.getMessage());
        }
        return null;
    }

    @Async("taskExecutor")
    public void downloadAndProcess(String remotePath) {
        try {
            log.info("Starting cloud file processing: {}", remotePath);

            String rawUrl = getFileRawUrl(remotePath);
            if (rawUrl == null) {
                log.error("Cannot get file URL: {}", remotePath);
                return;
            }

            String fileName = remotePath.substring(remotePath.lastIndexOf("/") + 1);
            Path targetPath = Paths.get(downloadPath, fileName);
            Files.createDirectories(targetPath.getParent());

            log.info("Downloading to: {}", targetPath);
            downloadStream(rawUrl, targetPath);

            log.info("Download complete, starting transcode: {}", fileName);
            transcodeService.startTranscode(targetPath.toString());

        } catch (Exception e) {
            log.error("CloudStorage processing error: {}", e.getMessage(), e);
        }
    }

    private void downloadStream(String fileUrl, Path target) throws IOException {
        URL url = new URL(fileUrl);
        try (InputStream in = url.openStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
