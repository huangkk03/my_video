package com.video.service;

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
import java.util.Map;

@Service
@Slf4j
public class CloudStorageService {

    @Value("${alist.url:http://alist:5244}")
    private String alistUrl;

    @Value("${alist.token}")
    private String alistToken;

    @Value("${download.path:/data/downloads/incoming}")
    private String downloadPath;

    @Autowired
    private TranscodeService transcodeService;

    private final RestTemplate restTemplate = new RestTemplate();

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

    private String getFileRawUrl(String path) {
        String apiUrl = alistUrl + "/api/fs/get";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", alistToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("path", path);
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

    private void downloadStream(String fileUrl, Path target) throws IOException {
        URL url = new URL(fileUrl);
        try (InputStream in = url.openStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}