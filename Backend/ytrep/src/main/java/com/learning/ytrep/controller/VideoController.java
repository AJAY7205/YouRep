package com.learning.ytrep.controller;

import com.learning.ytrep.payload.VideoDTO;
import com.learning.ytrep.payload.VideoResponse;
import com.learning.ytrep.service.VideoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;

import java.io.InputStream;

import org.simpleframework.xml.Path;
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
    public ResponseEntity<String> postVideo(@RequestPart("metadata")@Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)) VideoDTO videoDTO,
                                              @RequestPart("file") MultipartFile file){
        VideoDTO videoDTO1 = videoService.postVideo(videoDTO,file);
        return new ResponseEntity<>(videoDTO1.toString(),HttpStatus.CREATED);
    }

    @GetMapping(value = "/getVideo/{videoId}")
    public ResponseEntity<VideoResponse> getVideo(@PathVariable Long videoId){
        VideoResponse get = videoService.getVideo(videoId);
        return new ResponseEntity<>(get,HttpStatus.OK);
    }

    @GetMapping(value = "/videos/{videoId}/stream")
    public ResponseEntity<Resource> streamVideo(@PathVariable Long videoId, @RequestHeader(value = "Range", required = false) String range){
        InputStream videoStream = videoService.streamVideo(videoId);
        InputStreamResource resource = new InputStreamResource(videoStream);

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
            .header(HttpHeaders.CONTENT_TYPE, "video/mp4")
            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
            .body(resource);
    }
}


