package com.ibkpoc.amn.controller;

import com.ibkpoc.amn.dto.*;
import com.ibkpoc.amn.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/meeting")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    // Join meeting
    @PostMapping("/join")
    public ResponseEntity<CommonResponse<MeetingResponse>> joinMeeting(@RequestBody MeetingRequest request) {
        MeetingResponse response = meetingService.joinMeeting(request.getUserId());
        return ResponseEntity.ok(new CommonResponse<>("SUCCESS", "Joined meeting successfully", response));
    }

    // Start meeting
    @PostMapping("/start")
    public ResponseEntity<CommonResponse<MeetingResponse>> startMeeting(@RequestBody MeetingRequest request) {
        MeetingResponse response = meetingService.startMeeting(request.getUserId());
        return ResponseEntity.ok(new CommonResponse<>("SUCCESS", "Meeting started successfully", response));
    }

    // End meeting
    @PostMapping("/end")
    public ResponseEntity<CommonResponse<Void>> endMeeting(@RequestBody MeetingRequest request) {
        meetingService.endMeeting(request.getUserId());
        return ResponseEntity.ok(new CommonResponse<>("SUCCESS", "Meeting(s) ended successfully", null));
    }

    @GetMapping("/list")
    public ResponseEntity<CommonResponse<List<MeetingListResponse>>> listMeetings() {
        List<MeetingListResponse> response = meetingService.listMeetings();
        return ResponseEntity.ok(new CommonResponse<>("SUCCESS","Getting list successfully",response));
    }

    @PostMapping("/details")
    public ResponseEntity<CommonResponse<MeetingDetailResponse>> getMeetingDetails(@RequestBody MeetingDetailRequest request) {
        MeetingDetailResponse response = meetingService.getMeetingDetails(request.getMeetingId());
        return ResponseEntity.ok(new CommonResponse<>("SUCCESS", "성공", response));
    }
}