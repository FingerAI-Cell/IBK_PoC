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
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${ai.base-url}") // 환경별 URL 주입
    private String apiUrl_base;

    public List<MeetingResponseDto> getAllMeetings() {
        return meetingRepository.findAllByOrderByStartTimeDesc()
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MeetingSummaryResponseDto getMeetingSummaryById(Long meetingId) throws FileNotFoundException {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new FileNotFoundException("Meeting not found: " + meetingId));

        // 🔹 전체 요약 (OverallSummary) 처리
        List<MeetingSummaryResponseDto.OverallTopicDetail> overallTopics =
                (List<MeetingSummaryResponseDto.OverallTopicDetail>) parseSummary(meeting.getOverallSummary(), true);
        logger.info("Parsed overall summary for meetingId: {} -> JSON: {}", meetingId, overallTopics);

        // 🔹 화자별 요약 (SpeakerSummary) 처리
        List<MeetingSummaryResponseDto.SpeakerTopicDetail> speakerTopics =
                (List<MeetingSummaryResponseDto.SpeakerTopicDetail>) parseSummary(meeting.getSummary(), false);
        logger.info("Parsed speaker summary for meetingId: {} -> JSON: {}", meetingId, speakerTopics);

        return MeetingSummaryResponseDto.builder()
                .overall(MeetingSummaryResponseDto.OverallSummary.builder().topics(overallTopics).build())
                .speaker(MeetingSummaryResponseDto.SpeakerSummary.builder().topics(speakerTopics).build())
                .build();
    }


    private List<?> parseSummary(String json, boolean isOverall) {
        if (json == null || json.isBlank()) {
            return List.of(); // 빈 리스트 반환
        }

        // 1. JSON을 Map으로 변환
        Map<String, Object> summaryData;
        try {
            summaryData = objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse summary JSON", e);
        }

        // 2. output 필드 추출
        String rawOutput = (String) summaryData.get("output");
        if (rawOutput == null || rawOutput.isBlank()) {
            throw new IllegalStateException("Output field is missing or empty");
        }

        // 3. output 필드 정리
        String cleanedOutput = rawOutput
                .replaceAll("^```json", "") // 시작 부분의 ```json 제거
                .replaceAll("```", "")     // 끝 부분의 ``` 제거
                .replace("\n", "")         // 줄바꿈 제거
                .trim();

        // 4. JSON 데이터 파싱
        List<Map<String, Object>> parsedOutput;
        try {
            parsedOutput = objectMapper.readValue(cleanedOutput, List.class);
            logger.info("Parsed output JSON: {}", parsedOutput);
        } catch (Exception e) {
            logger.error("Failed to parse output JSON", e);
            throw new RuntimeException("Failed to parse output JSON", e);
        }

        // 🔹 5. 변환 로직 (Overall vs. Speaker 구분)
        if (isOverall) {
            logger.info("Processing Overall Summary with {} items.", parsedOutput.size());
            return parsedOutput.stream()
                    .map(item -> {
                        String topic = (String) item.get("topic");
                        String content = (String) item.get("content");
                        logger.info("Overall Topic: {}, Content: {}", topic, content);
                        return new MeetingSummaryResponseDto.OverallTopicDetail(topic, content);
                    })
                    .collect(Collectors.toList());
        } else {
            logger.info("Processing Speaker Summary with {} items.", parsedOutput.size());
            return parsedOutput.stream()
                    .map(item -> {
                        String topic = (String) item.get("topic");
                        if (topic == null || topic.isBlank()) {
                            logger.error("Topic field is missing or empty");
                            throw new IllegalStateException("Topic field is missing or empty");
                        }

                        // 🔹 Speaker 변환
                        List<MeetingSummaryResponseDto.SpeakerDetail> speakers = item.entrySet().stream()
                                .filter(entry -> !"topic".equals(entry.getKey()))
                                .map(entry -> {
                                    logger.info("Speaker: {}, Content: {}", entry.getKey(), entry.getValue());
                                    return new MeetingSummaryResponseDto.SpeakerDetail(
                                            entry.getKey(),
                                            entry.getValue().toString()
                                    );
                                })
                                .collect(Collectors.toList());

                        logger.info("Processed Speaker Topic: {} with {} speakers", topic, speakers.size());
                        return new MeetingSummaryResponseDto.SpeakerTopicDetail(topic, speakers);
                    })
                    .collect(Collectors.toList());
        }
    }



    private MeetingResponseDto convertToDto(Meeting meeting) {
        return MeetingResponseDto.builder()
                .confId(meeting.getConfId())
                .title(meeting.getTitle())
                .startTime(meeting.getStartTime())
                .endTime(meeting.getEndTime())
                .summarySign(meeting.getSummary() != null && meeting.getOverallSummary() != null) // 새로운 조건 추가
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
        logger.info("Fetched logs from DB: {}", logs);

        // 2. LogResponseDto 리스트 반환
        return logs.stream()
                .sorted(Comparator.comparing(MeetingLog::getStartTime))
                .map(log -> new LogResponseDto(
                        log.getConvId(),
                        log.getContent(),
                        log.getMeetingUser().getCuserId(), // cuserId
                        log.getStartTime()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateLogs(LogUpdateRequest request) {
        for (LogUpdateRequest.LogUpdate logUpdate : request.getLogs()) {
            MeetingLog meetingLog = meetingLogRepository.findById(logUpdate.getLogId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid logId: " + logUpdate.getLogId()));
            MeetingUser meetingUser = meetingUserRepository.findById(logUpdate.getCuserId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid cuserId: " + logUpdate.getCuserId()));
            meetingLog.setMeetingUser(meetingUser);
            meetingLogRepository.save(meetingLog);
        }
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

        List<MeetingUser> validUsers = users.stream()
                .filter(user -> !"UNKNOWN".equals(user.getSpeakerId()))
                .collect(Collectors.toList());
        logger.info("Filtered valid users (excluding 'UNKNOWN'): {}", validUsers);

        // 4. validUsers에서 cuserId 목록 생성
        Set<Long> validCuserIds = validUsers.stream()
                .map(MeetingUser::getCuserId)
                .collect(Collectors.toSet());

        // 5. 이름 매핑 로직 추가
        Map<Long, String> userNameMapping = validUsers.stream()
                .collect(Collectors.toMap(
                        MeetingUser::getCuserId,
                        user -> user.getName() != null ? user.getName() : user.getSpeakerId()
                ));
        logger.info("User name mapping prepared: {}", userNameMapping);

        // 6. 관련 MeetingLog 가져오기
        List<MeetingLog> logs = meetingLogRepository.findByMeetingUserCuserIdInOrderByStartTime(
                validCuserIds);
        logger.info("Log data prepared for summarization: {}", logs);

        // 7. name과 content 조합
        List<Map<String, Object>> speakerjsonData = logs.stream()
                .map(log -> Map.of(
                        "speaker", (Object) userNameMapping.get(log.getMeetingUser().getCuserId()),
                        "text", (Object) log.getContent()
                ))
                .collect(Collectors.toList());
        logger.info("JSON data prepared for summarization: {}", speakerjsonData);

        List<Map<String,Object>> overalljsonData = logs.stream()
                .map(log->Map.of(
                        "text", (Object) log.getContent()
                ))
                .collect(Collectors.toList());
        logger.info("Overall data prepared for summarization: {}", overalljsonData);

        // JSON 데이터를 문자열로 변환
        String speakerjsonString;
        String overalljsonString;
        try {
            speakerjsonString = new ObjectMapper().writeValueAsString(speakerjsonData);
            overalljsonString = new ObjectMapper().writeValueAsString(overalljsonData);
            logger.info("JSON string for summarization: {}", speakerjsonString);
            logger.info("JSON string for summarization: {}", overalljsonString);
        } catch (Exception e) {
            logger.error("Failed to convert JSON data to string", e);
            throw new RuntimeException("JSON 변환 실패");
        }

        // 5. AI 요약 호출
        try {
            // 🔹 5. AI API 호출 (화자별 요약)
            String speakerSummaryJson = summarizer.summarizeData(speakerjsonString, apiUrl_base + "/summary/invoke");
            logger.info("Speaker summary received: {}", speakerSummaryJson);

            // 🔹 6. AI API 호출 (전체 요약)
            String overallSummaryJson = summarizer.summarizeData(overalljsonString, apiUrl_base + "/summary/another/invoke");
            logger.info("Overall summary received: {}", overallSummaryJson);

            // 🔹 7. DB 저장
            meeting.setSummary(speakerSummaryJson);
            meeting.setOverallSummary(overallSummaryJson);
            meetingRepository.save(meeting);
            logger.info("Summary saved to database for confId: {}", confId);
        } catch (Exception e) {
            logger.error("Error during summarization", e);
            throw new RuntimeException("요약 생성 실패", e);
        }
    }

    @Transactional
    public void updateSpeakers(SpeakerUpdateRequest request) {
        for (SpeakerUpdateDto speakerUpdate : request.getSpeakers()) {
            MeetingUser meetingUser = meetingUserRepository.findById(speakerUpdate.getCuserId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid cuserId: " + speakerUpdate.getCuserId()));
            meetingUser.setName(speakerUpdate.getName());
            meetingUserRepository.save(meetingUser);
        }
        logger.info("Speaker updates completed for confId: {}", request.getConfId());
    }
}