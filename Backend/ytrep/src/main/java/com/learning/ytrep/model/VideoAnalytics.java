package com.learning.ytrep.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "video_analytics")
public class VideoAnalytics {

    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private Long analyticsId;

    @Column(nullable = false)
    private long viewCount;

    @Column(nullable = false)
    private long likeCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
