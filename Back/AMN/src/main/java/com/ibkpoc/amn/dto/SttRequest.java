package com.ibkpoc.amn.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SttRequest {
    private Long meetingId; // 요청받은 회의 ID
}
