package com.stream.app.service.impl;

import com.stream.app.entities.Video;
import com.stream.app.repositories.VideoRepository;
import com.stream.app.service.VideoService;
import jakarta.annotation.PostConstruct;
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
    }

    @Override
    public Video save(Video video, MultipartFile file) {


        try {
            String fileName = file.getOriginalFilename();
            String contentType = file.getContentType();
            InputStream inputStream = file.getInputStream();


            //file path
            String cleanFileName = StringUtils.cleanPath(fileName);

            //folder path
            String cleanFolder = StringUtils.cleanPath(DIR);

            //folder path with file name
            Path path = Paths.get(cleanFolder, cleanFileName);

            System.out.println("The path" + path);

            //copy file to folder
            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);

            //video meta data
            video.setContentType(contentType);
            video.setFilePath(path.toString());

            return videoRepository.save(video);
            //metadata save
        } catch(IOException e){
            e.printStackTrace();
            return  null;
        }

    }

    @Override
    public Video get(String videoId) {
        return null;
    }

    @Override
    public Video getByTitle(String title) {
        return null;
    }

    @Override
    public List<Video> getAll() {
        return List.of();
    }
}
