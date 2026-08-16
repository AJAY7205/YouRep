package com.learning.ytrep.repository;

import java.util.List;

import com.learning.ytrep.model.Video;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoRepository extends JpaRepository<Video,Long> {
    Video findByVideoId(Long videoId);
    List<Video> findByUserUserId(Long userId);
}
