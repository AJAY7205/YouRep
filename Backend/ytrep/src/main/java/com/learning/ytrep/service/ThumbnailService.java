package com.learning.ytrep.service;

import java.lang.module.ResolutionException;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.learning.ytrep.exception.APIException;
import com.learning.ytrep.exception.ResourceNotFoundException;
import com.learning.ytrep.model.Video;
import com.learning.ytrep.repository.VideoRepository;

@Service
public class ThumbnailService {
    private final RedisTemplate<String, byte[]> redisTemplate;
    private final StorageService storageService;
    private final VideoRepository videoRepository;
    private static final String THUMBNAIL_STRING = "thumbnail:";

    public ThumbnailService(RedisTemplate<String,byte[]> redisTemplate,StorageService storageService,VideoRepository videoRepository){
        this.redisTemplate = redisTemplate;
        this.storageService = storageService;
        this.videoRepository = videoRepository;
    }

    public String uploadThumbnail(MultipartFile file){
        return storageService.uploadThumbnail(file);
    }

    public byte[] getThumbnail(Long videoId){
        Video video = videoRepository.findByVideoId(videoId);
        if(video == null){
            throw new ResourceNotFoundException("Video","ID",videoId.toString());
        }
        String thumbnailKey = video.getThumbnailkey();
        if(thumbnailKey == null || thumbnailKey.isBlank()){
            throw new APIException("Thumbnail not found for video ID" + videoId);
        }
        String cacheKey = THUMBNAIL_STRING + videoId;

        byte[] cachedThumbnail = redisTemplate.opsForValue().get(cacheKey);
        if(cachedThumbnail != null){
            return cachedThumbnail;
        }

        byte[] thumbnail = storageService.getThumbnail(thumbnailKey);

        redisTemplate.opsForValue().set(cacheKey, thumbnail,1,TimeUnit.HOURS);

        return thumbnail;
    }

    public void deleteThumbnailCache(Long videoId){
        redisTemplate.delete(THUMBNAIL_STRING + videoId);
    }
}
