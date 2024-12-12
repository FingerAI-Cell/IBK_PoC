package com.ibkpoc.amn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserValidationResponse {
    private int userId;
    private boolean isValid;
    private boolean isActive; // 사용자 active 여부
    private boolean isMeetingActive; // 미팅 active 여부
    // Constructor, getters, setters
}
