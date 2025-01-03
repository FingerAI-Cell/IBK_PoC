package com.ibkpoc.amn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SpeakerUpdateDto {
    private String speakerId; // 발화자 ID (예: SPEAKER_00)
    private Long cuserId; // 발화자 cuserId
    private String name; // 새로운 이름
}
