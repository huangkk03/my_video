package com.video.service;

import com.video.entity.ImportTask;
import com.video.entity.Season;
import com.video.entity.Series;
import com.video.entity.Video;
import com.video.repository.ImportTaskRepository;
import com.video.repository.SeasonRepository;
import com.video.repository.SeriesRepository;
import com.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudMediaService {

    private static final long SCRAPE_TIMEOUT_SECONDS = 30L;
    
    private final CloudStorageService cloudStorageService;
    private final VideoService videoService;
    private final TranscodeService transcodeService;
    private final ScrapingAggregationService scrapingAggregationService;
    private final ImportTaskRepository importTaskRepository;
    private final SeriesRepository seriesRepository;
    private final SeasonRepository seasonRepository;
    private final VideoRepository videoRepository;
    
    @Value("${video.storage-path:/data/videos}")
    private String videoStoragePath;
    
    public ImportTask startImport(String fileName, String filePath, Long fileSize, String folderName) {
        String taskId = UUID.randomUUID().toString();
        
        ImportTask task = new ImportTask();
        task.setTaskId(taskId);
        task.setSourceName(fileName);
        task.setSourcePath(filePath);
        task.setSourceSize(fileSize);
        task.setStatus("pending");
        task.setProgress(0);
        task.setMessage("Preparing import...");
        
        task = importTaskRepository.save(task);
        processImportAsync(taskId, folderName);
        
        return task;
    }
    
    @Async
    public void processImportAsync(String taskId, String folderName) {
        ImportTask task = importTaskRepository.findByTaskId(taskId).orElse(null);
        if (task == null) {
            log.error("Task not found: {}", taskId);
            return;
        }
        
        try {
            task.setStatus("preparing");
            task.setMessage("Preparing stream transcode...");
            task.setProgress(5);
            importTaskRepository.save(task);

            String alistPath = task.getSourcePath();

            task.setProgress(10);
            task.setMessage("Scraping metadata...");
            importTaskRepository.save(task);

            task.setStatus("scraping");
            task.setMessage("Scraping metadata...");
            importTaskRepository.save(task);

            String fileName = task.getSourceName();
            String title = buildScrapeTitleFromFilename(fileName);
            ScrapingAggregationService.MetadataResult metadataResult = scrapMetadata(task, title);

            task.setProgress(30);
            task.setMessage("Starting stream transcode...");
            importTaskRepository.save(task);

            task.setStatus("transcoding");
            task.setMessage("Transcoding...");
            importTaskRepository.save(task);

            task = importTaskRepository.findByTaskId(taskId).orElse(null);
            if (task == null || task.getStatus().equals("failed")) {
                log.info("Task {} was cancelled, skipping transcode", taskId);
                return;
            }

            String videoUuid = startTranscodingFromAlist(task, alistPath, title);
            applyScrapedMetadataToVideo(videoUuid, metadataResult);

            task.setVideoUuid(videoUuid);
            task.setProgress(100);
            task.setStatus("completed");
            task.setMessage("Import complete");
            importTaskRepository.save(task);

            autoAssignToSeries(videoUuid, title, task.getSourceName());

        } catch (Exception e) {
            log.error("Import failed for task: {}", taskId, e);
            task.setStatus("failed");
            task.setMessage("Failed: " + e.getMessage());
            importTaskRepository.save(task);
        }
    }
    
    private void downloadFile(String downloadUrl, Path targetPath, ImportTask task) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(true);
        
        // Handle cross-protocol redirects (HTTP -> HTTPS) which Java doesn't follow automatically
        int status = conn.getResponseCode();
        if (status == HttpURLConnection.HTTP_MOVED_TEMP
            || status == HttpURLConnection.HTTP_MOVED_PERM
            || status == HttpURLConnection.HTTP_SEE_OTHER) {
            String newUrl = conn.getHeaderField("Location");
            log.info("Redirecting download to: {}", newUrl);
            conn = (HttpURLConnection) new URL(newUrl).openConnection();
            conn.setRequestMethod("GET");
        }
        
        long totalBytes = conn.getContentLengthLong();
        long downloadedBytes = 0;
        
        try (InputStream is = conn.getInputStream();
             FileOutputStream fos = new FileOutputStream(targetPath.toFile())) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            int lastProgress = 40;
            
            while ((bytesRead = is.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
                downloadedBytes += bytesRead;
                
                if (totalBytes > 0) {
                    int prog = (int) ((downloadedBytes * 40 / totalBytes) + 10);
                    if (prog > lastProgress) {
                        task.setProgress(prog);
                        importTaskRepository.save(task);
                        lastProgress = prog;
                    }
                }
            }
        }
        
        log.info("Downloaded file to: {}", targetPath);
    }
    
    private ScrapingAggregationService.MetadataResult scrapMetadata(ImportTask task, String title) {
        try {
            log.info("Start scraping metadata for task: {}, query: {}", task.getTaskId(), title);
            CompletableFuture<ScrapingAggregationService.MetadataResult> future = 
                scrapingAggregationService.searchMetadata(title);
            
            ScrapingAggregationService.MetadataResult result = future.get(SCRAPE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (result != null) {
                String metadataInfo = "";
                if (result.getTitle() != null) {
                    metadataInfo += "Title: " + result.getTitle();
                }
                if (result.getRating() != null) {
                    metadataInfo += " | Rating: " + result.getRating();
                }
                task.setMessage("Scraping complete: " + metadataInfo);
                importTaskRepository.save(task);
                log.info("Scraping success for task: {}, matched title: {}", task.getTaskId(), result.getTitle());
                return result;
            }
            log.warn("Scraping returned empty result for task: {}, query: {}", task.getTaskId(), title);
        } catch (java.util.concurrent.TimeoutException e) {
            log.warn("Scraping timeout for task: {}", task.getTaskId());
            task.setMessage("Scraping timeout, skipping...");
            importTaskRepository.save(task);
        } catch (Exception e) {
            log.warn("Scraping failed for task: {}", task.getTaskId(), e);
            task.setMessage("Scraping failed, skipping...");
            importTaskRepository.save(task);
        }
        return null;
    }

    private String buildScrapeTitleFromFilename(String fileName) {
        String title = fileName;
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0) {
            title = fileName.substring(0, dotIndex);
        }
        title = title.replaceAll("[._]", " ");
        title = cleanMovieTitle(title);
        title = title.replaceAll("\\s+", " ").trim();
        if (title.isEmpty()) {
            return fileName;
        }
        return title;
    }

    private String cleanMovieTitle(String title) {
        title = removeYear(title);
        title = removeVideoQualityTags(title);
        title = removeCodecTags(title);
        title = removeLanguageTags(title);
        title = removeReleaseGroup(title);
        title = removeCommonSuffixes(title);
        title = removeEpisodeIdentifiers(title);
        title = removeSpecialCharacters(title);
        return title.trim();
    }

    private String removeYear(String title) {
        Pattern pattern = Pattern.compile("(.*?)\\s*(19|20)\\d{2}\\s*(.*)");
        Matcher matcher = pattern.matcher(title);
        if (matcher.matches()) {
            String before = matcher.group(1).trim();
            String year = matcher.group(2);
            String after = matcher.group(3).trim();
            if (!before.isEmpty()) {
                return before + " " + year;
            }
            return before + year + " " + after;
        }
        return title;
    }

    private String removeVideoQualityTags(String title) {
        String[] tags = {
            "BD1080P", "BD720P", "1080P", "720P", "480P", "2160P", "4K",
            "BluRay", "Blu-ray", "HDTV", "WEBRIP", "WEB-DL", "DVDRIP",
            "UHD", "HD", "Remastered", "Extended"
        };
        for (String tag : tags) {
            title = title.replaceAll("(?i)" + tag, " ");
        }
        return title;
    }

    private String removeCodecTags(String title) {
        String[] tags = {
            "X264", "X265", "H264", "H265", "HEVC", "AVC",
            "DTS", "AAC", "AC3", "DTS-HD", "TrueHD", "Atmos",
            "8bit", "10bit", "12bit"
        };
        for (String tag : tags) {
            title = title.replaceAll("(?i)" + tag, " ");
        }
        return title;
    }

    private String removeLanguageTags(String title) {
        String[] tags = {
            "Mandarin", "English", "CHS-ENG", "CHT-ENG", "CHS", "CHT",
            "ENG", "中英双语", "双语", "中文字幕", "国语", "粤语"
        };
        for (String tag : tags) {
            title = title.replaceAll("(?i)" + tag, " ");
        }
        return title;
    }

    private String removeReleaseGroup(String title) {
        String[] groups = {
            "Adans", "HR", "PRiViTHD", "CHD", "M-team", "WiKi", "CTRLHD",
            "SPARKS", "CMCT", "TLF", "FRDS", "MySiLU", "BeiTai"
        };
        for (String group : groups) {
            title = title.replaceAll("(?i)" + group, " ");
        }
        return title;
    }

    private String removeCommonSuffixes(String title) {
        String[] suffixes = {
            "1080p", "720p", "576p", "480p", "360p",
            "webrip", "dvdrip", "bluray", "hdtv"
        };
        for (String suffix : suffixes) {
            title = title.replaceAll("(?i)" + suffix, " ");
        }
        return title;
    }

    private String removeEpisodeIdentifiers(String title) {
        title = title.replaceAll("(?i)S\\d{1,2}E\\d{1,3}", " ");
        title = title.replaceAll("(?i)EP\\s*\\d{1,3}", " ");
        title = title.replaceAll("(?i)第\\s*\\d{1,3}\\s*[集话]", " ");
        title = title.replaceAll("\\d{1,2}x\\d{1,3}", " ");
        return title;
    }

    private String removeSpecialCharacters(String title) {
        title = title.replaceAll("[&\\[\\](){}@#$%^*+=|<>\\/\\\\]", " ");
        return title;
    }

    private void applyScrapedMetadataToVideo(String videoUuid, ScrapingAggregationService.MetadataResult metadataResult) {
        if (metadataResult == null) {
            log.info("No metadata to apply for video: {}", videoUuid);
            return;
        }
        Optional<Video> videoOpt = videoRepository.findByUuid(videoUuid);
        if (!videoOpt.isPresent()) {
            log.warn("Video not found when applying scraped metadata: {}", videoUuid);
            return;
        }
        Video video = videoOpt.get();
        if (metadataResult.getTitle() != null && !metadataResult.getTitle().isEmpty()) {
            video.setTitle(metadataResult.getTitle());
        }
        if (metadataResult.getOverview() != null && !metadataResult.getOverview().isEmpty()) {
            video.setOverview(metadataResult.getOverview());
        }
        if (metadataResult.getPosterUrl() != null && !metadataResult.getPosterUrl().isEmpty()) {
            video.setPosterPath(metadataResult.getPosterUrl());
        }
        if (metadataResult.getRating() != null) {
            video.setRating(metadataResult.getRating());
        }
        if (metadataResult.getReleaseDate() != null && metadataResult.getReleaseDate().length() >= 4) {
            try {
                video.setReleaseYear(Integer.parseInt(metadataResult.getReleaseDate().substring(0, 4)));
            } catch (Exception ignored) {
                log.warn("Failed to parse release year for video: {}, releaseDate: {}", videoUuid, metadataResult.getReleaseDate());
            }
        }
        video.setScrapingStatus("success");
        videoRepository.save(video);
        log.info("Applied scraped metadata to video: {}, title: {}", videoUuid, video.getTitle());
    }
    
    private String startTranscoding(ImportTask task, Path videoPath, String title) throws Exception {
        File videoFile = videoPath.toFile();
        VideoService.UploadResult uploadResult = videoService.processVideoFile(videoFile, title);
        return uploadResult.getUuid();
    }
    
    public List<ImportTask> getAllTasks() {
        return importTaskRepository.findAllByOrderByCreatedAtDesc();
    }
    
    public List<ImportTask> getActiveTasks() {
        return importTaskRepository.findByStatusNotOrderByCreatedAtDesc("completed");
    }
    
    public ImportTask getTask(String taskId) {
        return importTaskRepository.findByTaskId(taskId).orElse(null);
    }
    
    public boolean cancelTask(String taskId) {
        ImportTask task = importTaskRepository.findByTaskId(taskId).orElse(null);
        if (task != null && !task.getStatus().equals("completed") && !task.getStatus().equals("failed")) {
            task.setStatus("failed");
            task.setMessage("Task cancelled by user");
            importTaskRepository.save(task);
            return true;
        }
        return false;
    }

    private String startTranscodingFromAlist(ImportTask task, String alistPath, String title) {
        String uuid = UUID.randomUUID().toString();

        String sourceUrl = cloudStorageService.getFileRawUrl(alistPath);
        if (sourceUrl == null) {
            throw new RuntimeException("Failed to get raw URL from AList for path: " + alistPath);
        }

        Video video = new Video();
        video.setUuid(uuid);
        video.setTitle(title);
        video.setOriginalFilename(task.getSourceName());
        video.setAlistPath(alistPath);
        video.setSourceType("remote_alist");
        video.setFileSize(task.getSourceSize());
        video.setStatus("pending");
        video.setCreatedAt(LocalDateTime.now());
        video = videoRepository.save(video);

        transcodeService.transcodeVideoFromUrl(uuid, sourceUrl);

        return uuid;
    }

    private void autoAssignToSeries(String videoUuid, String folderName, String fileName) {
        try {
            String matchName = extractSeriesNameForMatch(folderName, fileName);
            // Find series by fuzzy matching name
            List<Series> matchedSeries = seriesRepository.findByNameContaining(matchName);
            
            if (matchedSeries.isEmpty()) {
                log.info("No series found matching: {}", matchName);
                return;
            }
            
            // Use the first match (best match)
            Series series = matchedSeries.get(0);
            log.info("Auto-assigning video {} to series: {}", videoUuid, series.getName());
            
            // Parse season number from filename (e.g., S01E01, 01x01)
            Integer seasonNumber = parseSeasonNumber(fileName);
            Integer episodeNumber = parseEpisodeNumber(fileName);
            if (seasonNumber == null && episodeNumber != null) {
                seasonNumber = 1;
            }
            
            if (seasonNumber != null) {
                // Find or create the season
                Optional<Season> existingSeason = seasonRepository.findBySeriesIdAndSeasonNumber(series.getId(), seasonNumber);
                Season season;
                if (existingSeason.isPresent()) {
                    season = existingSeason.get();
                } else {
                    // Create the season
                    season = new Season();
                    season.setSeriesId(series.getId());
                    season.setSeasonNumber(seasonNumber);
                    season.setName("第 " + seasonNumber + " 季");
                    season = seasonRepository.save(season);
                    log.info("Created new season: {}", season.getName());
                }
                
                // Assign video to season
                Optional<Video> videoOpt = videoRepository.findByUuid(videoUuid);
                if (videoOpt.isPresent()) {
                    Video video = videoOpt.get();
                    video.setSeriesId(series.getId());
                    video.setSeasonId(season.getId());
                    video.setEpisodeNumber(episodeNumber);
                    videoRepository.save(video);
                    log.info("Assigned video {} to series {} season {} episode {}", 
                        videoUuid, series.getName(), seasonNumber, episodeNumber);
                }
            } else {
                // No season info, just assign to series
                Optional<Video> videoOpt = videoRepository.findByUuid(videoUuid);
                if (videoOpt.isPresent()) {
                    Video video = videoOpt.get();
                    video.setSeriesId(series.getId());
                    videoRepository.save(video);
                    log.info("Assigned video {} to series {}", videoUuid, series.getName());
                }
            }
        } catch (Exception e) {
            log.warn("Auto-assign to series failed: {}", e.getMessage());
        }
    }

    private String extractSeriesNameForMatch(String title, String fileName) {
        String base = title;
        if (base == null || base.trim().isEmpty()) {
            base = buildScrapeTitleFromFilename(fileName);
        }
        base = base.replaceAll("(?i)[._\\-\\s]*S\\d{1,2}E\\d{1,3}.*$", "");
        base = base.replaceAll("(?i)[._\\-\\s]*\\d{1,2}x\\d{1,3}.*$", "");
        base = base.replaceAll("第\\s*\\d{1,2}\\s*季.*$", "");
        base = base.replaceAll("第\\s*\\d{1,3}\\s*[集话].*$", "");
        base = base.replaceAll("\\[[^\\]]+\\]", " ");
        base = base.replaceAll("\\([^\\)]+\\)", " ");
        base = base.replaceAll("\\s+", " ").trim();
        return base.isEmpty() ? title : base;
    }
    
    private Integer parseSeasonNumber(String fileName) {
        // Pattern 1: S01E01 or S1E1
        Pattern p1 = Pattern.compile("[Ss](\\d{1,2})[Ee]\\d+");
        Matcher m1 = p1.matcher(fileName);
        if (m1.find()) {
            return Integer.parseInt(m1.group(1));
        }
        
        // Pattern 2: 01x01 or 1x01
        Pattern p2 = Pattern.compile("(\\d{1,2})x\\d+", Pattern.CASE_INSENSITIVE);
        Matcher m2 = p2.matcher(fileName);
        if (m2.find()) {
            return Integer.parseInt(m2.group(1));
        }

        Pattern p3 = Pattern.compile("第\\s*(\\d{1,2})\\s*季");
        Matcher m3 = p3.matcher(fileName);
        if (m3.find()) {
            return Integer.parseInt(m3.group(1));
        }
        
        return null;
    }
    
    private Integer parseEpisodeNumber(String fileName) {
        // Pattern 1: S01E01 or S1E1 -> get the E part
        Pattern p1 = Pattern.compile("[Ss]\\d{1,2}[Ee](\\d+)");
        Matcher m1 = p1.matcher(fileName);
        if (m1.find()) {
            return Integer.parseInt(m1.group(1));
        }
        
        // Pattern 2: 01x01 or 1x01 -> get the x part
        Pattern p2 = Pattern.compile("\\d{1,2}x(\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher m2 = p2.matcher(fileName);
        if (m2.find()) {
            return Integer.parseInt(m2.group(1));
        }

        Pattern p3 = Pattern.compile("第\\s*\\d{1,2}\\s*季\\s*第\\s*(\\d{1,3})\\s*[集话]");
        Matcher m3 = p3.matcher(fileName);
        if (m3.find()) {
            return Integer.parseInt(m3.group(1));
        }
        
        return null;
    }
}