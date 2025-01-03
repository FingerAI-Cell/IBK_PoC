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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeetingService {
    private final MeetingRepository meetingRepository;
    private final MeetingUserRepository meetingUserRepository;

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

        // 5. 참가자 수만큼 MeetingUser 생성
        System.out.println("MeetingUser 생성 시작, 참가자 수: " + participants);

        for (int i = 0; i < participants; i++) {
            MeetingUser meetingUser = new MeetingUser();
            meetingUser.setConfId(meeting.getConfId()); // 회의 ID 설정
            meetingUser.setSpeakerId(String.format("SPEAKER_%02d", i)); // SPEAKER_00, SPEAKER_01, ...
            meetingUser.setName(null); // name은 비워둠
            meetingUser.setCompany(null); // company는 비워둠
            // 각 참가자 정보 출력
            System.out.println("생성된 MeetingUser 정보: confId=" + meetingUser.getConfId() + ", speakerId=" + meetingUser.getSpeakerId());

            try {
                // 저장
                meetingUserRepository.save(meetingUser);
                System.out.println("MeetingUser 저장 성공: speakerId=" + meetingUser.getSpeakerId());
            } catch (Exception e) {
                System.out.println("MeetingUser 저장 실패: confId=" + meetingUser.getConfId() +
                        ", speakerId=" + meetingUser.getSpeakerId() +
                        ", 에러=" + e.getMessage());
                e.printStackTrace(); // 에러 전체 스택 트레이스 출력
            }
        }

        System.out.println("모든 MeetingUser 저장 완료");

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
                "data", Map.of(
                        "file_name", meeting.getWavSrc(),
                        "participant", meeting.getParticipants()
                )
        );

        try {
            // 파이썬 코드의 data 파라미터 구조에 맞춤
            String requestJson = String.format(
                    "{\"file_name\": \"%s\", \"participant\": %d}",
                    meeting.getWavSrc(),
                    meeting.getParticipants()
            );

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();

            log.info("STT API 호출 시작: {}, 요청 데이터: {}", apiUrl, requestJson);

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("STT API 호출 성공: 응답={}", response.body());
            } else {
                log.error("STT API 호출 실패: 상태 코드={}", response.statusCode());
                throw new RuntimeException("STT API 호출 실패");
            }

        } catch (Exception e) {
            log.error("STT API 호출 중 예외 발생: {}", e.getMessage(), e);
            throw new RuntimeException("STT API 처리 실패", e);
        }
    }
}