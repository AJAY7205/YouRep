package com.learning.ytrep.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
// import java.util.UUID;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Video {
    @Id
    @GeneratedValue(strategy =  GenerationType.IDENTITY)
    private Long videoId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String objectKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VideoStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "video", cascade = CascadeType.ALL,fetch =  FetchType.LAZY,orphanRemoval = true)
    private VideoAnalytics videoAnalytics;

}
