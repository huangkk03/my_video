package com.video.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OpenSubtitlesProvider {

    private static final String API_BASE = "https://api.opensubtitles.com/api/v1";
    private static final String API_TOKEN = "HSEi8Lob9ICzz7akXI9V6bgEa38p9rJX";
    private static final String USER_AGENT = "MKVVideoPlatform v1.0";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String calculateHash(String filePath) throws Exception {
        Path path = Paths.get(filePath);
        long fileSize = Files.size(path);

        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            byte[] header = new byte[65536];
            byte[] footer = new byte[65536];

            raf.readFully(header);

            raf.seek(Math.max(0, fileSize - 65536));
            raf.readFully(footer);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(header);
            baos.write(footer);
            baos.write(String.valueOf(fileSize).getBytes());
            byte[] data = baos.toByteArray();

            MessageDigest md = MessageDigest.getInstance("md5");
            byte[] hash = md.digest(data);
            return bytesToHex(hash);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public List<SubtitleSearchResult> searchByHash(String filePath, long fileSize, List<String> languages) throws Exception {
        String hash = calculateHash(filePath);
        log.info("Calculated hash for {}: {} (size: {})", filePath, hash, fileSize);

        StringBuilder langParam = new StringBuilder();
        for (int i = 0; i < languages.size(); i++) {
            if (i > 0) langParam.append("%2C");
            langParam.append(languages.get(i));
        }

        String url = API_BASE + "/subtitles?moviehash=" + hash + "&languages=" + langParam;
        log.info("Searching OpenSubtitles: {}", url);

        String response = executeCurlGet(url);
        return parseSubtitleResults(response);
    }

    public List<SubtitleSearchResult> searchByImdbId(String imdbId, List<String> languages) throws Exception {
        log.info("Searching OpenSubtitles by IMDB ID: {}", imdbId);

        StringBuilder langParam = new StringBuilder();
        for (int i = 0; i < languages.size(); i++) {
            if (i > 0) langParam.append("%2C");
            langParam.append(languages.get(i));
        }

        String url = API_BASE + "/subtitles?imdb_id=" + imdbId + "&languages=" + langParam;
        log.info("Searching OpenSubtitles: {}", url);

        String response = executeCurlGet(url);
        return parseSubtitleResults(response);
    }

    private List<SubtitleSearchResult> parseSubtitleResults(String jsonResponse) throws Exception {
        List<SubtitleSearchResult> results = new ArrayList<>();

        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode data = root.get("data");

        if (data == null || !data.isArray()) {
            log.info("No subtitle data in response");
            return results;
        }

        for (JsonNode item : data) {
            SubtitleSearchResult result = new SubtitleSearchResult();
            result.setId(item.has("id") ? item.get("id").asText() : "");

            JsonNode attributes = item.get("attributes");
            if (attributes != null) {
                result.setFileName(attributes.has("file_name") ? attributes.get("file_name").asText() : "");
                result.setLanguage(attributes.has("language") ? attributes.get("language").asText() : "");
                result.setDownloadCount(attributes.has("download_count") ? attributes.get("download_count").asInt() : 0);
                result.setFileId(attributes.has("files") && attributes.get("files").isArray()
                    && attributes.get("files").size() > 0
                    ? attributes.get("files").get(0).has("file_id")
                        ? attributes.get("files").get(0).get("file_id").asText()
                        : ""
                    : "");
                result.setCdNumber(attributes.has("cd_number") ? attributes.get("cd_number").asInt() : 1);
                result.setVoteCount(attributes.has("votes") ? attributes.get("votes").asInt() : 0);
            }

            if (result.getId() != null && !result.getId().isEmpty()) {
                results.add(result);
            }
        }

        log.info("Found {} subtitle results", results.size());
        return results;
    }

    public File downloadSubtitle(String fileId, String outputDir) throws Exception {
        String url = API_BASE + "/download";
        log.info("Downloading subtitle from: {}", url);

        String response = executeCurlPost(url, "{\"file_id\": " + fileId + "}");
        log.info("Download response: {}", response);

        if (response.contains("\"link\":")) {
            JsonNode jsonResponse = objectMapper.readTree(response);
            String downloadLink = jsonResponse.has("link") ? jsonResponse.get("link").asText() : null;
            String fileNameFromApi = jsonResponse.has("file_name") ? jsonResponse.get("file_name").asText() : null;

            if (downloadLink == null || downloadLink.isEmpty()) {
                throw new RuntimeException("No download link in response: " + response);
            }

            log.info("Following download link: {}", downloadLink);

            String fileName = fileNameFromApi != null ? fileNameFromApi : "subtitle_" + fileId + ".srt";
            Path outputPath = Paths.get(outputDir, fileName);

            Files.createDirectories(outputPath.getParent());
            executeCurlDownload(downloadLink, outputPath.toString());
            log.info("Downloaded to {}", outputPath);

            return outputPath.toFile();
        } else if (response.contains("We are down") || response.contains("Error 500")) {
            throw new RuntimeException("OpenSubtitles server is down: " + response);
        } else {
            throw new RuntimeException("Unexpected download response: " + response);
        }
    }

    private String executeCurlGet(String url) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
            "curl", "-s", "--noproxy", "*",
            "-X", "GET", url,
            "-H", "Api-Key: " + API_TOKEN,
            "-H", "User-Agent: " + USER_AGENT
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.warn("curl exited with code: {}", exitCode);
        }

        return output.toString();
    }

    private String executeCurlPost(String url, String body) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
            "curl", "-s", "--noproxy", "*",
            "-X", "POST", url,
            "-H", "Api-Key: " + API_TOKEN,
            "-H", "User-Agent: " + USER_AGENT,
            "-H", "Content-Type: application/json",
            "-d", body
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.warn("curl exited with code: {}", exitCode);
        }

        return output.toString();
    }

    private void executeCurlDownload(String url, String outputPath) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
            "curl", "-s", "--noproxy", "*",
            "-L", "-o", outputPath,
            "-A", USER_AGENT,
            url
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("curl download failed with exit code: " + exitCode);
        }
    }

    public static class SubtitleSearchResult {
        private String id;
        private String fileName;
        private String language;
        private int downloadCount;
        private String fileId;
        private int cdNumber;
        private int voteCount;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public int getDownloadCount() { return downloadCount; }
        public void setDownloadCount(int downloadCount) { this.downloadCount = downloadCount; }
        public String getFileId() { return fileId; }
        public void setFileId(String fileId) { this.fileId = fileId; }
        public int getCdNumber() { return cdNumber; }
        public void setCdNumber(int cdNumber) { this.cdNumber = cdNumber; }
        public int getVoteCount() { return voteCount; }
        public void setVoteCount(int voteCount) { this.voteCount = voteCount; }
    }
}