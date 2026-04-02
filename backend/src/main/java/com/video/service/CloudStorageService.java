package com.video.service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
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
        return normalizeAlistUrl(url);
    }

    private String normalizeAlistUrl(String url) {
        if (url == null) {
            return null;
        }
        String normalized = url.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public String getAlistToken() {
        String configuredToken = systemConfigService.getConfig(SystemConfigService.ALIST_TOKEN);
        if (configuredToken != null && !configuredToken.trim().isEmpty()) {
            String token = configuredToken.trim();
            if (isTokenValid(token)) {
                log.info("Using configured AList token, length: {}", token.length());
                return token;
            }
            log.warn("Configured AList token is invalid, falling back to username/password login");
        }

        return loginByUsernamePassword();
    }

    private boolean isTokenValid(String token) {
        try {
            String url = getAlistUrl() + "/api/me";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            HttpEntity<?> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, request, Map.class);
            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                return false;
            }
            Object codeObj = response.getBody().get("code");
            if (codeObj instanceof Number) {
                return ((Number) codeObj).intValue() == 200;
            }
            return false;
        } catch (Exception e) {
            log.warn("Failed to validate AList token: {}", e.getMessage());
            return false;
        }
    }

    private String loginByUsernamePassword() {
        String password = getAlistPassword();
        String username = getAlistUsername();

        if (username == null || username.trim().isEmpty() || password == null || password.trim().isEmpty()) {
            log.warn("AList username or password not configured");
            return null;
        }

        try {
            String url = getAlistUrl();
            String loginUrl = url + "/api/auth/login";
            log.info("Attempting to login to AList at: {}", loginUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, String> body = new HashMap<>();
            body.put("username", username.trim());
            body.put("password", password.trim());

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(loginUrl, request, Map.class);

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
            log.warn("AList login returned non-success payload, status: {}, body: {}", response.getStatusCode(), response.getBody());
        } catch (HttpStatusCodeException e) {
            log.error("Failed to login to AList, url: {}, status: {}, body: {}", getAlistUrl() + "/api/auth/login", e.getStatusCode(), e.getResponseBodyAsString());
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
            String targetUrl = url + "/api/me";
            log.info("testConnection - token length: {}, url: {}", token != null ? token.length() : 0, targetUrl);

            if (token == null || token.isEmpty()) {
                result.put("success", false);
                result.put("message", "AList 鉴权失败：未获取到 token，请检查 alist_token 或用户名密码配置");
                return result;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            HttpEntity<?> request = new HttpEntity<>(headers);
            log.info("Making request to: {}", targetUrl);
            
            // Use exchange instead of getForEntity for more control
            ResponseEntity<Map> response = restTemplate.exchange(
                targetUrl,
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
                result.put("message", "AList 连接失败，请求 " + targetUrl + " 返回状态: " + response.getStatusCode());
            }
        } catch (HttpStatusCodeException e) {
            String url = getAlistUrl() + "/api/me";
            result.put("success", false);
            result.put("message", "AList 连接失败，请求 " + url + " 返回状态: " + e.getStatusCode() + ", 响应: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "AList 连接异常: " + e.getMessage());
        }
        return result;
    }

    public Map<String, Object> getStorageList() {
        Map<String, Object> result = new HashMap<>();
        try {
            String token = getAlistToken();
            if (token == null || token.isEmpty()) {
                result.put("success", false);
                result.put("message", "AList 鉴权失败：未获取到 token，请检查 alist_token 或用户名密码配置");
                return result;
            }

            String targetUrl = getAlistUrl() + "/api/storage/list";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", token);
            HttpEntity<?> request = new HttpEntity<>(headers);
            ResponseEntity<Map> response = restTemplate.exchange(targetUrl, HttpMethod.GET, request, Map.class);
            result.put("success", response.getStatusCode() == HttpStatus.OK);
            result.put("data", response.getBody());
            if (response.getStatusCode() != HttpStatus.OK) {
                result.put("message", "AList 存储列表请求失败，URL: " + targetUrl + ", 状态: " + response.getStatusCode());
            }
        } catch (HttpStatusCodeException e) {
            String url = getAlistUrl() + "/api/storage/list";
            result.put("success", false);
            result.put("message", "AList 存储列表请求失败，URL: " + url + ", 状态: " + e.getStatusCode() + ", 响应: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "AList 存储列表请求异常: " + e.getMessage());
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
