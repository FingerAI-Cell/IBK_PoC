package com.ibkpoc.amn.dto;

import lombok.Data;
import java.util.List;

@Data
public class LogUpdateRequest {
    private Long confId;
    private List<LogUpdate> logs;

    @Data
    public static class LogUpdate {
        private Long logId;
        private Long cuserId;  // null 허용
    }
}