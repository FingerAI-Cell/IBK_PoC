package com.ibkpoc.amn.dto;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class StartMeetingRequest {
    private String startTime; // "yyyy-MM-dd HH:mm:ss" 형식
}