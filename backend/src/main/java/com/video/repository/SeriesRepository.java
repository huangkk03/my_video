package com.video.repository;

import com.video.entity.Series;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeriesRepository extends JpaRepository<Series, Long> {
    Optional<Series> findBySlug(String slug);
    
    Optional<Series> findByTmdbId(Long tmdbId);
    
    List<Series> findByCategoryIdOrderBySortOrder(Long categoryId);
    
    List<Series> findAllByOrderBySortOrder();
    
    @Query("SELECT s FROM Series s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Series> findByNameContaining(@Param("name") String name);
    
    @Query("SELECT COUNT(v) FROM Video v WHERE v.seriesId = :seriesId")
    Long countVideosBySeriesId(@Param("seriesId") Long seriesId);
}
