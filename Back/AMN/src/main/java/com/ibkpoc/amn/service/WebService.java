package com.ibkpoc.amn.service;

import aj.org.objectweb.asm.TypeReference;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibkpoc.amn.dto.*;
import com.ibkpoc.amn.entity.Meeting;
import com.ibkpoc.amn.entity.MeetingLog;
import com.ibkpoc.amn.entity.MeetingUser;
import com.ibkpoc.amn.repository.MeetingLogRepository;
import com.ibkpoc.amn.repository.MeetingRepository;
import com.ibkpoc.amn.repository.MeetingUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(WebService.class);

    private final MeetingRepository meetingRepository;
    private final MeetingUserRepository meetingUserRepository; // MeetingUser 엔티티용 JPA Repository
    private final MeetingLogRepository meetingLogRepository;

    private final AISummarizer summarizer;

    public List<MeetingResponseDto> getAllMeetings() {
        return meetingRepository.findAllByOrderByStartTimeDesc()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MeetingSummaryResponseDto getMeetingSummaryById(Long meetingId) throws FileNotFoundException {
        String summary = meetingRepository.findById(meetingId)
                .map(Meeting::getSummary)
                .orElse(""); // summary가 null인 경우 빈 문자열 반환

        return MeetingSummaryResponseDto.builder()
                .summary(summary)
                .build();
    }


    private MeetingResponseDto convertToDto(Meeting meeting) {
        return MeetingResponseDto.builder()
                .confId(meeting.getConfId())
                .title(meeting.getTitle())
                .startTime(meeting.getStartTime())
                .endTime(meeting.getEndTime())
                .summarySign((meeting.getSummary()!=null))
                .sttSign(meeting.getSttSign())
                .build();
    }

    public List<SpeakerResponseDto> getSpeakers(Long confId) {
        // 1. MeetingUser 조회
        List<MeetingUser> users = meetingUserRepository.findByConfId(confId);

        // 2. SpeakerResponseDto 리스트 반환
        return users.stream()
                .map(user -> new SpeakerResponseDto(
                        user.getSpeakerId(), // speakerId
                        user.getCuserId(),   // cuserId
                        user.getName() // name
                ))
                .collect(Collectors.toList());
    }

    public List<LogResponseDto> getLogs(Long confId) {
        // 1. MeetingLog 조회
        List<MeetingLog> logs = meetingLogRepository.findByConfId(confId);

        // 2. LogResponseDto 리스트 반환
        return logs.stream()
                .sorted(Comparator.comparing(MeetingLog::getStartTime))
                .map(log -> {
                    MeetingUser user = log.getMeetingUser();
                    return new LogResponseDto(
                            log.getContent(),                             // content
                            (user != null ? user.getCuserId() : null),    // cuserId
                            log.getStartTime()
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void generateSummary(Long confId) throws Exception {
        logger.info("Starting summary generation for confId: {}", confId);
        // 1. Meeting 가져오기
        Meeting meeting = meetingRepository.findById(confId)
                .orElseThrow(() -> new FileNotFoundException("Meeting not found: " + confId));
        logger.info("meeting data prepared for summarization: {}", meeting);
        // 2. 관련 MeetingUser 가져오기
        List<MeetingUser> users = meetingUserRepository.findByConfId(confId);
        logger.info("user data prepared for summarization: {}", users);

        // 2. users에서 cuserId 목록 생성
        Set<Long> cuserIds = users.stream()
                .map(MeetingUser::getCuserId)
                .collect(Collectors.toSet());

        // 2. 이름 매핑 로직 추가
        Map<Long, String> userNameMapping = users.stream()
                .collect(Collectors.toMap(
                        MeetingUser::getCuserId,
                        user -> user.getName() != null ? user.getName() : user.getSpeakerId()
                ));
        logger.info("User name mapping prepared: {}", userNameMapping);

        // 3. 관련 MeetingLog 가져오기
        List<MeetingLog> logs = meetingLogRepository.findByMeetingUserCuserIdInOrderByStartTime(
                cuserIds);
        logger.info("log data prepared for summarization: {}", logs);
        // 4. name과 content 조합
        List<Map<String, Object>> jsonData = logs.stream()
                .map(log -> Map.of(
                        "speaker", (Object) userNameMapping.get(log.getMeetingUser().getCuserId()),
                        "text", (Object) log.getContent()
                ))
                .collect(Collectors.toList());
        logger.info("JSON data prepared for summarization: {}", jsonData);

        // JSON 데이터를 문자열로 변환
        String jsonString;
        try {
            jsonString = new ObjectMapper().writeValueAsString(jsonData);
            logger.info("JSON string for summarization: {}", jsonString);
        } catch (Exception e) {
            logger.error("Failed to convert JSON data to string", e);
            throw new RuntimeException("JSON 변환 실패");
        }

        // 5. AI 요약 호출
        String summaryResults = summarizer.summarizeData(jsonString);
        logger.info("Summary results received: {}", summaryResults);

        // 6. 요약 결과 저장
        meeting.setSummary(summaryResults);
        logger.info("Final summary set to meeting: {}", summaryResults);

        meetingRepository.save(meeting);
        logger.info("Meeting saved to database: {}", meeting);
    }

    @Transactional
    public void updateSpeakers(SpeakerUpdateRequest request) {
        Long confId = request.getConfId();
        List<SpeakerUpdateDto> speakers = request.getSpeakers();

        logger.info("Updating speakers for confId: {}", confId);

        // 요청받은 각 Speaker에 대해 처리
        for (SpeakerUpdateDto speaker : speakers) {
            Long cuserId = speaker.getCuserId();
            String newName = speaker.getName();

            // MeetingUser 가져오기
            MeetingUser meetingUser = meetingUserRepository.findById(cuserId)
                    .orElseThrow(() -> new IllegalArgumentException("cuserId " + cuserId + "에 해당하는 발화자를 찾을 수 없습니다."));

            // 이름 업데이트
            meetingUser.setName(newName);
            logger.info("Updated cuserId {} with new name: {}", cuserId, newName);

            // 저장
            meetingUserRepository.save(meetingUser);
        }

        logger.info("Speaker updates completed for confId: {}", confId);
    }
}