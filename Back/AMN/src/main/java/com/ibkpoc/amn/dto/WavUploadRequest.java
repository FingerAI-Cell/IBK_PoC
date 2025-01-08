package com.ibkpoc.amn.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WavUploadRequest {
    private Long meetingId;       // 회의 ID
    private Integer sectionNumber; // 섹션 번호
    private String startTime;      // 시작 시간
    private MultipartFile chunkData; // WAV 청크 데이터
    private Integer totalChunks; // 총 청크 수
    private Integer currentChunk;

    public byte[] getChunkDataBytes() throws IOException {
        return chunkData != null ? chunkData.getBytes() : null;
    }
}
