package com.video.controller;

import com.video.entity.Subtitle;
import com.video.service.SubtitleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SubtitleController {

    private static final List<String> SUPPORTED_LANGUAGES = Arrays.asList("en", "zh-cn");

    private final SubtitleService subtitleService;

    @GetMapping("/api/videos/{uuid}/subtitles")
    public ResponseEntity<Map<String, Object>> getSubtitles(@PathVariable String uuid) {
        log.info("Getting subtitles for video: {}", uuid);

        subtitleService.initSubtitlesForVideo(uuid);
        List<Subtitle> subtitles = subtitleService.getSubtitlesForVideo(uuid);

        List<Map<String, Object>> subtitleList = new java.util.ArrayList<>();
        for (Subtitle sub : subtitles) {
            Map<String, Object> item = new HashMap<>();
            item.put("language", sub.getLanguage());
            item.put("label", getLanguageLabel(sub.getLanguage()));
            item.put("status", sub.getStatus());
            item.put("fileName", sub.getFileName());
            item.put("fileSize", sub.getFileSize());
            item.put("hasFile", sub.getFilePath() != null && new File(sub.getFilePath()).exists());
            subtitleList.add(item);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("videoUuid", uuid);
        response.put("subtitles", subtitleList);
        response.put("total", subtitleList.size());
        response.put("hasSubtitles", subtitleService.hasSubtitles(uuid));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/videos/{uuid}/subtitles/search")
    public ResponseEntity<Map<String, Object>> searchSubtitles(@PathVariable String uuid) {
        log.info("Triggering subtitle search for video: {}", uuid);

        CompletableFuture.runAsync(() -> {
            try {
                subtitleService.initSubtitlesForVideo(uuid);
                subtitleService.searchAndDownloadSubtitles(uuid);
            } catch (Exception e) {
                log.error("Subtitle search failed for video: {}", uuid, e);
            }
        });

        Map<String, Object> response = new HashMap<>();
        response.put("status", "searching");
        response.put("message", "Subtitle search initiated");
        response.put("videoUuid", uuid);

        return ResponseEntity.accepted().body(response);
    }

    @PostMapping("/api/videos/{uuid}/subtitles/{language}/search")
    public ResponseEntity<Map<String, Object>> searchSubtitleForLanguage(
            @PathVariable String uuid,
            @PathVariable String language) {
        log.info("Triggering subtitle search for video: {} language: {}", uuid, language);

        subtitleService.initSubtitlesForVideo(uuid);

        CompletableFuture.runAsync(() -> {
            try {
                subtitleService.searchSubtitleForLanguage(uuid, language);
            } catch (Exception e) {
                log.error("Subtitle search failed for video: {} language: {}", uuid, language, e);
            }
        });

        Map<String, Object> response = new HashMap<>();
        response.put("status", "searching");
        response.put("message", "Subtitle search initiated");
        response.put("videoUuid", uuid);
        response.put("language", language);

        return ResponseEntity.accepted().body(response);
    }

    @DeleteMapping("/api/videos/{uuid}/subtitles/{language}")
    public ResponseEntity<Map<String, Object>> deleteSubtitle(
            @PathVariable String uuid,
            @PathVariable String language) {
        log.info("Deleting subtitle for video: {} language: {}", uuid, language);

        boolean deleted = subtitleService.deleteSubtitle(uuid, language);

        Map<String, Object> response = new HashMap<>();
        if (deleted) {
            response.put("status", "deleted");
            response.put("message", "Subtitle deleted");
            return ResponseEntity.ok(response);
        } else {
            response.put("status", "not_found");
            response.put("message", "Subtitle not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @GetMapping("/api/subtitles/{uuid}/{lang}.vtt")
    public ResponseEntity<?> getSubtitleFile(@PathVariable String uuid, @PathVariable String lang) {
        log.info("Getting subtitle file: {}/{}", uuid, lang);

        File subtitleFile = subtitleService.getSubtitleFile(uuid, lang);

        if (subtitleFile == null || !subtitleFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/vtt"));
            headers.setContentLength(subtitleFile.length());
            headers.set("Access-Control-Allow-Origin", "*");

            return ResponseEntity.ok()
                .headers(headers)
                .body(new org.springframework.core.io.FileSystemResource(subtitleFile));

        } catch (Exception e) {
            log.error("Failed to serve subtitle file: {}/{}", uuid, lang, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/api/videos/{uuid}/subtitles/status")
    public ResponseEntity<Map<String, Object>> getSubtitleStatus(@PathVariable String uuid) {
        subtitleService.initSubtitlesForVideo(uuid);
        List<Subtitle> subtitles = subtitleService.getSubtitlesForVideo(uuid);

        Map<String, Object> status = new HashMap<>();
        status.put("videoUuid", uuid);
        status.put("hasSubtitles", subtitleService.hasSubtitles(uuid));

        List<Map<String, Object>> languageStatus = new java.util.ArrayList<>();
        for (Subtitle sub : subtitles) {
            Map<String, Object> item = new HashMap<>();
            item.put("language", sub.getLanguage());
            item.put("label", getLanguageLabel(sub.getLanguage()));
            item.put("status", sub.getStatus());
            item.put("hasFile", sub.getFilePath() != null && new File(sub.getFilePath()).exists());
            item.put("fileName", sub.getFileName());
            item.put("fileSize", sub.getFileSize());
            languageStatus.add(item);
        }
        status.put("languages", languageStatus);

        return ResponseEntity.ok(status);
    }

    private String getLanguageLabel(String language) {
        switch (language) {
            case "en": return "English";
            case "zh-cn": return "简体中文";
            case "zh-tw": return "繁體中文";
            case "ja": return "日本語";
            case "ko": return "한국어";
            default: return language.toUpperCase();
        }
    }
}