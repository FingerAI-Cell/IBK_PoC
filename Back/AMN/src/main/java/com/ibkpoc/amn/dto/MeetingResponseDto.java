package com.ibkpoc.amn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MeetingResponseDto {
    private Long confId;
    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean summarySign;
    private Boolean sttSign;
}