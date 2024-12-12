// 앱을 위한 백엔드 코드
package com.ibkpoc.amn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommonResponse<T> {
    private String status;
    private String message;
    private T data;

    // Constructor, getters, setters
}

