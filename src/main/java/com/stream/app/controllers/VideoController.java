package com.stream.app.controllers;

import com.stream.app.AppConstants;
import com.stream.app.entities.Video;
import com.stream.app.payload.ErrorMessage;
import com.stream.app.service.VideoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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

    @GetMapping("/stream/range/{videoId}")
    public ResponseEntity<Resource> streamVideoRange(
            @PathVariable String videoId,
            @RequestHeader(value = "Range", required = false) String range
    ) {
        System.out.println("The Range: " + range);

        Video video = videoService.get(videoId);
        Path path = Paths.get(video.getFilePath());

        Resource resource = new FileSystemResource(path);

        String contentType = video.getContentType();

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        // Length of the file
        long fileLength = path.toFile().length();

        if (range == null) {
            return ResponseEntity
                    .ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        }

        // Calculating start & end ranges
        long rangeStart;
        long rangeEnd;

        System.out.println("Range before split --> " + range);
        String[] ranges = range.replace("bytes=", "").split("-");

        System.out.println("Ranges after --> " + Arrays.toString(ranges));
        rangeStart = Long.parseLong(ranges[0]);

        rangeEnd = rangeStart + AppConstants.CHUNK_SIZE -1;

        if(rangeEnd >= fileLength){
            rangeEnd = fileLength -1;
        }


        // Check if the range includes an end
//        if (ranges.length > 1 && !ranges[1].isEmpty()) {
//            rangeEnd = Long.parseLong(ranges[1]);
//        } else {
//            rangeEnd = fileLength - 1; // If no end specified, go until the end of the file
//        }
//
//        // Adjust if rangeEnd exceeds fileLength
//        if (rangeEnd > fileLength - 1) {
//            rangeEnd = fileLength - 1;
//        }

        System.out.println("Range Start: " + rangeStart);
        System.out.println("Range End: " + rangeEnd);

        InputStream inputStream;
        try {
            inputStream = Files.newInputStream(path);
            inputStream.skip(rangeStart);
            long contentLength = rangeEnd - rangeStart + 1;

            byte[] data = new byte[(int) contentLength];
            int read = inputStream.read(data,0, data.length);
            System.out.println("read(number of bytes) : " +read);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Range", "bytes " + rangeStart + "-" + rangeEnd + "/" + fileLength);
            headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
            headers.add("Pragma", "no-cache");
            headers.add("Expires", "0");
            headers.add("X-Content-Type-Options", "nosniff");

            headers.setContentLength(contentLength);

            return ResponseEntity
                    .status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new ByteArrayResource(data));

        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }


    }

    @Value("${files.video.hls}")
    private String HLS_DIR;

    @GetMapping("/{videoId}/master.m3u8")
    public ResponseEntity<Resource> streamMaster(
            @PathVariable String videoId
    ){
        Path path = Paths.get(HLS_DIR,videoId,"master.m3u8");

        System.out.println("The path->" + path);

        if(!Files.exists(path)){
            return  new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Resource resource = new FileSystemResource(path);

        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl")
                .body(resource);
    }

}
