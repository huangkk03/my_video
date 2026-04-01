package com.video.controller;

import com.video.entity.Season;
import com.video.entity.Series;
import com.video.entity.Video;
import com.video.service.SeriesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/series")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SeriesController {
    
    private final SeriesService seriesService;
    
    @GetMapping
    public ResponseEntity<List<Series>> getAllSeries() {
        List<Series> seriesList = seriesService.getAllSeries();
        return ResponseEntity.ok(seriesList);
    }
    
    @GetMapping("/page")
    public ResponseEntity<Map<String, Object>> getSeriesPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Series> seriesPage = seriesService.getSeriesPage(page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("content", seriesPage.getContent());
        result.put("totalElements", seriesPage.getTotalElements());
        result.put("totalPages", seriesPage.getTotalPages());
        result.put("number", seriesPage.getNumber());
        result.put("size", seriesPage.getSize());
        return ResponseEntity.ok(result);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getSeriesDetail(@PathVariable Long id) {
        Series series = seriesService.getSeriesById(id).orElse(null);
        if (series == null) {
            return ResponseEntity.notFound().build();
        }
        List<Season> seasons = seriesService.getSeasonsBySeriesId(id);
        List<Video> videos = seriesService.getVideosBySeriesId(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("series", series);
        result.put("seasons", seasons);
        result.put("videos", videos);
        result.put("videoCount", videos.size());
        result.put("seasonCount", seasons.size());
        
        return ResponseEntity.ok(result);
    }
    
    @PostMapping
    public ResponseEntity<Series> createSeries(@RequestBody Series series) {
        Series created = seriesService.createSeries(series);
        return ResponseEntity.ok(created);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Series> updateSeries(@PathVariable Long id, @RequestBody Series series) {
        Series updated = seriesService.updateSeries(id, series);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }
    
    @PostMapping("/{id}/rescrap")
    public ResponseEntity<Series> rescrapSeries(@PathVariable Long id) {
        Series updated = seriesService.rescrapSeries(id);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSeries(@PathVariable Long id) {
        seriesService.deleteSeries(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<Series>> searchSeries(@RequestParam String name) {
        List<Series> results = seriesService.searchByName(name);
        return ResponseEntity.ok(results);
    }
    
    @GetMapping("/{id}/seasons")
    public ResponseEntity<List<Season>> getSeasons(@PathVariable Long id) {
        List<Season> seasons = seriesService.getSeasonsBySeriesId(id);
        return ResponseEntity.ok(seasons);
    }
    
    @PostMapping("/{id}/seasons")
    public ResponseEntity<Season> createSeason(@PathVariable Long id, @RequestBody Season season) {
        try {
            Season created = seriesService.createSeason(id, season);
            return ResponseEntity.ok(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @GetMapping("/{id}/videos")
    public ResponseEntity<List<Video>> getSeriesVideos(@PathVariable Long id) {
        List<Video> videos = seriesService.getVideosBySeriesId(id);
        return ResponseEntity.ok(videos);
    }
    
    @GetMapping("/unassigned")
    public ResponseEntity<Map<String, Object>> getUnassignedVideos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Video> videoPage = seriesService.getUnassignedVideos(page, size);
        Map<String, Object> result = new HashMap<>();
        result.put("content", videoPage.getContent());
        result.put("totalElements", videoPage.getTotalElements());
        result.put("totalPages", videoPage.getTotalPages());
        return ResponseEntity.ok(result);
    }
}
