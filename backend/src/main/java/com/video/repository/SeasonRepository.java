package com.video.repository;

import com.video.entity.Season;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeasonRepository extends JpaRepository<Season, Long> {
    List<Season> findBySeriesIdOrderBySeasonNumber(Long seriesId);
    
    Optional<Season> findBySeriesIdAndSeasonNumber(Long seriesId, Integer seasonNumber);
    
    Optional<Season> findByTmdbId(Long tmdbId);
    
    long countBySeriesId(Long seriesId);
}
