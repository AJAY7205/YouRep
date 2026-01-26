package com.learning.ytrep.controller;

import com.learning.ytrep.payload.VideoDTO;
import com.learning.ytrep.payload.VideoResponse;
import com.learning.ytrep.payload.VideoUploadRequest;
import com.learning.ytrep.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;

import java.io.InputStream;
// import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// import java.util.List;

@RestController
@RequestMapping("/api")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    @Operation(summary = "Post a new video")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            encoding = @Encoding(name = "metadata", contentType = "application/json")
    ))
    @PostMapping(
            value = "/posting-video",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<String> postVideo(@RequestPart("metadata")@Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)) VideoUploadRequest videoUploadRequest,
                                              @RequestPart("file") MultipartFile file,
                                              @RequestPart(value = "thumbnail",required = false) MultipartFile thumbnail){
        VideoDTO videoDTO1 = videoService.postVideo(videoUploadRequest,file,thumbnail);
        return new ResponseEntity<>(videoDTO1.toString(),HttpStatus.CREATED);
    }

    @GetMapping(value = "/getVideo/{videoId}")
    public ResponseEntity<VideoResponse> getVideo(@PathVariable Long videoId){
        VideoResponse get = videoService.getVideo(videoId);
        return new ResponseEntity<>(get,HttpStatus.OK);
    }

    @GetMapping(value = "/videos/{videoId}/stream")
    public ResponseEntity<Resource> streamVideo(@PathVariable Long videoId){
    
        InputStream videoStream = videoService.streamVideo(videoId);
        InputStreamResource resource = new InputStreamResource(videoStream);

    
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("video/mp4"));
        return ResponseEntity.ok()
            .headers(headers)
            .body(resource);
    }
    @Operation(summary = "Update a Video")
    @PutMapping(value = "/update-video/{videoId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VideoResponse> updateVideo(@RequestBody VideoUploadRequest videoUploadRequest,
                                                    @PathVariable Long videoId){
        VideoResponse videoResponse = videoService.updateVideo(videoUploadRequest,videoId);
        return new ResponseEntity<>(videoResponse,HttpStatus.ACCEPTED);
    }

    @Operation(summary = "Delete a Video")
    @DeleteMapping(value = "/delete-video/{videoId}")
    public ResponseEntity<VideoResponse> deleteVideo(@PathVariable Long videoId){
        VideoResponse videoResponse = videoService.deleteVideo(videoId);
        return new ResponseEntity<>(videoResponse,HttpStatus.ACCEPTED);
    }
    
    @Operation(summary = "Get All Video")
    @GetMapping(value = "/get-all-video")
    public ResponseEntity<VideoResponse> getAllVideo(){
        VideoResponse videoResponses = videoService.getAllVideo();
        return new ResponseEntity<>(videoResponses,HttpStatus.OK);
    }


}


