package com.ibkpoc.amn.controller;

import com.ibkpoc.amn.dto.*;
import com.ibkpoc.amn.service.MeetingService;
import com.ibkpoc.amn.service.RecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// controller/MeetingController.java
@Slf4j
@RestController
@RequestMapping("/meeting")
@RequiredArgsConstructor
public class MeetingController {
    private final MeetingService meetingService;
    private final RecordService recordService;

    @PostMapping("/start")
    public ResponseEntity<CommonResponse<?>> startMeeting(@RequestBody StartMeetingRequest request) {
        StartMeetingResponse response = meetingService.startMeeting(request.getStartTime());
        return ResponseEntity.ok(new CommonResponse<>("SUCCESS", "회의가 시작되었습니다", response));
    }

    @PostMapping("/end")
    public ResponseEntity<CommonResponse<?>> endMeeting(@RequestBody EndMeetingRequest request) {
        try {
            Long meetingId = request.getMeetingId();
            recordService.markEndSignal(meetingId);
            meetingService.endMeeting(meetingId);
            return ResponseEntity.ok(new CommonResponse<>("SUCCESS", "회의가 종료되었습니다", null));
        } catch (Exception e) {
            log.error("회의 종료 실패", e);
            return ResponseEntity.badRequest()
                    .body(new CommonResponse<>("ERROR", "회의 종료 실패: " + e.getMessage(), null));
        }
    }

    @PostMapping("/upload/chunk")
    public ResponseEntity<CommonResponse<?>> uploadRecordChunk(@ModelAttribute AudioChunkRequest request) {
        log.info("청크 업로드 요청: meetingId={}, chunkStartTime={}",
                request.getMeetingId(), request.getChunkStartTime());

        try {
            recordService.saveRecordChunk(
                    request.getMeetingId(),
                    request.getChunkStartTime(),
                    request.getDuration(),
                    request.getFile()
            );
            return ResponseEntity.ok(new CommonResponse<>("SUCCESS", "청크가 업로드되었습니다", null));
        } catch (Exception e) {
            log.error("청크 업로드 실패: meetingId={}", request.getMeetingId(), e);
            // 실패해도 200 응답 (클라이언트가 계속 진행하도록)
            return ResponseEntity.ok(
                    new CommonResponse<>("PARTIAL_SUCCESS", "청크 처리 중 일부 오류 발생", null)
            );
        }
    }
}



