package com.learning.ytrep.repository;

import com.learning.ytrep.model.Video;
import org.springframework.data.jpa.repository.JpaRepository;


public interface VideoRepository extends JpaRepository<Video,Long> {
    Video findByVideoId(Long videoId);
}
