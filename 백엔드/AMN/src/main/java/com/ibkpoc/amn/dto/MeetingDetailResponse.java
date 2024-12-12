package com.ibkpoc.amn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// MeetingDetailResponse.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetingDetailResponse {
    private String summary;
    private String participant;
}