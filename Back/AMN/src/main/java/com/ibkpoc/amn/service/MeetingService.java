package com.ibkpoc.amn.service;

import com.ibkpoc.amn.dto.*;
import com.ibkpoc.amn.entity.*;
import com.ibkpoc.amn.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingService {
    private final MeetingRepository meetingRepository;

    // 회의 시작
    public StartMeetingResponse startMeeting(String startTimeStr) {
        // 시간 문자열을 LocalDateTime으로 변환
        LocalDateTime startTime = LocalDateTime.parse(startTimeStr.replace(" ", "T"));

        // 새 회의 생성
        Meeting meeting = new Meeting();
        meeting.setIsActive(true);
        meeting.setStartTime(startTime);

        // 저장
        meeting = meetingRepository.save(meeting);

        // 회의 번호로 제목 설정
        meeting.setTitle("회의 번호 : " + meeting.getConfId());
        meeting = meetingRepository.save(meeting);

        // 응답 생성
        return new StartMeetingResponse(
                meeting.getConfId(),
                null, // userId는 더 이상 사용하지 않음
                startTimeStr
        );
    }

    // 회의 종료
    public void endMeeting(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalStateException("회의를 찾을 수 없습니다."));

        // 회의 종료 처리
        meeting.setIsActive(false);
        meeting.setEndTime(LocalDateTime.now());
        meetingRepository.save(meeting);
    }
}