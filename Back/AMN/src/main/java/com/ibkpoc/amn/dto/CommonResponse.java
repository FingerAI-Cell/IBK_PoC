// 앱을 위한 백엔드 코드
package com.ibkpoc.amn.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class CommonResponse<T> {
    private String status;
    private String message;
    private T data;

    // Constructor, getters, setters
}

