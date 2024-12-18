package com.ibkpoc.amn.dto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class StartMeetingRequest {
    private String startTime; // "yyyy-MM-dd HH:mm:ss" 형식
    @JsonProperty("participantCount") // JSON 필드명에 맞게 매핑
    private Integer participants; // 참가자 수 추가
}