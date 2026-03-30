package com.video.dto;

import lombok.Data;

@Data
public class VideoUploadResponse {
    private String uuid;
    private String status;
    private String message;
    
    public static VideoUploadResponse of(String uuid, String message) {
        VideoUploadResponse response = new VideoUploadResponse();
        response.setUuid(uuid);
        response.setStatus("pending");
        response.setMessage(message);
        return response;
    }
}