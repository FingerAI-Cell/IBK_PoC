package com.ibkpoc.amn.controller;

import com.ibkpoc.amn.dto.CommonResponse;
import com.ibkpoc.amn.dto.UserValidationRequest;
import com.ibkpoc.amn.dto.UserValidationResponse;
import com.ibkpoc.amn.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/validate")
    public ResponseEntity<CommonResponse<UserValidationResponse>> alidateUser(@RequestBody UserValidationRequest request) {
        // 요청 Body에서 userId 추출
        int userId = request.getUserId();

        UserValidationResponse response = userService.validateUser(userId);
        if (response.isValid()) {
            return ResponseEntity.ok(new CommonResponse<>("SUCCESS", "User is valid", response));
        }
        return ResponseEntity.ok(new CommonResponse<>("FAILURE", "유효하지 않은 사용자입니다.", response));
    }
}