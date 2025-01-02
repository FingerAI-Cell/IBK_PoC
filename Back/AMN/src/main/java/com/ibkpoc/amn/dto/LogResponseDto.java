package com.ibkpoc.amn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LogResponseDto {
    private String content;   // 발화 내용
    private Long cuserId;     // 사용자 ID
    private LocalDateTime startTime;
}
