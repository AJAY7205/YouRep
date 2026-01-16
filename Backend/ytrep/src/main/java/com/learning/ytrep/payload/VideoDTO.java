package com.learning.ytrep.payload;

import com.learning.ytrep.model.VideoStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoDTO {
    private Long videoId;
    private String videoName;
    private String videoDescription;
//    private String videoUrl;
    private VideoStatus videoStatus;
    private LocalDateTime videoCreateTime;
    private LocalDateTime videoUpdateTime;
}
