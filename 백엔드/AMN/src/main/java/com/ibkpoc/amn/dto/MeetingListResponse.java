package com.ibkpoc.amn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// MeetingListResponse.java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetingListResponse {
    private Long id;
    private String title;
    private LocalDateTime start;
    private LocalDateTime end;
    private Integer participant;
    private String topic;
}