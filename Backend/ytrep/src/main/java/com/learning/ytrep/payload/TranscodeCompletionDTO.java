package com.learning.ytrep.payload;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TranscodeCompletionDTO {

    private Long videoId;
    private String status;
    private String outputKey;
    private String error;
}
