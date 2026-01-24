package com.learning.ytrep.payload;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoAnalyticsResponse {
    List<VideoAnalyticsDTO> content;
}
