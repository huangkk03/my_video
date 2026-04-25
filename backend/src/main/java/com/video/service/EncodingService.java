package com.video.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class EncodingService {

    private static final String FFMPEG_COMMAND = "ffmpeg";

    public String detectCharset(File file) {
        try (InputStream is = new FileInputStream(file)) {
            byte[] buffer = new byte[4096];
            int len = is.read(buffer);
            if (len > 0) {
                org.mozilla.universalchardet.UniversalDetector detector =
                    new org.mozilla.universalchardet.UniversalDetector(null);
                detector.handleData(buffer, 0, len);
                detector.dataEnd();
                String encoding = detector.getDetectedCharset();
                detector.reset();
                if (encoding != null) {
                    return encoding;
                }
            }
        } catch (Exception e) {
            log.warn("Charset detection failed for {}, defaulting to UTF-8", file.getName(), e);
        }
        return "UTF-8";
    }

    public void convertToUtf8Vtt(File sourceSub, File targetVtt) throws Exception {
        String detectedCharset = detectCharset(sourceSub);
        log.info("Detected charset for {}: {}", sourceSub.getName(), detectedCharset);

        if ("UTF-8".equalsIgnoreCase(detectedCharset) || "ASCII".equalsIgnoreCase(detectedCharset)) {
            convertToVtt(sourceSub, targetVtt);
        } else {
            File utf8Temp = convertToUtf8(sourceSub, detectedCharset);
            try {
                convertToVtt(utf8Temp, targetVtt);
            } finally {
                if (utf8Temp.exists()) {
                    utf8Temp.delete();
                }
            }
        }
    }

    private File convertToUtf8(File source, String sourceCharset) throws IOException {
        File tempFile = File.createTempFile("subtitle_utf8_", ".tmp");
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(source), Charset.forName(sourceCharset)));
             BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(tempFile), Charset.forName("UTF-8")))) {
            char[] buffer = new char[8192];
            int len;
            while ((len = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, len);
            }
        }
        log.info("Converted {} from {} to UTF-8", source.getName(), sourceCharset);
        return tempFile;
    }

    public void convertToVtt(File srtFile, File vttFile) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
            FFMPEG_COMMAND,
            "-i", srtFile.getAbsolutePath(),
            "-y",
            "-f", "webvtt",
            vttFile.getAbsolutePath()
        );
        pb.redirectErrorStream(true);

        log.info("Converting {} to VTT: {}", srtFile.getName(), vttFile.getName());
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg: {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg VTT conversion failed with exit code: " + exitCode);
        }

        if (!vttFile.exists() || vttFile.length() == 0) {
            throw new RuntimeException("VTT file was not created: " + vttFile.getAbsolutePath());
        }

        log.info("Successfully converted {} to VTT: {} bytes", vttFile.getName(), vttFile.length());
    }

    public void convertSrtToVttContent(InputStream srtInput, OutputStream vttOutput, String charset) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(srtInput, Charset.forName(charset)));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(vttOutput, Charset.forName("UTF-8")));

        writer.write("WEBVTT\n\n");

        String line;
        int count = 0;
        StringBuilder buffer = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) {
                if (buffer.length() > 0) {
                    count++;
                    writer.write(count + "\n");
                    writer.write(buffer.toString().replace("\r\n", "\n").replace("\n\n", "\n"));
                    writer.write("\n\n");
                    buffer.setLength(0);
                }
            } else {
                if (buffer.length() > 0) {
                    buffer.append("\n");
                }
                if (line.matches("\\d{2}:\\d{2}:\\d{2}[,.]\\d{3}\\s*-->\\s*\\d{2}:\\d{2}:\\d{2}[,.]\\d{3}")) {
                    line = line.replace(",", ".");
                }
                buffer.append(line);
            }
        }

        if (buffer.length() > 0) {
            count++;
            writer.write(count + "\n");
            writer.write(buffer.toString().replace("\r\n", "\n").replace("\n\n", "\n"));
            writer.write("\n\n");
        }

        writer.flush();
        log.info("Converted SRT to VTT content, {} entries", count);
    }
}