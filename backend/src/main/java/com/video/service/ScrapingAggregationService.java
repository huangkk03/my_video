package com.video.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScrapingAggregationService {
    
    private final SystemConfigService systemConfigService;
    
    private static final String TMDB_BASE = "https://api.themoviedb.org/3";
    private static final String TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p/w500";
    private static final String DOUBAN_SEARCH = "https://movie.douban.com/j/search_subjects";
    
    private String getTmdbApiKey() {
        return systemConfigService.getConfig(SystemConfigService.TMDB_API_KEY, "");
    }
    
    private String getTmdbLanguage() {
        return systemConfigService.getConfig(SystemConfigService.TMDB_LANGUAGE, "zh-CN");
    }
    
    public CompletableFuture<MetadataResult> searchMetadata(String query) {
        return CompletableFuture.supplyAsync(() -> {
            MetadataResult result = new MetadataResult();
            result.setQuery(query);
            
            CompletableFuture<TmdbData> tmdbFuture = CompletableFuture.supplyAsync(() -> searchTmdb(query));
            CompletableFuture<DoubanData> doubanFuture = CompletableFuture.supplyAsync(() -> searchDouban(query));
            
            try {
                TmdbData tmdb = tmdbFuture.get();
                DoubanData douban = doubanFuture.get();
                
                result.setTmdb(tmdb);
                result.setDouban(douban);
                
                if (tmdb != null) {
                    result.setTitle(tmdb.getTitle());
                    result.setOverview(tmdb.getOverview());
                    result.setPosterUrl(tmdb.getPosterPath() != null ? TMDB_IMAGE_BASE + tmdb.getPosterPath() : null);
                    result.setReleaseDate(tmdb.getReleaseDate());
                    result.setRating(tmdb.getVoteAverage());
                }
                
                if (douban != null) {
                    if (result.getTitle() == null || result.getTitle().isEmpty()) {
                        result.setTitle(douban.getTitle());
                    }
                    if (result.getOverview() == null || result.getOverview().isEmpty()) {
                        result.setOverview(douban.getDescription());
                    }
                    if (douban.getRating() != null) {
                        result.setRating(douban.getRating());
                    }
                    if (result.getPosterUrl() == null) {
                        result.setPosterUrl(douban.getCoverUrl());
                    }
                }
                
            } catch (Exception e) {
                log.error("Error aggregating metadata for: {}", query, e);
            }
            
            return result;
        });
    }
    
    public TmdbData searchTmdb(String query) {
        String apiKey = getTmdbApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("TMDB API key not configured");
            return null;
        }
        
        try {
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            String url = String.format("%s/search/movie?api_key=%s&language=%s&query=%s",
                TMDB_BASE, apiKey, getTmdbLanguage(), encodedQuery);
            
            String response = fetch(url);
            Map<String, Object> json = parseJsonSimple(response);
            
            List<Map<String, Object>> results = (List<Map<String, Object>>) json.get("results");
            if (results != null && !results.isEmpty()) {
                Map<String, Object> movie = results.get(0);
                
                TmdbData data = new TmdbData();
                data.setId(((Number) movie.get("id")).longValue());
                data.setTitle((String) movie.get("title"));
                data.setOverview((String) movie.get("overview"));
                data.setPosterPath((String) movie.get("poster_path"));
                data.setReleaseDate((String) movie.get("release_date"));
                data.setVoteAverage(movie.get("vote_average") != null ? 
                    ((Number) movie.get("vote_average")).doubleValue() : null);
                
                return data;
            }
        } catch (Exception e) {
            log.error("Error searching TMDB: {}", query, e);
        }
        return null;
    }
    
    public DoubanData searchDouban(String query) {
        try {
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            String url = String.format("%s?tag=%s&type=TV&sort=recommend&page_limit=1",
                DOUBAN_SEARCH, encodedQuery);
            
            String response = fetch(url);
            
            int start = response.indexOf("{\"subjects\":[");
            if (start == -1) {
                return null;
            }
            
            String jsonPart = response.substring(start);
            int endBrace = jsonPart.indexOf("]}");
            if (endBrace != -1) {
                jsonPart = jsonPart.substring(0, endBrace + 2);
            }
            
            Map<String, Object> json = parseJsonSimple(jsonPart);
            List<Map<String, Object>> subjects = (List<Map<String, Object>>) json.get("subjects");
            
            if (subjects != null && !subjects.isEmpty()) {
                Map<String, Object> subject = subjects.get(0);
                
                DoubanData data = new DoubanData();
                data.setId(((Number) subject.get("id")).longValue());
                data.setTitle((String) subject.get("title"));
                data.setCoverUrl((String) subject.get("cover_url"));
                data.setRating(subject.get("rating") != null ? 
                    Double.parseDouble(subject.get("rating").toString()) : null);
                data.setDescription((String) subject.get("info"));
                
                return data;
            }
        } catch (Exception e) {
            log.error("Error searching Douban: {}", query, e);
        }
        return null;
    }
    
    private String fetch(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        
        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
    
    private Map<String, Object> parseJsonSimple(String json) {
        Map<String, Object> result = new HashMap<>();
        try {
            int i = 0;
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '{') {
                    return parseObject(json, new int[]{0});
                }
                i++;
            }
        } catch (Exception e) {
            log.error("JSON parse error", e);
        }
        return result;
    }
    
    private List<Map<String, Object>> parseArray(String json, int[] pos) {
        List<Map<String, Object>> list = new ArrayList<>();
        pos[0]++;
        skipWhitespace(json, pos);
        
        while (pos[0] < json.length() && json.charAt(pos[0]) != ']') {
            skipWhitespace(json, pos);
            char c = json.charAt(pos[0]);
            if (c == '{') {
                list.add(parseObject(json, pos));
            } else if (c == '[') {
                pos[0]++;
                parseArray(json, pos);
            } else if (c == '"') {
                pos[0]++;
                while (pos[0] < json.length() && json.charAt(pos[0]) != '"') {
                    if (json.charAt(pos[0]) == '\\') pos[0]++;
                    pos[0]++;
                }
                pos[0]++;
            }
            skipWhitespace(json, pos);
            if (pos[0] < json.length() && json.charAt(pos[0]) == ',') {
                pos[0]++;
            }
        }
        if (pos[0] < json.length()) pos[0]++;
        return list;
    }
    
    private Map<String, Object> parseObject(String json, int[] pos) {
        Map<String, Object> map = new HashMap<>();
        pos[0]++;
        skipWhitespace(json, pos);
        
        while (pos[0] < json.length() && json.charAt(pos[0]) != '}') {
            skipWhitespace(json, pos);
            if (json.charAt(pos[0]) == '"') {
                String key = parseString(json, pos);
                skipWhitespace(json, pos);
                if (pos[0] < json.length() && json.charAt(pos[0]) == ':') {
                    pos[0]++;
                }
                skipWhitespace(json, pos);
                
                Object value;
                char c = json.charAt(pos[0]);
                if (c == '{') {
                    value = parseObject(json, pos);
                } else if (c == '[') {
                    pos[0]++;
                    value = parseArray(json, pos);
                } else if (c == '"') {
                    value = parseString(json, pos);
                } else if (c == 't' || c == 'f') {
                    value = json.charAt(pos[0]) == 't';
                    pos[0] += json.substring(pos[0]).startsWith("true") ? 4 : 5;
                } else if (c == 'n') {
                    value = null;
                    pos[0] += 4;
                } else {
                    value = parseNumber(json, pos);
                }
                map.put(key, value);
            }
            skipWhitespace(json, pos);
            if (pos[0] < json.length() && json.charAt(pos[0]) == ',') {
                pos[0]++;
            }
        }
        if (pos[0] < json.length()) pos[0]++;
        return map;
    }
    
    private String parseString(String json, int[] pos) {
        pos[0]++;
        StringBuilder sb = new StringBuilder();
        while (pos[0] < json.length() && json.charAt(pos[0]) != '"') {
            char c = json.charAt(pos[0]);
            if (c == '\\') {
                pos[0]++;
                c = json.charAt(pos[0]);
            }
            sb.append(c);
            pos[0]++;
        }
        pos[0]++;
        return sb.toString();
    }
    
    private Number parseNumber(String json, int[] pos) {
        StringBuilder sb = new StringBuilder();
        while (pos[0] < json.length()) {
            char c = json.charAt(pos[0]);
            if ((c >= '0' && c <= '9') || c == '.' || c == '-' || c == 'e' || c == 'E') {
                sb.append(c);
                pos[0]++;
            } else {
                break;
            }
        }
        String num = sb.toString();
        if (num.isEmpty()) return 0;
        return num.contains(".") ? Double.parseDouble(num) : Long.parseLong(num);
    }
    
    private void skipWhitespace(String json, int[] pos) {
        while (pos[0] < json.length() && Character.isWhitespace(json.charAt(pos[0]))) {
            pos[0]++;
        }
    }
    
    public static class MetadataResult {
        private String query;
        private String title;
        private String overview;
        private String posterUrl;
        private String releaseDate;
        private Double rating;
        private TmdbData tmdb;
        private DoubanData douban;
        
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getOverview() { return overview; }
        public void setOverview(String overview) { this.overview = overview; }
        public String getPosterUrl() { return posterUrl; }
        public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }
        public String getReleaseDate() { return releaseDate; }
        public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
        public Double getRating() { return rating; }
        public void setRating(Double rating) { this.rating = rating; }
        public TmdbData getTmdb() { return tmdb; }
        public void setTmdb(TmdbData tmdb) { this.tmdb = tmdb; }
        public DoubanData getDouban() { return douban; }
        public void setDouban(DoubanData douban) { this.douban = douban; }
    }
    
    public static class TmdbData {
        private Long id;
        private String title;
        private String overview;
        private String posterPath;
        private String releaseDate;
        private Double voteAverage;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getOverview() { return overview; }
        public void setOverview(String overview) { this.overview = overview; }
        public String getPosterPath() { return posterPath; }
        public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
        public String getReleaseDate() { return releaseDate; }
        public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
        public Double getVoteAverage() { return voteAverage; }
        public void setVoteAverage(Double voteAverage) { this.voteAverage = voteAverage; }
    }
    
    public static class DoubanData {
        private Long id;
        private String title;
        private String coverUrl;
        private Double rating;
        private String description;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCoverUrl() { return coverUrl; }
        public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
        public Double getRating() { return rating; }
        public void setRating(Double rating) { this.rating = rating; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
}
