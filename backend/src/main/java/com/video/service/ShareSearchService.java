package com.video.service;

import com.video.dto.ShareSearchResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
public class ShareSearchService {

    private static final String BING_SEARCH_URL = "https://www.bing.com/search";
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final Pattern LINK_PATTERN = Pattern.compile("href=\"([^\"]*aliyundrive\\.com[^\"]*)\"[^>]*>([^<]*)</a>", Pattern.CASE_INSENSITIVE);
    private static final Pattern TITLE_PATTERN = Pattern.compile("<h2[^>]*>[^<]*<a[^>]*>([^<]*)</a>", Pattern.CASE_INSENSITIVE);

    public List<ShareSearchResult> searchAliyunShare(String keyword, int limit) {
        List<ShareSearchResult> results = new ArrayList<>();

        if (keyword == null || keyword.trim().isEmpty()) {
            return results;
        }

        try {
            String searchUrl = buildSearchUrl(keyword);
            String htmlContent = fetchHtmlContent(searchUrl);
            results = parseSearchResults(htmlContent, limit);

            log.info("Found {} results for keyword: {}", results.size(), keyword);
        } catch (Exception e) {
            log.error("Search failed for keyword: {}", keyword, e);
        }

        return results;
    }

    private String buildSearchUrl(String keyword) {
        try {
            String encodedKeyword = URLEncoder.encode("site:aliyundrive.com " + keyword, StandardCharsets.UTF_8.toString());
            return BING_SEARCH_URL + "?q=" + encodedKeyword + "&first=0&count=50";
        } catch (Exception e) {
            log.error("Failed to build search URL", e);
            return BING_SEARCH_URL + "?q=" + keyword;
        }
    }

    private String fetchHtmlContent(String searchUrl) throws Exception {
        URL url = new URL(searchUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);

        StringBuilder html = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                html.append(line).append("\n");
            }
        }

        return html.toString();
    }

    private List<ShareSearchResult> parseSearchResults(String htmlContent, int limit) {
        List<ShareSearchResult> results = new ArrayList<>();

        Pattern resultPattern = Pattern.compile("<li[^>]*class=\"[^\"]*b_algo[^\"]*\"[^>]*>(.*?)</li>", Pattern.DOTALL);
        Matcher resultMatcher = resultPattern.matcher(htmlContent);

        while (resultMatcher.find() && results.size() < limit) {
            String resultBlock = resultMatcher.group(1);

            String url = extractUrl(resultBlock);
            String title = extractTitle(resultBlock);

            if (url != null && title != null && !title.isEmpty()) {
                String snippet = extractSnippet(resultBlock);
                ShareSearchResult result = new ShareSearchResult(
                        cleanHtml(title),
                        url,
                        "aliyundrive.com",
                        cleanHtml(snippet)
                );
                results.add(result);
            }
        }

        return results;
    }

    private String extractUrl(String block) {
        Pattern pattern = Pattern.compile("href=\"([^\"]*aliyundrive\\.com[^\"]*)\"");
        Matcher matcher = pattern.matcher(block);
        if (matcher.find()) {
            String url = matcher.group(1);
            return url.replaceAll("&amp;", "&");
        }
        return null;
    }

    private String extractTitle(String block) {
        Pattern pattern = Pattern.compile("<h2[^>]*>.*?<a[^>]*>([^<]*)</a>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(block);
        if (matcher.find()) {
            return matcher.group(1);
        }

        Pattern altPattern = Pattern.compile("<a[^>]*href=\"[^\"]*aliyundrive[^\"]*\"[^>]*>([^<]*)</a>", Pattern.CASE_INSENSITIVE);
        Matcher altMatcher = altPattern.matcher(block);
        if (altMatcher.find()) {
            return altMatcher.group(1);
        }

        return null;
    }

    private String extractSnippet(String block) {
        Pattern pattern = Pattern.compile("<p[^>]*>([^<]*)</p>");
        Matcher matcher = pattern.matcher(block);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String cleanHtml(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]*>", "")
                   .replaceAll("&nbsp;", " ")
                   .replaceAll("&amp;", "&")
                   .replaceAll("&lt;", "<")
                   .replaceAll("&gt;", ">")
                   .replaceAll("&quot;", "\"")
                   .trim();
    }
}
