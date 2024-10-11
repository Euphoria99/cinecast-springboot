package com.stream.app.service.impl;

import com.stream.app.entities.Video;
import com.stream.app.repositories.VideoRepository;
import com.stream.app.service.VideoService;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Service
public class VideoServiceImpl implements VideoService {

    @Value("${files.video}")
    String DIR;

    @Value("${files.video.hls}")
    String HLS_DIR;

    private VideoRepository videoRepository;

    public VideoServiceImpl(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @PostConstruct
    public void init(){

        File file = new File(DIR);

        if(!file.exists()){
                file.mkdir();
            System.out.println("Folder Created!");
        }else {
            System.out.println("Folder exists!");
        }

        File  hls_file = new File(HLS_DIR);


        try {
            Files.createDirectories(Paths.get(HLS_DIR));
            System.out.println("HLS folder created");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    @Transactional
    public Video save(Video video, MultipartFile file) {
        Path path = null;
        try {
            // File upload logic
            String fileName = file.getOriginalFilename();
            String contentType = file.getContentType();
            InputStream inputStream = file.getInputStream();

            // Clean file path and folder name
            String cleanFileName = StringUtils.cleanPath(fileName);
            String cleanFolder = StringUtils.cleanPath(DIR);

            // Folder path with file name
            path = Paths.get(cleanFolder, cleanFileName);

            System.out.println("The path: " + path);

            // Copy file to folder
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);

            // Set video metadata
            video.setContentType(contentType);
            video.setFilePath(path.toString());

            videoRepository.save(video);

            // Process the video (this could throw an exception)
            processVideo(video.getVideoId());

            // Save metadata only after successful processing
            return video;
        } catch (IOException | RuntimeException e) {
            // Handle exception, roll back, and delete the partially saved file if needed
            if (path != null) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
            throw new RuntimeException("Video processing failed. Metadata not saved.", e);
        }
    }


    @Override
    public Video get(String videoId) {
        Video video = videoRepository.findById(videoId).orElseThrow(() -> new RuntimeException("video not found"));
        return video;
    }

    @Override
    public Video getByTitle(String title) {
        return null;
    }

    @Override
    public List<Video> getAll() {
        return videoRepository.findAll();
    }

    @Override
    public String processVideo(String videoId) {
        Video video = this.get(videoId);
        String filePath = video.getFilePath();

        Path videoPath = Paths.get(filePath);

        try {
            Path outputPath = Paths.get(HLS_DIR, videoId);
            Files.createDirectories(outputPath);

            // Define qualities and their resolutions
            String[][] qualities = {
                    {"360p", "640x360", "800000"},
                    {"480p", "854x480", "1400000"},
                    {"720p", "1280x720", "2800000"},
                    {"1080p", "1920x1080", "5000000"}
            };

            // Create a master playlist command
            StringBuilder masterPlaylistCmd = new StringBuilder("#EXTM3U\n");

            // Build the FFmpeg commands for each quality
            for (String[] quality : qualities) {
                String qualityName = quality[0];
                String resolution = quality[1];
                String bandwidth = quality[2];
                String segmentFileName = String.format("segment_%s_%%03d.ts", qualityName);
                String playlistFileName = String.format("%s.m3u8", qualityName);

                // Command to generate HLS for the current quality
                String ffmpegCmd = String.format(
                        "ffmpeg -i \"%s\" -vf \"scale=%s\" -c:v libx264 -c:a aac -strict -2 -f hls " +
                                "-hls_time 10 -hls_list_size 0 -hls_segment_filename \"%s/%s\" \"%s/%s\"",
                        videoPath, resolution, outputPath, segmentFileName, outputPath, playlistFileName
                );

                // Append to the master playlist content
                masterPlaylistCmd.append(String.format("#EXT-X-STREAM-INF:BANDWIDTH=%s,RESOLUTION=%s\n", bandwidth, resolution));
                masterPlaylistCmd.append(playlistFileName).append("\n");

                // Execute the FFmpeg command
                ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", ffmpegCmd);
                processBuilder.inheritIO();
                Process process = processBuilder.start();
                int exit = process.waitFor();
                if (exit != 0) {
                    throw new RuntimeException("Video processing failed for quality " + qualityName);
                }
            }

            // Write the master playlist file
            Path masterPlaylistPath = outputPath.resolve("master.m3u8");
            Files.write(masterPlaylistPath, masterPlaylistCmd.toString().getBytes());

            return videoId;

        } catch (IOException ex) {
            throw new RuntimeException("Video processing failed!", ex);
        } catch (InterruptedException e) {
            throw new RuntimeException("Video processing interrupted!", e);
        }
    }
}
