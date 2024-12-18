package com.ibkpoc.amn.service;

import com.ibkpoc.amn.dto.*;
import com.ibkpoc.amn.entity.*;
import com.ibkpoc.amn.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.InputStreamReader;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingService {
    private final MeetingRepository meetingRepository;

    // 회의 시작
    public StartMeetingResponse startMeeting(Integer participants,String startTimeStr) {
        log.info("startMeeting 호출: participants={}, startTimeStr={}", participants, startTimeStr);

        // 시간 문자열을 LocalDateTime으로 변환
        LocalDateTime startTime = LocalDateTime.parse(startTimeStr.replace(" ", "T"));

        // 새 회의 생성
        Meeting meeting = new Meeting();
        meeting.setIsActive(true);
        meeting.setStartTime(startTime);
        meeting.setParticipants(participants);

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

    public void updateWavSrc(Long meetingId, String filePath) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalStateException("회의를 찾을 수 없습니다."));

        meeting.setWavSrc(filePath);
        meetingRepository.save(meeting);
    }

    public void processSttRequest(Long meetingId) {
        // DB에서 회의 정보 조회
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("회의 정보를 찾을 수 없습니다: meetingId=" + meetingId));

        // 필요한 정보 로그
        log.info("STT 파라미터 조회: meetingId={}, participants={}, wavSrc={}",
                meetingId, meeting.getParticipants(), meeting.getWavSrc());

        if (meeting.getWavSrc() == null) {
            throw new IllegalStateException("WAV 파일 경로가 존재하지 않습니다: meetingId=" + meetingId);
        }

        // 파이썬 코드 실행
        String pythonScriptPath = "/path/to/stt_script.py"; // 파이썬 스크립트 경로 설정
        ProcessBuilder processBuilder = new ProcessBuilder(
                "python", pythonScriptPath,
                "--file_name", meeting.getWavSrc(),
                "--participant", meeting.getParticipants().toString()
        );

        try {
            log.info("STT 파이썬 스크립트 실행 시작: {}", pythonScriptPath);
            Process process = processBuilder.start();

            // 결과 출력 로그
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("STT 스크립트 결과: {}", line);
            }

            int exitCode = process.waitFor();
            log.info("STT 파이썬 스크립트 종료: exitCode={}", exitCode);

            if (exitCode != 0) {
                throw new RuntimeException("STT 스크립트 실행 중 오류 발생");
            }

        } catch (Exception e) {
            log.error("STT 스크립트 실행 실패: {}", e.getMessage(), e);
            throw new RuntimeException("STT 처리 실패", e);
        }
    }
}