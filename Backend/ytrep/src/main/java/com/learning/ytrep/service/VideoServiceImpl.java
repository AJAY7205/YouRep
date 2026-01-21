package com.learning.ytrep.service;

import com.learning.ytrep.exception.APIException;
import com.learning.ytrep.model.Video;
//import com.learning.ytrep.model.VideoAnalytics;
import com.learning.ytrep.model.VideoStatus;
import com.learning.ytrep.payload.VideoDTO;
import com.learning.ytrep.payload.VideoResponse;
import com.learning.ytrep.repository.VideoRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class VideoServiceImpl implements VideoService{

    private final VideoRepository videoRepository;
    private final StorageService storageService;
    private final ModelMapper modelMapper;

    public VideoServiceImpl(VideoRepository videoRepository,StorageService storageService,ModelMapper modelMapper){
        this.videoRepository = videoRepository;
        this.storageService = storageService;
        this.modelMapper = modelMapper;
    }

    @Override
    public VideoDTO postVideo(VideoDTO videoDTO, MultipartFile file){
        Video video = modelMapper.map(videoDTO,Video.class);
//        video.setVideoId(videoDTO.getVideoId());
        video.setVideoId(null);
        video.setTitle(videoDTO.getTitle());
        video.setStatus(VideoStatus.UPLOADED);
        video.setDescription(videoDTO.getDescription());
        video.setCreatedAt(LocalDateTime.now());
        video.setUpdatedAt(LocalDateTime.now());
        String object = storageService.uploadVideo(file);
        video.setObjectKey(object);
        videoRepository.save(video);
        return modelMapper.map(video,VideoDTO.class);
    }

    @Override
    public VideoResponse getVideo(Long videoId){
        Video video = videoRepository.findByVideoId(videoId);
        if(video == null){
            throw new APIException("Video Not Found");
        }
        VideoDTO videoDTO = modelMapper.map(video,VideoDTO.class);
        VideoResponse videoResponse = new VideoResponse();
        videoResponse.setContent(List.of(videoDTO));
        return videoResponse;
    }
}
