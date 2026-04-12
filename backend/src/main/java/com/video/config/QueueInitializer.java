package com.video.config;

import com.video.service.DownloadQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueInitializer implements ApplicationRunner {

    private final DownloadQueueService downloadQueueService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting download queue processor...");
        downloadQueueService.processDownloadQueue();
    }
}
