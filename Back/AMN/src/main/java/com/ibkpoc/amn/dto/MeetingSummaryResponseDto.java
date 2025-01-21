package com.ibkpoc.amn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MeetingSummaryResponseDto {
    private List<TopicDetail> topics; // 여러 주제와 스피커 정보를 담은 리스트

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TopicDetail {
        private String topic; // 주제
        private List<SpeakerDetail> speakers; // 주제에 해당하는 스피커 목록
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SpeakerDetail {
        private String name;    // 발화자 이름
        private String content; // 발언 내용
    }
}
