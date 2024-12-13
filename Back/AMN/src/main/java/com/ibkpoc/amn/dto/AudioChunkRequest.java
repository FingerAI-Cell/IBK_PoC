package com.ibkpoc.amn.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

// dto/AudioChunkRequest.java
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class AudioChunkRequest {
    private Long meetingId;
    private Long chunkStartTime;
    private Long duration;
    private MultipartFile file;
}