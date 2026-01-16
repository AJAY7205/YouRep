package com.learning.ytrep.payload;

import com.learning.ytrep.model.Video;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class VideoResponse {
    private List<VideoDTO> content;

}
