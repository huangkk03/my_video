package com.video.controller;

import com.video.entity.Season;
import com.video.entity.Video;
import com.video.service.SeriesService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/seasons")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SeasonController {
    
    private final SeriesService seriesService;
    
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getSeasonDetail(@PathVariable Long id) {
        Season season = seriesService.getSeasonById(id).orElse(null);
        if (season == null) {
            return ResponseEntity.notFound().build();
        }
        List<Video> videos = seriesService.getVideosBySeasonId(id);
        
        Map<String, Object> result = new HashMap<>();
        result.put("season", season);
        result.put("videos", videos);
        result.put("videoCount", videos.size());
        
        return ResponseEntity.ok(result);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Season> updateSeason(@PathVariable Long id, @RequestBody Season season) {
        Season updated = seriesService.updateSeason(id, season);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSeason(@PathVariable Long id) {
        seriesService.deleteSeason(id);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{id}/videos")
    public ResponseEntity<List<Video>> getSeasonVideos(@PathVariable Long id) {
        List<Video> videos = seriesService.getVideosBySeasonId(id);
        return ResponseEntity.ok(videos);
    }
}
