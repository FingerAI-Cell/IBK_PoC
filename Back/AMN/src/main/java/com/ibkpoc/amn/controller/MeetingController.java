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
        // 전달받은 참가자 수와 시작 시간 로그 출력
        log.info("회의 시작 요청: startTime={}, participants={}", request.getStartTime(), request.getParticipants());
        StartMeetingResponse response = meetingService.startMeeting(request.getParticipants(), request.getStartTime());
        return ResponseEntity.ok(new CommonResponse<>("SUCCESS", "회의가 시작되었습니다", response));
    }

    @PostMapping("/end")
    public ResponseEntity<CommonResponse<?>> endMeeting(@RequestBody EndMeetingRequest request) {
        try {
            Long meetingId = request.getMeetingId();
            meetingService.endMeeting(meetingId);
            return ResponseEntity.ok(new CommonResponse<>("SUCCESS", "회의가 종료되었습니다", null));
        } catch (Exception e) {
            log.error("회의 종료 실패", e);
            return ResponseEntity.badRequest()
                    .body(new CommonResponse<>("ERROR", "회의 종료 실패: " + e.getMessage(), null));
        }
    }

    @PostMapping("/upload/chunk")
    public ResponseEntity<CommonResponse<?>> uploadWavChunk(@ModelAttribute WavUploadRequest request) {
        log.info("청크 업로드 요청 수신: meetingId={}, sectionNumber={}, currentChunk={}, totalChunks={}",
                request.getMeetingId(), request.getSectionNumber(), request.getCurrentChunk(), request.getTotalChunks());
        try {
            byte[] chunkBytes = request.getChunkDataBytes(); // MultipartFile을 byte[]로 변환
            if (chunkBytes == null) {
                throw new IllegalArgumentException("청크 데이터가 비어 있습니다.");
            }
            // WavUploadRequest를 새로운 객체로 생성해 RecordService에 전달
            WavUploadRequest processedRequest = new WavUploadRequest(
                    request.getMeetingId(),
                    request.getSectionNumber(),
                    request.getStartTime(),
                    null, // chunkData는 이미 변환되었으므로 null 처리
                    request.getTotalChunks(),
                    request.getCurrentChunk()
            );
            // 청크 처리
            recordService.saveAndProcessChunk(processedRequest, chunkBytes); // byte[] 전달

            return ResponseEntity.ok(new CommonResponse<>("SUCCESS", "청크 처리 완료", null));
        } catch (Exception e) {
            log.error("청크 처리 실패: meetingId={}, sectionNumber={}, error={}",
                    request.getMeetingId(), request.getSectionNumber(), e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(new CommonResponse<>("ERROR", "청크 처리 실패: " + e.getMessage(), null));
        }
    }

    @PostMapping("/stt-request")
    public ResponseEntity<CommonResponse<?>> processStt(@RequestBody SttRequest request) {
        try {
            log.info("STT 요청 수신: meetingId={}", request.getMeetingId());

            // 서비스 호출
            meetingService.processSttRequest(request.getMeetingId());

            return ResponseEntity.ok(new CommonResponse<>("SUCCESS", "STT 요청이 처리되었습니다", null));
        } catch (Exception e) {
            log.error("STT 요청 처리 실패: meetingId={}", request.getMeetingId(), e);
            return ResponseEntity.badRequest()
                    .body(new CommonResponse<>("ERROR", "STT 요청 처리 실패: " + e.getMessage(), null));
        }
    }
}






