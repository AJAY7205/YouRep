package com.learning.ytrep.service;


import com.learning.ytrep.payload.VideoDTO;
import com.learning.ytrep.payload.VideoResponse;
import com.learning.ytrep.payload.VideoUploadRequest;

import java.io.InputStream;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public interface VideoService {

    VideoDTO postVideo(VideoUploadRequest videoUploadRequest, MultipartFile file,MultipartFile thumbnail);

    VideoResponse getVideo(Long videoId);

    InputStream streamVideo(Long videoId);

    long getVideoSize(Long videoId);

    VideoResponse updateVideo(VideoUploadRequest videoUploadRequest, Long videoId);

    VideoResponse getAllVideo();

    VideoResponse deleteVideo(Long videoId);
}
