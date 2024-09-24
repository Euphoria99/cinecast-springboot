package com.stream.app.service;

import com.stream.app.entities.Video;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface VideoService {

    //save
    Video save(Video video, MultipartFile file);


    //get video by id
    Video get(String videoId);

    //get video by title
    Video getByTitle(String title);

    //get all video
    List<Video> getAll();
}
