package com.video.service;

import com.video.entity.Season;
import com.video.entity.Series;
import com.video.entity.Video;
import com.video.repository.SeasonRepository;
import com.video.repository.SeriesRepository;
import com.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeriesService {
    
    private final SeriesRepository seriesRepository;
    private final SeasonRepository seasonRepository;
    private final VideoRepository videoRepository;
    
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
    
    @Transactional
    public Series createSeries(Series series) {
        if (series.getSlug() == null || series.getSlug().isEmpty()) {
            series.setSlug(generateSlug(series.getName()));
        }
        return seriesRepository.save(series);
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
    
    private String generateSlug(String name) {
        if (name == null) return "";
        return name.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "-")
                   .toLowerCase()
                   .replaceAll("-+", "-")
                   .replaceAll("^-|-$", "");
    }
}
