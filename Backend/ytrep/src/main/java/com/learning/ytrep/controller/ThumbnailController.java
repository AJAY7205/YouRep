package com.learning.ytrep.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.learning.ytrep.service.ThumbnailService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api")
public class ThumbnailController {

    private final ThumbnailService thumbnailService;

    public ThumbnailController(ThumbnailService thumbnailService){
        this.thumbnailService = thumbnailService;
    }
    
    @Operation(summary = "Get video thumbnail")
    @GetMapping("/videos/{videoId}/thumbnail")
    public ResponseEntity<byte[]> getThumbnail(@PathVariable Long videoId){
        byte[] thumbnail = thumbnailService.getThumbnail(videoId);
        if(thumbnail == null){
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.ALL)
                .body(thumbnail);
    }
}
