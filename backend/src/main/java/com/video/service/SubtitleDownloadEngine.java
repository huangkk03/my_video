package com.video.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;

@Slf4j
@Service
public class SubtitleDownloadEngine {

    private static final String PYTHON_SCRIPT = "/app/scripts/download_subtitles.py";

    public boolean downloadSubtitle(String imdbId, String title, String language, String outputDir) {
        log.info("Attempting to download subtitle: imdbId={}, title={}, language={}", imdbId, title, language);

        // Try Python subliminal script first
        if (downloadWithPython(imdbId, title, language, outputDir)) {
            return true;
        }

        log.warn("Python download failed for imdbId={}, language={}", imdbId, language);
        return false;
    }

    private boolean downloadWithPython(String imdbId, String title, String language, String outputDir) {
        log.info("Trying Python subliminal script for imdbId={}, language={}", imdbId, language);

        Path scriptPath = Paths.get(PYTHON_SCRIPT);
        if (!Files.exists(scriptPath)) {
            log.warn("Python script not found at: {}", scriptPath);
            return false;
        }

        try {
            List<String> command = Arrays.asList(
                "python3",
                scriptPath.toString(),
                "--imdb-id", imdbId,
                "--title", title,
                "--languages", language,
                "--output-dir", outputDir
            );

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Read output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            log.info("Python script exit code: {}, output: {}", exitCode, output);

            if (exitCode == 0) {
                // Check if subtitle file was created
                Path vttFile = Paths.get(outputDir, language + ".vtt");
                return Files.exists(vttFile);
            }

            return false;
        } catch (Exception e) {
            log.error("Error running Python script: {}", e.getMessage());
            return false;
        }
    }

    public List<String> downloadAllLanguages(String imdbId, String title, List<String> languages, String outputDir) {
        List<String> downloaded = new ArrayList<>();

        for (String language : languages) {
            if (downloadSubtitle(imdbId, title, language, outputDir)) {
                downloaded.add(language);
            }
        }

        return downloaded;
    }
}
