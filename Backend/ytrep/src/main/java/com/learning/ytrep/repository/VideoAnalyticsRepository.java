package com.learning.ytrep.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.learning.ytrep.model.VideoAnalytics;

public interface VideoAnalyticsRepository extends JpaRepository<VideoAnalytics,Long> {
        
}
