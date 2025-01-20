package com.ibkpoc.amn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor // 기본 생성자 추가 (필수)
public class MeetingSummaryResponseDto {
    private String summary;
}