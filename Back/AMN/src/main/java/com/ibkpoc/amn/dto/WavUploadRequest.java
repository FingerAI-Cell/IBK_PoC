package com.ibkpoc.amn.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter @Setter
public class WavUploadRequest {
    private Long meetingId;
    private String startTime;
    private Long duration;
    private Integer currentChunk;
    private Integer totalChunks;
    private MultipartFile file;
}