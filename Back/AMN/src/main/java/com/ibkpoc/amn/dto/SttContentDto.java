package com.ibkpoc.amn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SttContentDto {
    private String speaker;
    private String start_time;
    private String end_time;
    private String text;
}