package com.video.service;

import com.video.dto.VideoUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class VideoTranscodeTest {

    @Autowired
    private VideoService videoService;

    @TempDir
    Path tempDir;

    @Test
    public void testUploadEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
            "file", 
            "test.mkv", 
            "video/x-matroska", 
            new byte[0]
        );
        
        assertThrows(RuntimeException.class, () -> {
            videoService.uploadVideo(emptyFile, "Test Video");
        });
    }

    @Test
    public void testUploadNonVideoFile() {
        MockMultipartFile nonVideoFile = new MockMultipartFile(
            "file", 
            "test.txt", 
            "text/plain", 
            "not a video".getBytes()
        );
        
        assertThrows(RuntimeException.class, () -> {
            videoService.uploadVideo(nonVideoFile, "Test");
        });
    }

    @Test
    public void testUploadValidVideo() {
        byte[] fakeVideoData = new byte[1024];
        MockMultipartFile validFile = new MockMultipartFile(
            "file", 
            "test.mkv", 
            "video/x-matroska", 
            fakeVideoData
        );
        
        VideoUploadResponse response = videoService.uploadVideo(validFile, "Test Video");
        
        assertNotNull(response.getUuid());
        assertEquals("pending", response.getStatus());
    }
}