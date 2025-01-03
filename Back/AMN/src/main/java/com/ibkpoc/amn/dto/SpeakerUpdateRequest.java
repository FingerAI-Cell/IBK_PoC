package com.ibkpoc.amn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SpeakerUpdateRequest {
    private Long confId; // 회의 ID
    private List<SpeakerUpdateDto> speakers; // 발화자 업데이트 리스트
}

