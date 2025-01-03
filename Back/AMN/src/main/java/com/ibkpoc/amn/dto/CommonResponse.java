// 앱을 위한 백엔드 코드
package com.ibkpoc.amn.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
public class CommonResponse<T> { // 앱의 요청에 대한 응답
    private String status;
    private String message;
    private T data;

    // Constructor, getters, setters
}

