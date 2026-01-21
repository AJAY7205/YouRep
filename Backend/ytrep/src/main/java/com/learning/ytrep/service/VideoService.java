package com.learning.ytrep.service;


import com.learning.ytrep.payload.VideoDTO;
import com.learning.ytrep.payload.VideoResponse;
import org.springframework.web.multipart.MultipartFile;

public interface VideoService {

    VideoDTO postVideo(VideoDTO videoDTO, MultipartFile file);

    VideoResponse getVideo(Long videoId);
}
