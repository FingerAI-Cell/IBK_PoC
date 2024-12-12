package com.ibkpoc.amn.controller;

import com.ibkpoc.amn.dto.*;
import com.ibkpoc.amn.service.MeetingService;
import com.ibkpoc.amn.service.RecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/meeting")  // /api 제거
@RequiredArgsConstructor
public class MeetingController {
    private final MeetingService meetingService;
    private final RecordService recordService;

    // 회의 시작
    @PostMapping("/start")
    public ResponseEntity<CommonResponse<?>> startMeeting(@RequestBody StartMeetingRequest request) {
        StartMeetingResponse response = meetingService.startMeeting(request.getStartTime());
        return ResponseEntity.ok(new CommonResponse<>("SUCCESS", "회의가 시작되었습니다", response));
    }

    // 회의 종료
    @PostMapping("/end")
    public ResponseEntity<CommonResponse<?>> endMeeting(@RequestBody EndMeetingRequest request) {
        meetingService.endMeeting(request.getMeetingId());
        return ResponseEntity.ok(new CommonResponse<>("SUCCESS", "회의가 종료되었습니다", null));
    }

    // 녹음 파일 업로드
    @PostMapping("/upload")
    public ResponseEntity<CommonResponse<?>> uploadRecord(
            @RequestParam("file") MultipartFile file
    ) {
        try {
            recordService.saveRecordFile(file);
            return ResponseEntity.ok(new CommonResponse<>("SUCCESS", "파일이 업로드되었습니다", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(new CommonResponse<>("ERROR", "파일 업로드 실패: " + e.getMessage(), null));
        }
    }
}