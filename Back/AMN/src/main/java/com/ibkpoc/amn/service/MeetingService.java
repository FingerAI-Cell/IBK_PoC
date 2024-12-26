package com.ibkpoc.amn.service;

import com.ibkpoc.amn.dto.*;
import com.ibkpoc.amn.entity.*;
import com.ibkpoc.amn.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingService {
    private final MeetingRepository meetingRepository;
    @PersistenceContext
    private EntityManager entityManager; // EntityManager 주입


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

    @Transactional
    public void processSttRequest(Long meetingId) {
        // DB에서 회의 정보 조회
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalArgumentException("회의 정보를 찾을 수 없습니다: meetingId=" + meetingId));

        entityManager.refresh(meeting);

        // 필요한 정보 로그
        log.info("STT 파라미터 조회: meetingId={}, participants={}, wavSrc={}",
                meetingId, meeting.getParticipants(), meeting.getWavSrc());

        if (meeting.getWavSrc() == null) {
            throw new IllegalStateException("WAV 파일 경로가 존재하지 않습니다: meetingId=" + meetingId);
        }

        // API URL 설정
        String apiUrl = "http://localhost:8081/run";

        // 요청 데이터 생성
        Map<String, Object> requestData = Map.of(
                "file_name", meeting.getWavSrc(),
                "participant", meeting.getParticipants()
        );

        try {
            // RestTemplate으로 API 호출
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestData, headers);

            log.info("STT API 호출 시작: {}", apiUrl);
            ResponseEntity<Map> responseEntity = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    requestEntity,
                    Map.class
            );

            // 응답 처리
            if (responseEntity.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> responseBody = responseEntity.getBody();
                log.info("STT API 호출 성공: 응답={}", responseBody);

                // 필요한 경우, 응답 데이터를 Meeting 객체에 저장하거나 추가 처리
                // 예: meeting.setSttResult(responseBody.get("result").toString());
                // meetingRepository.save(meeting);
            } else {
                log.error("STT API 호출 실패: 상태 코드={}", responseEntity.getStatusCode());
                throw new RuntimeException("STT API 호출 실패");
            }

        } catch (Exception e) {
            log.error("STT API 호출 중 예외 발생: {}", e.getMessage(), e);
            throw new RuntimeException("STT API 처리 실패", e);
        }
    }
}