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
import java.util.Map;

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

    @PostMapping("/summarize")
    public ResponseEntity<ApiResponse<String>> summarizeMeeting(@RequestParam Long confId) {
        try {
            String summary = webService.generateSummary(confId);
            return ResponseEntity.ok(ApiResponse.success(summary));
        } catch (FileNotFoundException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("STT 파일을 찾을 수 없습니다."));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("요약 생성 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/speakers")
    public ResponseEntity<ApiResponse<List<SpeakerResponseDto>>> getSpeakers(@RequestBody SttRequest request) {
        try {
            Long confId = request.getMeetingId(); // 요청받은 회의 ID
            List<SpeakerResponseDto> speakers = webService.getSpeakers(confId);
            return ResponseEntity.ok(ApiResponse.success(speakers));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("발화자 설정 조회 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/logs")
    public ResponseEntity<ApiResponse<List<LogResponseDto>>> getLogs(@RequestBody SttRequest request) {
        try {
            Long confId = request.getMeetingId(); // 요청받은 회의 ID
            List<LogResponseDto> logs = webService.getLogs(confId);
            return ResponseEntity.ok(ApiResponse.success(logs));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("로그 조회 중 오류가 발생했습니다."));
        }
    }


}