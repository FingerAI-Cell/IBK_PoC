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
    private OverallSummary overall;
    private SpeakerSummary speaker;

    // 전체 요약 (Overall Summary)
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OverallSummary {
        private List<OverallTopicDetail> topics;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class OverallTopicDetail {
        private String topic;
        private String content; // "content" 추가됨
    }

    // 화자별 요약 (Speaker Summary)
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SpeakerSummary {
        private List<SpeakerTopicDetail> topics;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SpeakerTopicDetail {
        private String topic;
        private List<SpeakerDetail> speakers;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SpeakerDetail {
        private String name;
        private String content;
    }
}
