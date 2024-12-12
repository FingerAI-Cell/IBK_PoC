package com.ibkpoc.amn.dto;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EndMeetingRequest {
    private Long meetingId;
}