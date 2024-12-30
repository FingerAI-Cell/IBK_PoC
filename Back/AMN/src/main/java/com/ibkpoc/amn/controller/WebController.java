package com.ibkpoc.amn.controller;

import com.ibkpoc.amn.dto.*;
import com.ibkpoc.amn.service.WebService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class WebController {

    private final WebService webService;

    @GetMapping("/")
    public ResponseEntity<List<MeetingResponseDto>> getMeetings() {
        log.info("회의록 목록 조회 요청");
        List<MeetingResponseDto> meetings = webService.getAllMeetings();
        return ResponseEntity.ok(meetings);
    }

    @PostMapping("/stt")
    public ResponseEntity<ApiResponse<List<SttContentDto>>> getSttContent(@RequestBody SttRequestDto request) {
        try {
            List<SttContentDto> content = webService.readSttContent(request.getSttSrc());
            return ResponseEntity.ok(ApiResponse.success(content));
        } catch (FileNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("파일을 찾을 수 없습니다."));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("파일 처리 중 오류가 발생했습니다."));
        }
    }
}