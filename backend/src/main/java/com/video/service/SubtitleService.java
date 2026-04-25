package com.video.service;

import com.video.entity.Subtitle;
import com.video.entity.Video;
import com.video.repository.SubtitleRepository;
import com.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubtitleService {

    private static final String SUBTITLE_STORAGE_PATH = "/data/subtitles";
    private static final List<String> DEFAULT_LANGUAGES = Arrays.asList("en", "zh-cn");

    private final SubtitleRepository subtitleRepository;
    private final VideoRepository videoRepository;
    private final OpenSubtitlesProvider openSubtitlesProvider;
    private final EncodingService encodingService;

    @Async("transcodeExecutor")
    @Transactional
    public CompletableFuture<Void> searchAndDownloadSubtitlesAsync(String videoUuid) {
        log.info("Starting async subtitle search for video: {}", videoUuid);

        Video video = videoRepository.findByUuid(videoUuid).orElse(null);
        if (video == null) {
            log.error("Video not found: {}", videoUuid);
            return CompletableFuture.completedFuture(null);
        }

        String imdbId = video.getImdbId();
        if (imdbId == null || imdbId.isEmpty()) {
            log.warn("Video {} has no IMDB ID, cannot search subtitles via API", videoUuid);
            return CompletableFuture.completedFuture(null);
        }

        String hlsDir = getHlsDirectory(video);
        if (hlsDir == null) {
            log.error("Cannot find HLS directory for video: {}", videoUuid);
            return CompletableFuture.completedFuture(null);
        }

        Path subtitleDir = Paths.get(SUBTITLE_STORAGE_PATH, videoUuid);
        try {
            Files.createDirectories(subtitleDir);
        } catch (IOException e) {
            log.error("Failed to create subtitle directory: {}", subtitleDir, e);
            return CompletableFuture.completedFuture(null);
        }

        for (String language : DEFAULT_LANGUAGES) {
            Optional<Subtitle> existing = subtitleRepository.findByVideoUuidAndLanguage(videoUuid, language);
            if (existing.isPresent() && Subtitle.STATUS_DOWNLOADED.equals(existing.get().getStatus())) {
                log.info("Subtitle {} already exists for video {}, skipping", language, videoUuid);
                continue;
            }

            Subtitle subtitle = existing.orElseGet(() -> createSubtitle(videoUuid, language));
            subtitle.setStatus(Subtitle.STATUS_SEARCHING);
            subtitleRepository.save(subtitle);

            try {
                searchAndDownloadForLanguageByImdbId(videoUuid, imdbId, hlsDir, subtitle);
            } catch (Exception e) {
                log.error("Failed to download subtitle for {} language: {}", language, e.getMessage());
                subtitle.setStatus(Subtitle.STATUS_FAILED);
                subtitleRepository.save(subtitle);
            }
        }

        log.info("Async subtitle search completed for video: {}", videoUuid);
        return CompletableFuture.completedFuture(null);
    }

    public void triggerSubtitleSearch(String videoUuid) {
        searchAndDownloadSubtitlesAsync(videoUuid);
    }

    @Transactional
    public List<Subtitle> searchAndDownloadSubtitles(String videoUuid) {
        Video video = videoRepository.findByUuid(videoUuid).orElse(null);
        if (video == null) {
            log.error("Video not found: {}", videoUuid);
            return Arrays.asList();
        }

        String imdbId = video.getImdbId();
        if (imdbId == null || imdbId.isEmpty()) {
            log.warn("Video {} has no IMDB ID, cannot search subtitles via API", videoUuid);
            return Arrays.asList();
        }

        log.info("Starting subtitle search for video: {} with IMDB ID: {}", video.getTitle(), imdbId);

        String hlsDir = getHlsDirectory(video);
        if (hlsDir == null) {
            log.error("Cannot find HLS directory for video: {}", videoUuid);
            return Arrays.asList();
        }

        Path subtitleDir = Paths.get(SUBTITLE_STORAGE_PATH, videoUuid);
        try {
            Files.createDirectories(subtitleDir);
        } catch (IOException e) {
            log.error("Failed to create subtitle directory: {}", subtitleDir, e);
            return Arrays.asList();
        }

        for (String language : DEFAULT_LANGUAGES) {
            Optional<Subtitle> existing = subtitleRepository.findByVideoUuidAndLanguage(videoUuid, language);
            if (existing.isPresent() && Subtitle.STATUS_DOWNLOADED.equals(existing.get().getStatus())) {
                log.info("Subtitle {} already exists for video {}, skipping", language, videoUuid);
                continue;
            }

            Subtitle subtitle = existing.orElseGet(() -> createSubtitle(videoUuid, language));
            subtitle.setStatus(Subtitle.STATUS_SEARCHING);
            subtitleRepository.save(subtitle);

            try {
                searchAndDownloadForLanguageByImdbId(videoUuid, imdbId, hlsDir, subtitle);
            } catch (Exception e) {
                log.error("Failed to download subtitle for {} language: {}", language, e.getMessage());
                subtitle.setStatus(Subtitle.STATUS_FAILED);
                subtitleRepository.save(subtitle);
            }
        }

        return subtitleRepository.findByVideoUuid(videoUuid);
    }

    private String getHlsDirectory(Video video) {
        if (video.getHlsPath() != null && video.getHlsPath().contains("/data/videos/")) {
            String uuid = video.getUuid();
            Path hlsDir = Paths.get("/data/videos", uuid);
            if (Files.exists(hlsDir)) {
                return hlsDir.toString();
            }
        }
        return null;
    }

    private Subtitle createSubtitle(String videoUuid, String language) {
        Subtitle subtitle = new Subtitle();
        subtitle.setVideoUuid(videoUuid);
        subtitle.setLanguage(language);
        subtitle.setStatus(Subtitle.STATUS_PENDING);
        return subtitle;
    }

    private void searchAndDownloadForLanguageByImdbId(String videoUuid, String imdbId, String hlsDir, Subtitle subtitle) throws Exception {
        String language = subtitle.getLanguage();
        log.info("Searching {} subtitles for IMDB ID: {}", language, imdbId);

        List<OpenSubtitlesProvider.SubtitleSearchResult> results =
            openSubtitlesProvider.searchByImdbId(imdbId, Arrays.asList(language));

        if (results.isEmpty()) {
            log.info("No {} subtitles found for IMDB ID {}", language, imdbId);
            subtitle.setStatus(Subtitle.STATUS_NOT_FOUND);
            subtitleRepository.save(subtitle);
            return;
        }

        OpenSubtitlesProvider.SubtitleSearchResult bestResult = results.get(0);
        log.info("Found subtitle: {} (file_id: {})", bestResult.getFileName(), bestResult.getFileId());

        String subtitleDir = SUBTITLE_STORAGE_PATH + "/" + videoUuid;
        File downloadedFile = openSubtitlesProvider.downloadSubtitle(bestResult.getFileId(), subtitleDir);

        File subtitleFile;
        if (downloadedFile.getName().toLowerCase().endsWith(".zip")) {
            subtitleFile = extractSubtitleFromZip(downloadedFile, subtitleDir, bestResult.getFileName());
            if (subtitleFile == null) {
                throw new RuntimeException("Failed to extract subtitle from zip");
            }
            if (downloadedFile.exists()) downloadedFile.delete();
        } else {
            subtitleFile = downloadedFile;
        }

        File vttFile = new File(hlsDir + "/" + language + ".vtt");
        encodingService.convertToUtf8Vtt(subtitleFile, vttFile);

        subtitle.setFilePath(vttFile.getAbsolutePath());
        subtitle.setFileName(bestResult.getFileName());
        subtitle.setOpenSubtitlesId(bestResult.getId());
        subtitle.setFileSize(vttFile.length());
        subtitle.setCdNumber(bestResult.getCdNumber());
        subtitle.setVoteCount(bestResult.getVoteCount());
        subtitle.setStatus(Subtitle.STATUS_DOWNLOADED);
        subtitle.setDownloadedAt(LocalDateTime.now());
        subtitleRepository.save(subtitle);

        if (subtitleFile.exists() && !subtitleFile.getName().equals(downloadedFile.getName())) {
            subtitleFile.delete();
        }

        log.info("Successfully downloaded and converted {} subtitle to VTT: {}", language, vttFile.getAbsolutePath());
    }

    private File extractSubtitleFromZip(File zipFile, String outputDir, String preferredName) throws IOException {
        String extractedFileName = null;

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName().toLowerCase();
                if (name.endsWith(".srt") || name.endsWith(".ass") || name.endsWith(".ssa")) {
                    String baseName = Paths.get(name).getFileName().toString();
                    String outputPath = outputDir + "/" + baseName;

                    if (preferredName != null && preferredName.contains(".")) {
                        String preferredBase = preferredName.substring(0, preferredName.lastIndexOf('.'));
                        if (!baseName.startsWith(preferredBase)) {
                            continue;
                        }
                    }

                    try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = zis.read(buffer)) != -1) {
                            fos.write(buffer, 0, len);
                        }
                    }
                    log.info("Extracted subtitle: {}", outputPath);
                    extractedFileName = outputPath;
                    break;
                }
            }
        }

        if (extractedFileName != null) {
            return new File(extractedFileName);
        }
        return null;
    }

    public List<Subtitle> getSubtitlesForVideo(String videoUuid) {
        return subtitleRepository.findByVideoUuid(videoUuid);
    }

    public Optional<Subtitle> getSubtitle(String videoUuid, String language) {
        return subtitleRepository.findByVideoUuidAndLanguage(videoUuid, language);
    }

    public File getSubtitleFile(String videoUuid, String language) {
        Optional<Subtitle> subtitle = subtitleRepository.findByVideoUuidAndLanguage(videoUuid, language);
        if (subtitle.isPresent() && Subtitle.STATUS_DOWNLOADED.equals(subtitle.get().getStatus())) {
            String filePath = subtitle.get().getFilePath();
            if (filePath != null) {
                File file = new File(filePath);
                if (file.exists()) {
                    return file;
                }
            }
        }
        return null;
    }

    public boolean hasSubtitles(String videoUuid) {
        List<Subtitle> subtitles = subtitleRepository.findByVideoUuidAndStatus(videoUuid, Subtitle.STATUS_DOWNLOADED);
        return !subtitles.isEmpty();
    }

    @Transactional
    public boolean deleteSubtitle(String videoUuid, String language) {
        Optional<Subtitle> subtitle = subtitleRepository.findByVideoUuidAndLanguage(videoUuid, language);
        if (subtitle.isPresent()) {
            Subtitle sub = subtitle.get();
            if (sub.getFilePath() != null) {
                File file = new File(sub.getFilePath());
                if (file.exists()) {
                    file.delete();
                }
            }
            subtitleRepository.delete(sub);
            return true;
        }
        return false;
    }

    @Transactional
    public void initSubtitlesForVideo(String videoUuid) {
        for (String language : DEFAULT_LANGUAGES) {
            if (!subtitleRepository.existsByVideoUuidAndLanguage(videoUuid, language)) {
                Subtitle subtitle = createSubtitle(videoUuid, language);
                subtitleRepository.save(subtitle);
            }
        }
    }

    @Transactional
    public Subtitle searchSubtitleForLanguage(String videoUuid, String language) {
        Video video = videoRepository.findByUuid(videoUuid).orElse(null);
        if (video == null) {
            throw new RuntimeException("Video not found: " + videoUuid);
        }

        String imdbId = video.getImdbId();
        if (imdbId == null || imdbId.isEmpty()) {
            throw new RuntimeException("Video has no IMDB ID");
        }

        String hlsDir = getHlsDirectory(video);
        if (hlsDir == null) {
            throw new RuntimeException("Cannot find HLS directory");
        }

        Optional<Subtitle> existing = subtitleRepository.findByVideoUuidAndLanguage(videoUuid, language);
        Subtitle subtitle = existing.orElseGet(() -> createSubtitle(videoUuid, language));
        subtitle.setStatus(Subtitle.STATUS_SEARCHING);
        subtitleRepository.save(subtitle);

        try {
            searchAndDownloadForLanguageByImdbId(videoUuid, imdbId, hlsDir, subtitle);
        } catch (Exception e) {
            log.error("Failed to download subtitle for {} language: {}", language, e.getMessage());
            subtitle.setStatus(Subtitle.STATUS_FAILED);
            subtitleRepository.save(subtitle);
        }

        return subtitle;
    }
}