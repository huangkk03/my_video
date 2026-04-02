package com.video.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatchAssignRequest {
    private List<String> videoUuids;
    private Long seriesId;
    private Long seasonId;
    private Integer episodeStart;
}
