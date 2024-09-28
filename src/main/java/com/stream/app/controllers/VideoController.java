package com.stream.app.controllers;

import com.stream.app.entities.Video;
import com.stream.app.payload.ErrorMessage;
import com.stream.app.service.VideoService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/videos")
public class VideoController {

    private  VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    //upload video
    @PostMapping("/upload")
    public ResponseEntity<?> create(
            @RequestParam("file")MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description
    ){

        Video video = new Video();
        video.setVideoId(UUID.randomUUID().toString());
        video.setTitle(title);
        video.setDescription(description);


        Video savedVideo =  videoService.save(video, file);

       if(savedVideo !=null){
           return ResponseEntity
                   .status(HttpStatus.OK)
                   .body(video);
       }else {
           return ResponseEntity
                   .status(HttpStatus.INTERNAL_SERVER_ERROR)
                   .body(ErrorMessage
                           .builder()
                           .message("Video not Upload")
                           .success(false)
                           .build());
       }
    }

    //stream video
    @GetMapping("/stream/{videoId}")
    public ResponseEntity<Resource> stream(
            @PathVariable String videoId
    ){
            Video video = videoService.get(videoId);

            String contentType = video.getContentType();
            String filePath = video.getFilePath();
            Resource resource = new FileSystemResource(filePath);

            if(contentType == null){
                contentType = "application/octet-stream";
            }

            return  ResponseEntity
                    .ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
    }

    //get all video

    @GetMapping("/list-videos")
    public List<Video> getAll(){
        return videoService.getAll();
    }
}
