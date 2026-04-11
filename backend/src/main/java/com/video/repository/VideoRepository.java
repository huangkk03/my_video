package com.video.repository;

import com.video.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VideoRepository extends JpaRepository<Video, Long> {
    Optional<Video> findByUuid(String uuid);
    Page<Video> findByStatus(String status, Pageable pageable);
    Page<Video> findBySeriesId(Long seriesId, Pageable pageable);
    Page<Video> findBySeasonId(Long seasonId, Pageable pageable);
    List<Video> findBySeriesIdOrderBySeasonIdAscEpisodeNumberAsc(Long seriesId);
    List<Video> findBySeasonIdOrderByEpisodeNumberAsc(Long seasonId);
    Page<Video> findBySeriesIdIsNullOrderByCreatedAtDesc(Pageable pageable);
    List<Video> findBySeriesIdAndSeasonIdAndEpisodeNumber(Long seriesId, Long seasonId, Integer episodeNumber);
    Page<Video> findByFolderId(Long folderId, Pageable pageable);
    Page<Video> findByFolderIdIsNull(Pageable pageable);
    List<Video> findByFolderId(Long folderId);
    int countByFolderId(Long folderId);
    int countByFolderIdIsNull();
}