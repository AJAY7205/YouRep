package com.learning.ytrep.repository;

import com.learning.ytrep.model.Video;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoRepository extends JpaRepository<Video,Long> {
    Video findByVideoId(Long videoId);
    
    // List<Video> findAll();
}
