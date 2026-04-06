package com.video.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
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

            try {
                TmdbData tmdb = searchTmdb(query);
                if (tmdb == null) {
                    // Fallback to TV endpoint for episode/series style names
                    TmdbTvData tmdbTv = searchTmdbTv(query);
                    if (tmdbTv != null) {
                        tmdb = new TmdbData();
                        tmdb.setId(tmdbTv.getId());
                        tmdb.setTitle(tmdbTv.getName());
                        tmdb.setOverview(tmdbTv.getOverview());
                        tmdb.setPosterPath(tmdbTv.getPosterPath());
                        tmdb.setReleaseDate(tmdbTv.getFirstAirDate());
                        tmdb.setVoteAverage(tmdbTv.getVoteAverage());
                    }
                }

                result.setTmdb(tmdb);
                result.setDouban(null);
                
                if (tmdb != null) {
                    result.setTitle(tmdb.getTitle());
                    result.setOverview(tmdb.getOverview());
                    result.setPosterUrl(tmdb.getPosterPath() != null ? TMDB_IMAGE_BASE + tmdb.getPosterPath() : null);
                    result.setReleaseDate(tmdb.getReleaseDate());
                    result.setRating(tmdb.getVoteAverage());
                    
                    Map<String, Object> credits = getTmdbMovieCredits(tmdb.getId());
                    if (credits != null) {
                        result.setActors((String) credits.get("actorsStr"));
                        result.setDirector((String) credits.get("director"));
                        @SuppressWarnings("unchecked")
                        List<Actor> actorList = (List<Actor>) credits.get("actors");
                        result.setActorList(actorList);
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
        
        log.info("Searching TMDB Movie for query: {}", query);
        try {
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            String url = String.format("%s/search/movie?api_key=%s&language=%s&query=%s",
                TMDB_BASE, apiKey, getTmdbLanguage(), encodedQuery);

            log.info("Fetching TMDB Movie data from URL: {}", url.replace(apiKey, "HIDDEN_API_KEY"));
            String response = fetch(url);
            if (response == null || response.isEmpty()) {
                log.warn("TMDB Movie search returned empty response for query: {}", query);
                return null;
            }

            JsonNode root = OBJECT_MAPPER.readTree(response);
            JsonNode results = root.path("results");
            if (results.isArray() && results.size() > 0) {
                JsonNode movie = results.get(0);
                TmdbData data = new TmdbData();
                data.setId(movie.path("id").asLong());
                data.setTitle(movie.path("title").asText(null));
                data.setOverview(movie.path("overview").asText(null));
                data.setPosterPath(movie.path("poster_path").asText(null));
                data.setReleaseDate(movie.path("release_date").asText(null));
                data.setVoteAverage(movie.hasNonNull("vote_average") ? movie.path("vote_average").asDouble() : null);

                log.info("Successfully parsed TMDB Movie data: id={}, title={}", data.getId(), data.getTitle());
                return data;
            } else {
                log.warn("TMDB Movie search returned no results in JSON for query: {}", query);
            }
        } catch (Exception e) {
            log.error("Error searching TMDB: {}", query, e);
        }
        return null;
    }
    
    public Map<String, Object> getTmdbMovieCredits(Long movieId) {
        String apiKey = getTmdbApiKey();
        if (apiKey == null || apiKey.isEmpty() || movieId == null) {
            return null;
        }
        
        Map<String, Object> result = new HashMap<>();
        List<Actor> actorsList = new ArrayList<>();
        StringBuilder actorsBuilder = new StringBuilder();
        result.put("actors", actorsList);
        result.put("actorsStr", "");
        result.put("director", "");
        
        try {
            String url = String.format("%s/movie/%d/credits?api_key=%s&language=%s",
                TMDB_BASE, movieId, apiKey, getTmdbLanguage());
            
            log.info("Fetching TMDB Movie credits from URL: {}", url.replace(apiKey, "HIDDEN_API_KEY"));
            String response = fetch(url);
            if (response == null || response.isEmpty()) {
                return result;
            }
            
            JsonNode root = OBJECT_MAPPER.readTree(response);
            
            JsonNode castArray = root.path("cast");
            if (castArray.isArray()) {
                int count = 0;
                for (JsonNode castMember : castArray) {
                    if (count >= 10) break;
                    String name = castMember.path("name").asText(null);
                    if (name != null && !name.isEmpty()) {
                        Actor actor = new Actor();
                        actor.setName(name);
                        actor.setCharacter(castMember.path("character").asText(null));
                        actor.setProfilePath(castMember.path("profile_path").asText(null));
                        actorsList.add(actor);
                        
                        if (actorsBuilder.length() > 0) {
                            actorsBuilder.append(", ");
                        }
                        actorsBuilder.append(name);
                        count++;
                    }
                }
            }
            result.put("actorsStr", actorsBuilder.toString());
            
            JsonNode crewArray = root.path("crew");
            if (crewArray.isArray()) {
                for (JsonNode member : crewArray) {
                    String job = member.path("job").asText(null);
                    if ("Director".equals(job)) {
                        String director = member.path("name").asText(null);
                        if (director != null && !director.isEmpty()) {
                            result.put("director", director);
                            break;
                        }
                    }
                }
            }
            
            log.info("Successfully parsed TMDB Movie credits: actorsCount={}, director={}", 
                actorsList.size(), result.get("director"));
            
        } catch (Exception e) {
            log.error("Error fetching TMDB movie credits: movieId={}", movieId, e);
        }
        
        return result;
    }
    
    public TmdbTvData searchTmdbTv(String query) {
        String apiKey = getTmdbApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("TMDB API key not configured");
            return null;
        }
        log.info("Using TMDB API Key starting with: {}", apiKey.substring(0, Math.min(4, apiKey.length())));
        log.info("Searching TMDB TV for query: {}", query);
        
        try {
            String encodedQuery = URLEncoder.encode(query, "UTF-8");
            String url = String.format("%s/search/tv?api_key=%s&language=%s&query=%s",
                TMDB_BASE, apiKey, getTmdbLanguage(), encodedQuery);
            
            log.info("Fetching TMDB TV data from URL: {}", url.replace(apiKey, "HIDDEN_API_KEY"));
            String response = fetch(url);
            
            if (response == null || response.isEmpty()) {
                log.warn("TMDB TV search returned empty response for query: {}", query);
                return null;
            }
            
            log.debug("TMDB TV response: {}", response);
            JsonNode root = OBJECT_MAPPER.readTree(response);
            JsonNode results = root.path("results");
            if (results.isArray() && results.size() > 0) {
                JsonNode tv = results.get(0);
                TmdbTvData data = new TmdbTvData();
                data.setId(tv.path("id").asLong());
                data.setName(tv.path("name").asText(null));
                data.setOverview(tv.path("overview").asText(null));
                data.setPosterPath(tv.path("poster_path").asText(null));
                data.setFirstAirDate(tv.path("first_air_date").asText(null));
                data.setVoteAverage(tv.hasNonNull("vote_average") ? tv.path("vote_average").asDouble() : null);
                
                log.info("Successfully parsed TMDB TV data: id={}, name={}", data.getId(), data.getName());
                return data;
            } else {
                log.warn("TMDB TV search returned no results in JSON for query: {}", query);
                log.info("Raw JSON response: {}", root);
            }
        } catch (Exception e) {
            log.error("Error searching TMDB TV for query: {}", query, e);
        }
        return null;
    }
    
    public TmdbTvDetailData searchTmdbTvById(Long tvId) {
        String apiKey = getTmdbApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("TMDB API key not configured");
            return null;
        }
        
        try {
            String url = String.format("%s/tv/%d?api_key=%s&language=%s",
                TMDB_BASE, tvId, apiKey, getTmdbLanguage());
            
            log.info("Fetching TMDB TV details from URL: {}", url.replace(apiKey, "HIDDEN_API_KEY"));
            String response = fetch(url);
            
            if (response == null || response.isEmpty()) {
                log.warn("TMDB TV details returned empty response for tvId: {}", tvId);
                return null;
            }
            
            JsonNode root = OBJECT_MAPPER.readTree(response);
            TmdbTvDetailData data = new TmdbTvDetailData();
            data.setId(root.path("id").asLong());
            data.setName(root.path("name").asText(null));
            data.setOverview(root.path("overview").asText(null));
            data.setPosterPath(root.path("poster_path").asText(null));
            data.setNumberOfSeasons(root.path("number_of_seasons").isNull() ? null : root.path("number_of_seasons").asInt());
            
            log.info("Successfully parsed TMDB TV details: id={}, name={}, seasons={}", 
                data.getId(), data.getName(), data.getNumberOfSeasons());
            return data;
        } catch (Exception e) {
            log.error("Error fetching TMDB TV details for tvId: {}", tvId, e);
        }
        return null;
    }
    
    public TmdbSeasonData getTmdbTvSeasonDetails(Long tvId, Integer seasonNumber) {
        String apiKey = getTmdbApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            log.error("TMDB API key is empty or null for getTmdbTvSeasonDetails");
            return null;
        }
        
        String language = getTmdbLanguage();
        String url = String.format("%s/tv/%d/season/%d?api_key=%s&language=%s",
            TMDB_BASE, tvId, seasonNumber, apiKey, language);
        
        log.info("=== getTmdbTvSeasonDetails START ===");
        log.info("tvId={}, seasonNumber={}, language={}", tvId, seasonNumber, language);
        log.info("Request URL: {}", url.replace(apiKey, "***" + apiKey.substring(0, 4) + "***"));
        
        try {
            log.info("Calling fetch() for TMDB season...");
            String response = fetch(url);
            log.info("fetch() returned, response is {}", response == null ? "NULL" : "length=" + response.length());
            
            if (response == null || response.isEmpty()) {
                log.error("Empty or null response from TMDB for tvId={}, season={}", tvId, seasonNumber);
                return null;
            }
            
            log.info("Parsing JSON response with Jackson...");
            JsonNode root = OBJECT_MAPPER.readTree(response);
            log.info("JSON parsed successfully");
            
            TmdbSeasonData data = new TmdbSeasonData();
            data.setId(root.path("id").asLong());
            data.setName(root.path("name").asText(null));
            data.setOverview(root.path("overview").asText(null));
            data.setPosterPath(root.path("poster_path").asText(null));
            data.setSeasonNumber(root.path("season_number").isNull() ? null : root.path("season_number").asInt());
            
            JsonNode episodesNode = root.path("episodes");
            if (episodesNode.isArray()) {
                List<TmdbEpisodeData> episodeList = new ArrayList<>();
                for (JsonNode ep : episodesNode) {
                    TmdbEpisodeData epData = new TmdbEpisodeData();
                    epData.setId(ep.path("id").asLong());
                    epData.setName(ep.path("name").asText(null));
                    epData.setEpisodeNumber(ep.path("episode_number").isNull() ? null : ep.path("episode_number").asInt());
                    epData.setSeasonNumber(ep.path("season_number").isNull() ? null : ep.path("season_number").asInt());
                    epData.setOverview(ep.path("overview").asText(null));
                    epData.setStillPath(ep.path("still_path").asText(null));
                    epData.setAirDate(ep.path("air_date").asText(null));
                    epData.setVoteAverage(ep.path("vote_average").isNull() ? null : ep.path("vote_average").asDouble());
                    episodeList.add(epData);
                }
                data.setEpisodes(episodeList);
                log.info("Parsed {} episodes for season", episodeList.size());
            } else {
                log.warn("No episodes array found in TMDB response for tvId={}, season={}", tvId, seasonNumber);
            }
            
            log.info("=== getTmdbTvSeasonDetails END ===");
            return data;
        } catch (Exception e) {
            log.error("Exception in getTmdbTvSeasonDetails: tvId={}, season={}, error={}", 
                tvId, seasonNumber, e.getMessage(), e);
            log.info("=== getTmdbTvSeasonDetails END WITH ERROR ===");
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
        conn.setConnectTimeout(30000); // 30 seconds
        conn.setReadTimeout(30000); // 30 seconds
        
        int status = conn.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            log.error("HTTP request failed with status: {} for URL: {}", status, urlStr.replaceAll("api_key=[^&]+", "api_key=HIDDEN"));
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream()))) {
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    errorResponse.append(line);
                }
                log.error("Error response body: {}", errorResponse.toString());
            } catch (Exception e) {
                log.error("Could not read error response body", e);
            }
            return null;
        }
        
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
        private String actors;
        private String director;
        private List<Actor> actorList;
        
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
        public String getActors() { return actors; }
        public void setActors(String actors) { this.actors = actors; }
        public String getDirector() { return director; }
        public void setDirector(String director) { this.director = director; }
        public List<Actor> getActorList() { return actorList; }
        public void setActorList(List<Actor> actorList) { this.actorList = actorList; }
    }
    
    public static class Actor {
        private String name;
        private String character;
        private String profilePath;
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCharacter() { return character; }
        public void setCharacter(String character) { this.character = character; }
        public String getProfilePath() { return profilePath; }
        public void setProfilePath(String profilePath) { this.profilePath = profilePath; }
        public String getProfileUrl() {
            return profilePath != null ? TMDB_IMAGE_BASE + profilePath : null;
        }
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
    
    public static class TmdbTvData {
        private Long id;
        private String name;
        private String overview;
        private String posterPath;
        private String firstAirDate;
        private Double voteAverage;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getOverview() { return overview; }
        public void setOverview(String overview) { this.overview = overview; }
        public String getPosterPath() { return posterPath; }
        public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
        public String getFirstAirDate() { return firstAirDate; }
        public void setFirstAirDate(String firstAirDate) { this.firstAirDate = firstAirDate; }
        public Double getVoteAverage() { return voteAverage; }
        public void setVoteAverage(Double voteAverage) { this.voteAverage = voteAverage; }
    }
    
    public static class TmdbTvDetailData {
        private Long id;
        private String name;
        private String overview;
        private String posterPath;
        private Integer numberOfSeasons;
        private List<TmdbSeasonData> seasons;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getOverview() { return overview; }
        public void setOverview(String overview) { this.overview = overview; }
        public String getPosterPath() { return posterPath; }
        public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
        public Integer getNumberOfSeasons() { return numberOfSeasons; }
        public void setNumberOfSeasons(Integer numberOfSeasons) { this.numberOfSeasons = numberOfSeasons; }
        public List<TmdbSeasonData> getSeasons() { return seasons; }
        public void setSeasons(List<TmdbSeasonData> seasons) { this.seasons = seasons; }
    }
    
    public static class TmdbSeasonData {
        private Long id;
        private String name;
        private String overview;
        private String posterPath;
        private Integer seasonNumber;
        private List<TmdbEpisodeData> episodes;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getOverview() { return overview; }
        public void setOverview(String overview) { this.overview = overview; }
        public String getPosterPath() { return posterPath; }
        public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
        public Integer getSeasonNumber() { return seasonNumber; }
        public void setSeasonNumber(Integer seasonNumber) { this.seasonNumber = seasonNumber; }
        public List<TmdbEpisodeData> getEpisodes() { return episodes; }
        public void setEpisodes(List<TmdbEpisodeData> episodes) { this.episodes = episodes; }
    }
    
    public static class TmdbEpisodeData {
        private Long id;
        private String name;
        private Integer episodeNumber;
        private Integer seasonNumber;
        private String overview;
        private String stillPath;
        private String airDate;
        private Double voteAverage;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Integer getEpisodeNumber() { return episodeNumber; }
        public void setEpisodeNumber(Integer episodeNumber) { this.episodeNumber = episodeNumber; }
        public Integer getSeasonNumber() { return seasonNumber; }
        public void setSeasonNumber(Integer seasonNumber) { this.seasonNumber = seasonNumber; }
        public String getOverview() { return overview; }
        public void setOverview(String overview) { this.overview = overview; }
        public String getStillPath() { return stillPath; }
        public void setStillPath(String stillPath) { this.stillPath = stillPath; }
        public String getAirDate() { return airDate; }
        public void setAirDate(String airDate) { this.airDate = airDate; }
        public Double getVoteAverage() { return voteAverage; }
        public void setVoteAverage(Double voteAverage) { this.voteAverage = voteAverage; }
    }
}
