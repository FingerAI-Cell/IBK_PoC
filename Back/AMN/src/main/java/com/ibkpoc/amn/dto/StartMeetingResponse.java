package com.ibkpoc.amn.dto;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class StartMeetingResponse {
    private Long convId;
    private Integer userId;
    private String startTime;
}