package com.video.dto;

import lombok.Data;

@Data
public class ShareSearchResult {
    private String title;
    private String url;
    private String source;
    private String snippet;

    public ShareSearchResult() {}

    public ShareSearchResult(String title, String url, String source, String snippet) {
        this.title = title;
        this.url = url;
        this.source = source;
        this.snippet = snippet;
    }
}
