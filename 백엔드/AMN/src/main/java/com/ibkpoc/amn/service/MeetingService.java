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
    private final UserRepository userRepository;
    private final MeetingLogRepository meetingLogRepository;

    // 참여 로그를 기록하는 private 메서드
    private void createParticipationLog(Long confId, Integer userId) {
        MeetingLog meetingLog = new MeetingLog();
        meetingLog.setConfId(confId);
        meetingLog.setUserId(userId);
        meetingLog.setContent("");
        meetingLog.setStartTime(LocalDateTime.now());
        meetingLog.setEndTime(LocalDateTime.now());
        meetingLogRepository.save(meetingLog);
    }

    // Join meeting
    public MeetingResponse joinMeeting(Integer userId) {
        // 1. 활성 미팅 조회
        List<Meeting> activeMeetings = meetingRepository.findByIsActive(true);
        if (activeMeetings.isEmpty()) {
            throw new IllegalStateException("No active meetings found. Start a meeting first.");
        }
        if (activeMeetings.size() > 1) {
            throw new IllegalStateException("Multiple active meetings detected. Unable to join.");
        }

        // 2. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (user.getIsActive()) {
            throw new IllegalStateException("User is already in another meeting.");
        }

        // 3. 미팅 참여 인원 증가 및 저장
        Meeting meeting = activeMeetings.get(0);
//        int incrementParticipate = meeting.getParticipantCount() + 1;
        // 로그 1: 초기 참가자 수
//        System.out.println("로그 1 - 현재 참가자 수 (Before Increment): " + meeting.getParticipantCount());
        // 로그 2: 증가 후 값
//        System.out.println("로그 2 - 증가된 참가자 수 (After Increment): " + meeting.getParticipantCount());

//        meeting.setParticipantCount(incrementParticipate);
//        meetingRepository.save(meeting);  // 변경 사항 저장
        // 참여 로그 기록 - 메서드 호출로 변경
        createParticipationLog(meeting.getConfId(), userId);

//        // 로그 3: 저장 후 다시 확인
//        Meeting updatedMeeting = meetingRepository.findById(meeting.getConfId())
//                .orElseThrow(() -> new IllegalStateException("Meeting not found after save"));
//        System.out.println("로그 3 - 저장 후 참가자 수 (After Save): " + updatedMeeting.getParticipantCount());
//

        // 4. 사용자 상태 업데이트 및 저장
        user.setIsActive(true);
        userRepository.save(user);  // 사용자 상태 저장

        // 5. 응답 반환
        return new MeetingResponse(user.getUserId(), meeting.getConfId());
    }

    // Start meeting
    public MeetingResponse startMeeting(Integer userId) {
        if (meetingRepository.existsByIsActive(true)) {
            throw new IllegalStateException("An active meeting already exists. Cannot start a new one.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (user.getIsActive()) {
            throw new IllegalStateException("User is already in another meeting.");
        }

        Meeting meeting = new Meeting();
        meeting.setIsActive(true);
        meeting.setCreatedBy(userId);
        meeting.setStartTime(LocalDateTime.now());
//        meeting.setParticipantCount(1);

        meeting = meetingRepository.save(meeting);
        meeting.setTitle("회의 번호 : "+meeting.getConfId());
        meeting = meetingRepository.save(meeting);

        // 참여 로그 기록 - 메서드 호출로 변경
        createParticipationLog(meeting.getConfId(), userId);

        user.setIsActive(true);
        userRepository.save(user);

        return new MeetingResponse(user.getUserId(), meeting.getConfId());
    }

    // End meeting
    public void endMeeting(Integer userId) {
        List<Meeting> activeMeetings = meetingRepository.findByIsActive(true);

        if (activeMeetings.isEmpty()) {
            throw new IllegalStateException("No active meetings to end.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        if (!user.getIsActive()) {
            throw new IllegalStateException("User is not part of any active meeting.");
        }

        for (Meeting meeting : activeMeetings) {
            meeting.setIsActive(false);
            meeting.setEndTime(LocalDateTime.now());
            meetingRepository.save(meeting);
        }

        // 모든 활성 사용자 비활성화
        List<User> activeUsers = userRepository.findByIsActive(true);
        for (User activeUser : activeUsers) {
            activeUser.setIsActive(false);
            userRepository.save(activeUser);
        }
    }

    // 회의 목록 조회
    public List<MeetingListResponse> listMeetings() {
        List<Meeting> meetings = meetingRepository.findAll();

        return meetings.stream()
                .map(meeting -> {
                    int participantCount = meetingLogRepository.countDistinctUserIdByConfId(meeting.getConfId());

                    return new MeetingListResponse(
                            meeting.getConfId(),
                            meeting.getTitle(),
                            meeting.getStartTime(),
                            meeting.getEndTime(),
                            participantCount,
                            meeting.getTopic()
                    );
                })
                .collect(Collectors.toList());
    }

    public MeetingDetailResponse getMeetingDetails(Long meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new IllegalStateException("회의를 찾을 수 없습니다."));

        // DB 레벨에서 중복 제거된 참가자 ID 목록 조회
        List<Integer> participantIds = meetingLogRepository.findDistinctUserIdsByConfId(meetingId);

        // 나머지 로직은 동일
        Map<String, List<String>> companyGroups = userRepository.findByUserIdIn(participantIds)
                .stream()
                .collect(Collectors.groupingBy(
                        User::getCompany,
                        Collectors.mapping(User::getName, Collectors.toList())
                ));

        StringBuilder participantStr = new StringBuilder();
        companyGroups.forEach((company, names) -> {
            participantStr.append(company)
                    .append(": ")
                    .append(String.join(", ", names))
                    .append("\n");
        });

        return new MeetingDetailResponse(
                meeting.getSummary(),
                participantStr.toString().trim()
        );
    }
}
