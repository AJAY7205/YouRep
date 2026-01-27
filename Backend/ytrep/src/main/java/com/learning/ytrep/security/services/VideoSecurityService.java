package com.learning.ytrep.security.services;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import com.learning.ytrep.model.Video;
import com.learning.ytrep.repository.VideoRepository;

@Service("videoSecurityService")
public class VideoSecurityService {
    
    private final VideoRepository videoRepository;

    public VideoSecurityService(VideoRepository videoRepository){
        this.videoRepository = videoRepository;
    }

    public boolean isVideoOwner(Authentication authentication,Long videoId){
        if(authentication == null || !authentication.isAuthenticated()){
            return false;
        }

        String username = authentication.getName();
        Video video = videoRepository.findByVideoId(videoId);

        if(video == null || video.getUser()==null){
            return false;
        }

        return video.getUser().getUsername().equals(username);
    }
}
