package com.video.service;

import com.video.entity.Season;
import com.video.entity.Series;
import com.video.entity.Video;
import com.video.repository.SeasonRepository;
import com.video.repository.SeriesRepository;
import com.video.repository.VideoRepository;
import com.video.service.ScrapingAggregationService.TmdbData;
import com.video.service.ScrapingAggregationService.TmdbEpisodeData;
import com.video.service.ScrapingAggregationService.TmdbSeasonData;
import com.video.service.ScrapingAggregationService.TmdbTvData;
import com.video.service.ScrapingAggregationService.TmdbTvDetailData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeriesService {
    
    private final SeriesRepository seriesRepository;
    private final SeasonRepository seasonRepository;
    private final VideoRepository videoRepository;
    private final ScrapingAggregationService scrapingAggregationService;
    
    public List<Series> getAllSeries() {
        return seriesRepository.findAllByOrderBySortOrder();
    }
    
    public Page<Series> getSeriesPage(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return seriesRepository.findAll(pageable);
    }
    
    public Optional<Series> getSeriesById(Long id) {
        return seriesRepository.findById(id);
    }
    
    public Series getSeriesDetail(Long id) {
        Series series = seriesRepository.findById(id).orElse(null);
        return series;
    }
    
    public Series createSeries(Series series) {
        if (series.getSlug() == null || series.getSlug().isEmpty()) {
            series.setSlug(generateSlug(series.getName()));
        }
        
        // Auto-scrape metadata
        if (series.getName() != null && !series.getName().isEmpty()) {
            log.info("Starting auto-scrape for new series: {}", series.getName());
            try {
                TmdbTvData tmdbData = scrapingAggregationService.searchTmdbTv(series.getName());
                if (tmdbData != null) {
                    log.info("Auto-scrape successful (TV) for series: {}, tmdbId: {}", series.getName(), tmdbData.getId());
                    if (series.getTmdbId() == null) {
                        series.setTmdbId(tmdbData.getId());
                    }
                    if (series.getOverview() == null || series.getOverview().isEmpty()) {
                        series.setOverview(tmdbData.getOverview());
                    }
                    if (series.getPosterPath() == null || series.getPosterPath().isEmpty()) {
                        if (tmdbData.getPosterPath() != null) {
                            series.setPosterPath("https://image.tmdb.org/t/p/w500" + tmdbData.getPosterPath());
                        }
                    }
                } else {
                    log.info("Auto-scrape (TV) returned no results, falling back to Movie search for: {}", series.getName());
                    TmdbData movieData = scrapingAggregationService.searchTmdb(series.getName());
                    if (movieData != null) {
                        log.info("Auto-scrape successful (Movie) for series: {}, tmdbId: {}", series.getName(), movieData.getId());
                        if (series.getTmdbId() == null) {
                            series.setTmdbId(movieData.getId());
                        }
                        if (series.getOverview() == null || series.getOverview().isEmpty()) {
                            series.setOverview(movieData.getOverview());
                        }
                        if (series.getPosterPath() == null || series.getPosterPath().isEmpty()) {
                            if (movieData.getPosterPath() != null) {
                                series.setPosterPath("https://image.tmdb.org/t/p/w500" + movieData.getPosterPath());
                            }
                        }
                    } else {
                        log.warn("Auto-scrape returned no results (TV or Movie) for series: {}", series.getName());
                    }
                }
            } catch (Exception e) {
                log.error("Failed to auto-scrape metadata for series: {}", series.getName(), e);
            }
        }
        
        log.info("Persisting series to database: name={}, tmdbId={}", series.getName(), series.getTmdbId());
        Series saved = seriesRepository.save(series);
        log.info("Series persisted successfully: id={}, name={}", saved.getId(), saved.getName());
        return saved;
    }
    
    @Transactional
    public Series updateSeries(Long id, Series updatedSeries) {
        Series series = seriesRepository.findById(id).orElse(null);
        if (series == null) {
            return null;
        }
        if (updatedSeries.getName() != null) {
            series.setName(updatedSeries.getName());
        }
        if (updatedSeries.getSlug() != null) {
            series.setSlug(updatedSeries.getSlug());
        }
        if (updatedSeries.getPosterPath() != null) {
            series.setPosterPath(updatedSeries.getPosterPath());
        }
        if (updatedSeries.getBackdropPath() != null) {
            series.setBackdropPath(updatedSeries.getBackdropPath());
        }
        if (updatedSeries.getOverview() != null) {
            series.setOverview(updatedSeries.getOverview());
        }
        if (updatedSeries.getTmdbId() != null) {
            series.setTmdbId(updatedSeries.getTmdbId());
        }
        if (updatedSeries.getCategoryId() != null) {
            series.setCategoryId(updatedSeries.getCategoryId());
        }
        if (updatedSeries.getSortOrder() != null) {
            series.setSortOrder(updatedSeries.getSortOrder());
        }
        return seriesRepository.save(series);
    }
    
    public Series rescrapSeries(Long id) {
        Series series = seriesRepository.findById(id).orElse(null);
        if (series == null) {
            return null;
        }
        
        log.info("Starting manual scrape for series: {}", series.getName());
        try {
            TmdbTvData tmdbData = scrapingAggregationService.searchTmdbTv(series.getName());
            if (tmdbData != null) {
                log.info("Manual scrape successful (TV) for series: {}, tmdbId: {}", series.getName(), tmdbData.getId());
                series.setTmdbId(tmdbData.getId());
                series.setOverview(tmdbData.getOverview());
                if (tmdbData.getPosterPath() != null) {
                    series.setPosterPath("https://image.tmdb.org/t/p/w500" + tmdbData.getPosterPath());
                }
                log.info("Persisting rescraped series (TV) to database: id={}, name={}", series.getId(), series.getName());
                Series saved = seriesRepository.save(series);
                log.info("Rescraped series persisted successfully: id={}, name={}", saved.getId(), saved.getName());
                return saved;
            } else {
                log.info("Manual scrape (TV) returned no results, falling back to Movie search for: {}", series.getName());
                TmdbData movieData = scrapingAggregationService.searchTmdb(series.getName());
                if (movieData != null) {
                    log.info("Manual scrape successful (Movie) for series: {}, tmdbId: {}", series.getName(), movieData.getId());
                    series.setTmdbId(movieData.getId());
                    series.setOverview(movieData.getOverview());
                    if (movieData.getPosterPath() != null) {
                        series.setPosterPath("https://image.tmdb.org/t/p/w500" + movieData.getPosterPath());
                    }
                    log.info("Persisting rescraped series (Movie fallback) to database: id={}, name={}", series.getId(), series.getName());
                    Series saved = seriesRepository.save(series);
                    log.info("Rescraped series persisted successfully: id={}, name={}", saved.getId(), saved.getName());
                    return saved;
                } else {
                    log.warn("Manual scrape returned no results (TV or Movie) for series: {}", series.getName());
                }
            }
        } catch (Exception e) {
            log.error("Failed to manual scrape metadata for series: {}", series.getName(), e);
        }
        return series;
    }
    
    @Transactional
    public void deleteSeries(Long id) {
        Series series = seriesRepository.findById(id).orElse(null);
        if (series != null) {
            List<Video> videos = videoRepository.findBySeriesIdOrderBySeasonIdAscEpisodeNumberAsc(id);
            for (Video video : videos) {
                video.setSeriesId(null);
                video.setSeasonId(null);
                video.setEpisodeNumber(null);
                videoRepository.save(video);
            }
            seriesRepository.delete(series);
        }
    }
    
    public List<Series> searchByName(String name) {
        return seriesRepository.findByNameContaining(name);
    }
    
    public Optional<Series> findByTmdbId(Long tmdbId) {
        return seriesRepository.findByTmdbId(tmdbId);
    }
    
    public Optional<Series> findBySlug(String slug) {
        return seriesRepository.findBySlug(slug);
    }
    
    public List<Season> getSeasonsBySeriesId(Long seriesId) {
        return seasonRepository.findBySeriesIdOrderBySeasonNumber(seriesId);
    }
    
    @Transactional
    public Season createSeason(Long seriesId, Season season) {
        Series series = seriesRepository.findById(seriesId).orElse(null);
        if (series == null) {
            throw new RuntimeException("Series not found");
        }
        season.setSeriesId(seriesId);
        if (season.getName() == null || season.getName().isEmpty()) {
            season.setName("第 " + season.getSeasonNumber() + " 季");
        }
        return seasonRepository.save(season);
    }
    
    @Transactional
    public Season updateSeason(Long id, Season updatedSeason) {
        Season season = seasonRepository.findById(id).orElse(null);
        if (season == null) {
            return null;
        }
        if (updatedSeason.getSeasonNumber() != null) {
            season.setSeasonNumber(updatedSeason.getSeasonNumber());
        }
        if (updatedSeason.getName() != null) {
            season.setName(updatedSeason.getName());
        }
        if (updatedSeason.getPosterPath() != null) {
            season.setPosterPath(updatedSeason.getPosterPath());
        }
        if (updatedSeason.getOverview() != null) {
            season.setOverview(updatedSeason.getOverview());
        }
        if (updatedSeason.getTmdbId() != null) {
            season.setTmdbId(updatedSeason.getTmdbId());
        }
        return seasonRepository.save(season);
    }
    
    @Transactional
    public void deleteSeason(Long id) {
        Season season = seasonRepository.findById(id).orElse(null);
        if (season != null) {
            List<Video> videos = videoRepository.findBySeasonIdOrderByEpisodeNumberAsc(id);
            for (Video video : videos) {
                video.setSeasonId(null);
                video.setEpisodeNumber(null);
                videoRepository.save(video);
            }
            seasonRepository.delete(season);
        }
    }
    
    public Optional<Season> getSeasonById(Long id) {
        return seasonRepository.findById(id);
    }
    
    public Optional<Season> findSeasonBySeriesIdAndSeasonNumber(Long seriesId, Integer seasonNumber) {
        return seasonRepository.findBySeriesIdAndSeasonNumber(seriesId, seasonNumber);
    }
    
    public List<Video> getVideosBySeriesId(Long seriesId) {
        return videoRepository.findBySeriesIdOrderBySeasonIdAscEpisodeNumberAsc(seriesId);
    }
    
    public List<Video> getVideosBySeasonId(Long seasonId) {
        return videoRepository.findBySeasonIdOrderByEpisodeNumberAsc(seasonId);
    }
    
    public Page<Video> getUnassignedVideos(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return videoRepository.findBySeriesIdIsNullOrderByCreatedAtDesc(pageable);
    }
    
    @Transactional
    public void assignVideoToSeason(String videoUuid, Long seasonId, Integer episodeNumber) {
        Video video = videoRepository.findByUuid(videoUuid).orElse(null);
        if (video != null) {
            Season season = seasonRepository.findById(seasonId).orElse(null);
            if (season != null) {
                video.setSeriesId(season.getSeriesId());
                video.setSeasonId(seasonId);
                video.setEpisodeNumber(episodeNumber);
                videoRepository.save(video);
            }
        }
    }
    
    @Transactional
    public void removeVideoFromSeries(String videoUuid) {
        Video video = videoRepository.findByUuid(videoUuid).orElse(null);
        if (video != null) {
            video.setSeriesId(null);
            video.setSeasonId(null);
            video.setEpisodeNumber(null);
            videoRepository.save(video);
        }
    }

    @Transactional
    public int batchAssignVideos(List<String> videoUuids, Long seriesId, Long seasonId, Integer episodeStart) {
        if (videoUuids == null || videoUuids.isEmpty()) {
            throw new RuntimeException("videoUuids 不能为空");
        }
        Series series = seriesRepository.findById(seriesId).orElseThrow(() -> new RuntimeException("系列不存在"));

        Season season = null;
        if (seasonId != null) {
            season = seasonRepository.findById(seasonId).orElseThrow(() -> new RuntimeException("季度不存在"));
            if (!series.getId().equals(season.getSeriesId())) {
                throw new RuntimeException("季度不属于该系列");
            }
        }

        int assigned = 0;
        int nextEpisode = episodeStart != null && episodeStart > 0 ? episodeStart : -1;
        for (String uuid : videoUuids) {
            if (uuid == null || uuid.trim().isEmpty()) {
                continue;
            }
            Optional<Video> videoOpt = videoRepository.findByUuid(uuid.trim());
            if (!videoOpt.isPresent()) {
                continue;
            }
            Video video = videoOpt.get();
            video.setSeriesId(series.getId());
            video.setSeasonId(season != null ? season.getId() : null);
            if (nextEpisode > 0) {
                video.setEpisodeNumber(nextEpisode++);
            }
            videoRepository.save(video);
            
            assigned++;
        }
        return assigned;
    }
    
    private String generateSlug(String name) {
        if (name == null) return "";
        return name.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "-")
                   .toLowerCase()
                   .replaceAll("-+", "-")
                   .replaceAll("^-|-$", "");
    }
    
    public Map<String, Object> scrapeSeasonFromTmdb(Long seriesId, Integer seasonNumber) {
        Map<String, Object> result = new HashMap<>();
        
        Series series = seriesRepository.findById(seriesId).orElse(null);
        if (series == null) {
            result.put("success", false);
            result.put("message", "Series not found");
            return result;
        }
        
        if (series.getTmdbId() == null) {
            result.put("success", false);
            result.put("message", "Series has no TMDB ID");
            return result;
        }
        
        log.info("Scraping season {} from TMDB for series: {} (tmdbId: {})", seasonNumber, series.getName(), series.getTmdbId());
        
        try {
            TmdbSeasonData seasonData = scrapingAggregationService.getTmdbTvSeasonDetails(series.getTmdbId(), seasonNumber);
            if (seasonData == null) {
                result.put("success", false);
                result.put("message", "Failed to fetch season data from TMDB");
                return result;
            }
            
            Season season = createOrUpdateSeason(seriesId, seasonData);
            List<Video> episodes = createSeasonEpisodes(season, seasonData.getEpisodes());
            
            log.info("Successfully scraped season {} with {} episodes for series: {}", seasonNumber, episodes.size(), series.getName());
            
            result.put("success", true);
            result.put("season", season);
            result.put("episodes", episodes);
            result.put("episodeCount", episodes.size());
            
            return result;
        } catch (Exception e) {
            log.error("Error scraping season from TMDB: seriesId={}, seasonNumber={}", seriesId, seasonNumber, e);
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
            return result;
        }
    }
    
    public Map<String, Object> scrapeAllSeasonsFromTmdb(Long seriesId) {
        Map<String, Object> result = new HashMap<>();
        
        Series series = seriesRepository.findById(seriesId).orElse(null);
        if (series == null || series.getTmdbId() == null) {
            result.put("success", false);
            result.put("message", "Series not found or has no TMDB ID");
            return result;
        }
        
        log.info("Scraping all seasons from TMDB for series: {} (tmdbId: {})", series.getName(), series.getTmdbId());
        
        try {
            TmdbTvDetailData tvData = scrapingAggregationService.searchTmdbTvById(series.getTmdbId());
            if (tvData == null || tvData.getNumberOfSeasons() == null) {
                result.put("success", false);
                result.put("message", "Failed to fetch TV details from TMDB");
                return result;
            }
            
            List<Map<String, Object>> scrapedSeasons = new ArrayList<>();
            int totalSeasons = tvData.getNumberOfSeasons();
            
            for (int i = 1; i <= totalSeasons; i++) {
                Map<String, Object> seasonResult = scrapeSeasonFromTmdb(seriesId, i);
                scrapedSeasons.add(seasonResult);
            }
            
            result.put("success", true);
            result.put("totalSeasons", totalSeasons);
            result.put("seasons", scrapedSeasons);
            
            return result;
        } catch (Exception e) {
            log.error("Error scraping all seasons from TMDB: seriesId={}", seriesId, e);
            result.put("success", false);
            result.put("message", "Error: " + e.getMessage());
            return result;
        }
    }
    
    private Season createOrUpdateSeason(Long seriesId, TmdbSeasonData data) {
        Season season = null;
        
        Optional<Season> existingOpt = seasonRepository.findBySeriesIdAndSeasonNumber(seriesId, data.getSeasonNumber());
        if (existingOpt.isPresent()) {
            season = existingOpt.get();
            log.info("Updating existing season: {} (id: {})", data.getName(), season.getId());
        } else {
            season = new Season();
            season.setSeriesId(seriesId);
            log.info("Creating new season: {}", data.getName());
        }
        
        season.setSeasonNumber(data.getSeasonNumber());
        season.setName(data.getName());
        season.setOverview(data.getOverview());
        season.setTmdbId(data.getId());
        
        if (data.getPosterPath() != null) {
            season.setPosterPath("https://image.tmdb.org/t/p/w500" + data.getPosterPath());
        }
        
        return seasonRepository.save(season);
    }
    
    private List<Video> createSeasonEpisodes(Season season, List<TmdbEpisodeData> tmdbEpisodes) {
        List<Video> videos = new ArrayList<>();
        
        if (tmdbEpisodes == null || tmdbEpisodes.isEmpty()) {
            return videos;
        }
        
        for (TmdbEpisodeData epData : tmdbEpisodes) {
            // 查找该集是否已有真实视频（remote_alist）
            List<Video> existing = videoRepository.findBySeriesIdAndSeasonIdAndEpisodeNumber(
                season.getSeriesId(), season.getId(), epData.getEpisodeNumber());
            
            // 筛选出真实视频（remote_alist 有 hls_path）
            Video existingVideo = null;
            for (Video v : existing) {
                if (v.getHlsPath() != null && !v.getHlsPath().isEmpty()) {
                    existingVideo = v;
                    break;
                }
            }
            
            if (existingVideo != null) {
                // 更新已有真实视频的元信息
                log.info("Updating metadata for episode {} - videoId={}, title={}", 
                    epData.getEpisodeNumber(), existingVideo.getUuid(), epData.getName());
                
                existingVideo.setTitle(epData.getName());
                existingVideo.setOverview(epData.getOverview());
                if (epData.getStillPath() != null) {
                    existingVideo.setThumbnailPath("https://image.tmdb.org/t/p/w500" + epData.getStillPath());
                }
                existingVideo.setScrapingStatus("scraped");
                
                videos.add(videoRepository.save(existingVideo));
            } else {
                // 没有对应真实视频，跳过（不再创建刮削数据）
                log.info("No real video found for episode {} in seasonId={}, skipping", 
                    epData.getEpisodeNumber(), season.getId());
            }
        }
        
        return videos;
    }
}
