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

        try{
            Path outputPath= Paths.get(HLS_DIR,videoId);
            Files.createDirectories(outputPath);

            String ffmpegCmd = String.format(
                    "ffmpeg -i \"%s\" -c:v libx264 -c:a aac -strict -2 -f hls -hls_time 10 -hls_list_size 0 -hls_segment_filename \"%s/segment_%%3d.ts\"  \"%s/master.m3u8\" ",
                    videoPath, outputPath, outputPath
            );

            System.out.println(ffmpegCmd);
            //file this command
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", ffmpegCmd);
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            int exit = process.waitFor();
            if (exit != 0) {
                throw new RuntimeException("video processing failed!!");
            }

            return videoId;

        } catch (IOException ex) {
            throw new RuntimeException("Video processing fail!!");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
