package com.learning.ytrep.controller;

import com.learning.ytrep.config.Idempotent;
import com.learning.ytrep.payload.VideoDTO;
import com.learning.ytrep.payload.VideoResponse;
import com.learning.ytrep.payload.VideoUploadRequest;
import com.learning.ytrep.service.VideoService;
import com.learning.ytrep.service.VideoStreamInfo;
import com.learning.ytrep.service.TranscodeProgressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;

// import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// import java.util.List;

@RestController
@RequestMapping("/api")
public class VideoController {

    private final VideoService videoService;
    private final TranscodeProgressService progressService;

    public VideoController(VideoService videoService, TranscodeProgressService progressService) {
        this.videoService = videoService;
        this.progressService = progressService;
    }

    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("API is healthy");
    }
    @Operation(summary = "Post a new video USER/ADMIN only")
    @Idempotent
    @PreAuthorize("hasAuthority('ADMIN') or (hasAuthority('USER') and @verificationSecurityService.isVerified(authentication))")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @Content(
            encoding = @Encoding(name = "metadata", contentType = "application/json")
    ))
    @PostMapping(
            value = "/posting-video",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<String> postVideo(@RequestPart("metadata")@Parameter(content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)) VideoUploadRequest videoUploadRequest,
                                              @RequestPart("file") MultipartFile file,
                                              @RequestPart(value = "thumbnail",required = false) MultipartFile thumbnail,
                                              Authentication authentication){
        String username = authentication.getName();
        VideoDTO videoDTO1 = videoService.postVideo(videoUploadRequest,file,thumbnail,username);
        return new ResponseEntity<>(videoDTO1.toString(),HttpStatus.ACCEPTED);
    }

    @Operation(summary = "Get video details (ALL type of Users)")
    @GetMapping(value = "/getVideo/{videoId}")
    public ResponseEntity<VideoResponse> getVideo(@PathVariable Long videoId){
        VideoResponse get = videoService.getVideo(videoId);
        return new ResponseEntity<>(get,HttpStatus.OK);
    }

    @Operation(summary = "Stream video (ALL type of Users) with HTTP Range support")
    @GetMapping(value = "/videos/{videoId}/stream")
    public ResponseEntity<Resource> streamVideo(
            @PathVariable Long videoId,
            @RequestHeader(value = "Range", required = false) String rangeHeader){

        if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {
            String[] ranges = rangeHeader.substring(6).split("-");
            long start;
            try {
                start = Long.parseLong(ranges[0]);
            } catch (NumberFormatException e) {
                start = 0;
            }
            long requestedEnd = (ranges.length > 1 && !ranges[1].isEmpty())
                    ? Long.parseLong(ranges[1])
                    : -1;

            VideoStreamInfo info = videoService.getVideoStreamInfo(videoId, start, requestedEnd);

            if (start >= info.totalSize()) {
                HttpHeaders headers = new HttpHeaders();
                headers.set("Content-Range", "bytes */" + info.totalSize());
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .headers(headers).build();
            }

            InputStreamResource resource = new InputStreamResource(info.stream());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("video/mp4"));
            headers.setContentLength(info.contentLength());
            headers.set("Content-Range", "bytes " + info.start() + "-" + info.end() + "/" + info.totalSize());
            headers.set("Accept-Ranges", "bytes");

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(headers)
                    .body(resource);
        }

        VideoStreamInfo info = videoService.getVideoStreamInfo(videoId, 0, -1);
        InputStreamResource resource = new InputStreamResource(info.stream());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("video/mp4"));
        headers.setContentLength(info.totalSize());
        headers.set("Accept-Ranges", "bytes");
        return ResponseEntity.ok()
            .headers(headers)
            .body(resource);
    }
    @Operation(summary = "Update a Video(Owner or ADMIN only)")
    @PreAuthorize("hasAuthority('ADMIN') or @videoSecurityService.isVideoOwner(authentication, #videoId)")
    @PutMapping(value = "/update-video/{videoId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VideoResponse> updateVideo(@RequestBody VideoUploadRequest videoUploadRequest,
                                                    @PathVariable Long videoId,
                                                    Authentication authentication){
        VideoResponse videoResponse = videoService.updateVideo(videoUploadRequest,videoId);
        return new ResponseEntity<>(videoResponse,HttpStatus.ACCEPTED);
    }

    @Operation(summary = "Delete a Video(OWNER or ADMIN only)")
    @PreAuthorize("hasAuthority('ADMIN') or @videoSecurityService.isVideoOwner(authentication, #videoId)")
    @DeleteMapping(value = "/delete-video/{videoId}")
    public ResponseEntity<VideoResponse> deleteVideo(@PathVariable Long videoId,Authentication authentication){
        VideoResponse videoResponse = videoService.deleteVideo(videoId);
        return new ResponseEntity<>(videoResponse,HttpStatus.ACCEPTED);
    }
    
    @Operation(summary = "Get All Video(All users including GUEST)")
    @GetMapping(value = "/get-all-video")
    public ResponseEntity<VideoResponse> getAllVideo(){
        VideoResponse videoResponses = videoService.getAllVideo();
        return new ResponseEntity<>(videoResponses,HttpStatus.OK);
    }

    @Operation(summary = "Get live transcode progress + ETA (All users)")
    @GetMapping(value = "/videos/{videoId}/transcode-progress")
    public ResponseEntity<java.util.Map<String, Object>> getTranscodeProgress(@PathVariable Long videoId){
        return new ResponseEntity<>(progressService.getProgress(videoId), HttpStatus.OK);
    }


}


