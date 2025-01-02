package com.ibkpoc.amn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpeakerResponseDto {
    private String speakerId; // 발화자 ID (SPEAKER_XX 형식)
    private Long cuserId;     // 사용자 ID
    private String name;      // 사용자 이름 (없으면 speakerId 반환)
}
