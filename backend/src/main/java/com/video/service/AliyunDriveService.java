package com.video.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class AliyunDriveService {
    
    private final VideoService videoService;
    private final TranscodeService transcodeService;
    private final SystemConfigService systemConfigService;
    
    private static final String API_BASE = "https://openapi.alipan.com/v2";
    
    private String accessToken;
    
    private String getRefreshToken() {
        return systemConfigService.getConfig(SystemConfigService.ALIYUN_REFRESH_TOKEN, "");
    }
    
    private String getRootFolderId() {
        return systemConfigService.getConfig(SystemConfigService.ALIYUN_ROOT_FOLDER, "root");
    }
    
    public String getAccessToken() {
        if (accessToken == null || accessToken.isEmpty()) {
            refreshAccessToken();
        }
        return accessToken;
    }
    
    public void refreshAccessToken() {
        try {
            URL url = new URL(API_BASE + "/oauth/token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            
            String refreshToken = getRefreshToken();
            if (refreshToken == null || refreshToken.isEmpty()) {
                log.warn("Refresh token is empty");
                return;
            }
            
            String body = String.format("{\"refresh_token\":\"%s\",\"grant_type\":\"refresh_token\"}", refreshToken);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes());
            }
            
            int code = conn.getResponseCode();
            log.info("Token refresh response code: {}", code);
            if (code == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    log.info("Token refresh response: {}", response);
                    Map<String, Object> json = parseJson(response.toString());
                    accessToken = (String) json.get("access_token");
                    log.info("Access token obtained: {}", accessToken != null ? "yes" : "no");
                }
            } else {
                log.error("Failed to refresh token: {}", code);
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    log.error("Error response: {}", response);
                }
            }
        } catch (Exception e) {
            log.error("Error refreshing access token", e);
        }
    }
    
    public List<AliyunFile> searchFiles(String query) {
        List<AliyunFile> results = new ArrayList<>();
        try {
            String token = getAccessToken();
            URL url = new URL(API_BASE + "/file/search");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setDoOutput(true);
            
            String body = String.format(
                "{\"query\":\"%s\",\"parent_file_id\":\"%s\",\"limit\":50}",
                query, getRootFolderId()
            );
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes());
            }
            
            int code = conn.getResponseCode();
            if (code == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    Map<String, Object> json = parseJson(response.toString());
                    Map<String, Object> data = (Map<String, Object>) json.get("data");
                    if (data != null) {
                        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
                        if (items != null) {
                            for (Map<String, Object> item : items) {
                                AliyunFile file = new AliyunFile();
                                file.setId((String) item.get("file_id"));
                                file.setName((String) item.get("name"));
                                file.setSize(((Number) item.get("size")).longValue());
                                file.setType((String) item.get("type"));
                                file.setCategory((String) item.get("category"));
                                file.setParentId((String) item.get("parent_file_id"));
                                results.add(file);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error searching files: {}", query, e);
        }
        return results;
    }
    
    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();
        try {
            String token = getAccessToken();
            if (token == null || token.isEmpty()) {
                result.put("success", false);
                result.put("message", "Token无效或未配置");
                return result;
            }
            
            URL url = new URL(API_BASE + "/file/list");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setDoOutput(true);
            
            String body = String.format("{\"parent_file_id\":\"%s\",\"limit\":50}", getRootFolderId());
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes());
            }
            
            int code = conn.getResponseCode();
            if (code == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    Map<String, Object> json = parseJson(response.toString());
                    Map<String, Object> data = (Map<String, Object>) json.get("data");
                    
                    result.put("success", true);
                    result.put("message", "连接成功");
                    
                    if (data != null) {
                        List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
                        List<AliyunFile> files = new ArrayList<>();
                        if (items != null) {
                            for (Map<String, Object> item : items) {
                                AliyunFile file = new AliyunFile();
                                file.setId((String) item.get("file_id"));
                                file.setName((String) item.get("name"));
                                file.setSize(((Number) item.get("size")).longValue());
                                file.setType((String) item.get("type"));
                                file.setCategory((String) item.get("category"));
                                files.add(file);
                            }
                        }
                        result.put("files", files);
                        result.put("total", files.size());
                    }
                }
            } else {
                result.put("success", false);
                result.put("message", "API返回错误: " + code);
            }
        } catch (Exception e) {
            log.error("Error testing connection", e);
            result.put("success", false);
            result.put("message", "连接失败: " + e.getMessage());
        }
        return result;
    }
    
    public String getDownloadUrl(String fileId) {
        try {
            String token = getAccessToken();
            URL url = new URL(API_BASE + "/file/get_download_url");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setDoOutput(true);
            
            String body = String.format("{\"file_id\":\"%s\"}", fileId);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes());
            }
            
            int code = conn.getResponseCode();
            if (code == 200) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    Map<String, Object> json = parseJson(response.toString());
                    Map<String, Object> data = (Map<String, Object>) json.get("data");
                    if (data != null) {
                        return (String) data.get("url");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error getting download URL for file: {}", fileId, e);
        }
        return null;
    }
    
    public String downloadFile(String fileId, String targetDir) {
        String downloadUrl = getDownloadUrl(fileId);
        if (downloadUrl == null) {
            return null;
        }
        
        try {
            String fileName = fileId;
            for (AliyunFile f : searchFiles("")) {
                if (f.getId().equals(fileId)) {
                    fileName = f.getName();
                    break;
                }
            }
            
            Path dir = Paths.get(targetDir);
            Files.createDirectories(dir);
            Path targetPath = dir.resolve(fileName);
            
            URL url = new URL(downloadUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            
            try (InputStream is = conn.getInputStream();
                 FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, bytesRead);
                }
            }
            
            return targetPath.toString();
        } catch (Exception e) {
            log.error("Error downloading file: {}", fileId, e);
            return null;
        }
    }
    
    public CompletableFuture<String> downloadAndTranscode(String fileId, String videoTitle) {
        return CompletableFuture.supplyAsync(() -> {
            String tempDir = System.getProperty("java.io.tmpdir");
            String downloadedPath = downloadFile(fileId, tempDir);
            
            if (downloadedPath == null) {
                throw new RuntimeException("Download failed");
            }
            
            java.io.File videoFile = new java.io.File(downloadedPath);
            
            VideoService.UploadResult result = videoService.processVideoFile(videoFile, videoTitle);
            
            return result.getUuid();
        });
    }
    
    private Map<String, Object> parseJson(String json) {
        Map<String, Object> result = new HashMap<>();
        int i = 0;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '{') {
                return parseObject(json, new int[]{0});
            }
            i++;
        }
        return result;
    }
    
    private List<Object> parseArray(String json, int[] pos) {
        List<Object> list = new ArrayList<>();
        pos[0]++;
        skipWhitespace(json, pos);
        
        while (json.charAt(pos[0]) != ']') {
            skipWhitespace(json, pos);
            char c = json.charAt(pos[0]);
            if (c == '{') {
                list.add(parseObject(json, pos));
            } else if (c == '"') {
                list.add(parseString(json, pos));
            } else if (c == 't' || c == 'f') {
                list.add(parseBoolean(json, pos));
            } else if (c == 'n') {
                list.add(null);
                pos[0] += 4;
            } else {
                list.add(parseNumber(json, pos));
            }
            skipWhitespace(json, pos);
            if (json.charAt(pos[0]) == ',') {
                pos[0]++;
            }
        }
        pos[0]++;
        return list;
    }
    
    private Map<String, Object> parseObject(String json, int[] pos) {
        Map<String, Object> map = new HashMap<>();
        pos[0]++;
        skipWhitespace(json, pos);
        
        while (json.charAt(pos[0]) != '}') {
            skipWhitespace(json, pos);
            String key = parseString(json, pos);
            skipWhitespace(json, pos);
            pos[0]++;
            skipWhitespace(json, pos);
            
            char c = json.charAt(pos[0]);
            Object value;
            if (c == '{') {
                value = parseObject(json, pos);
            } else if (c == '[') {
                pos[0]++;
                value = parseArray(json, pos);
            } else if (c == '"') {
                value = parseString(json, pos);
            } else if (c == 't' || c == 'f') {
                value = parseBoolean(json, pos);
            } else if (c == 'n') {
                value = null;
                pos[0] += 4;
            } else {
                value = parseNumber(json, pos);
            }
            map.put(key, value);
            skipWhitespace(json, pos);
            if (json.charAt(pos[0]) == ',') {
                pos[0]++;
            }
        }
        pos[0]++;
        return map;
    }
    
    private String parseString(String json, int[] pos) {
        pos[0]++;
        StringBuilder sb = new StringBuilder();
        while (json.charAt(pos[0]) != '"') {
            char c = json.charAt(pos[0]);
            if (c == '\\') {
                pos[0]++;
                c = json.charAt(pos[0]);
            }
            sb.append(c);
            pos[0]++;
        }
        pos[0]++;
        return sb.toString();
    }
    
    private Number parseNumber(String json, int[] pos) {
        StringBuilder sb = new StringBuilder();
        while (pos[0] < json.length()) {
            char c = json.charAt(pos[0]);
            if ((c >= '0' && c <= '9') || c == '.' || c == '-' || c == 'e' || c == 'E') {
                sb.append(c);
                pos[0]++;
            } else {
                break;
            }
        }
        String num = sb.toString();
        return num.contains(".") ? Double.parseDouble(num) : Long.parseLong(num);
    }
    
    private Boolean parseBoolean(String json, int[] pos) {
        if (json.charAt(pos[0]) == 't') {
            pos[0] += 4;
            return true;
        } else {
            pos[0] += 5;
            return false;
        }
    }
    
    private void skipWhitespace(String json, int[] pos) {
        while (pos[0] < json.length() && Character.isWhitespace(json.charAt(pos[0]))) {
            pos[0]++;
        }
    }
    
    public static class AliyunFile {
        private String id;
        private String name;
        private Long size;
        private String type;
        private String category;
        private String parentId;
        
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Long getSize() { return size; }
        public void setSize(Long size) { this.size = size; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getParentId() { return parentId; }
        public void setParentId(String parentId) { this.parentId = parentId; }
    }
}
