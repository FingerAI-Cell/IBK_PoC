package com.ibkpoc.amn.service;

import aj.org.objectweb.asm.TypeReference;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibkpoc.amn.dto.LogResponseDto;
import com.ibkpoc.amn.dto.MeetingResponseDto;
import com.ibkpoc.amn.dto.SpeakerResponseDto;
import com.ibkpoc.amn.dto.SttContentDto;
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
import java.util.List;
import java.util.Map;
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

    private MeetingResponseDto convertToDto(Meeting meeting) {
        return MeetingResponseDto.builder()
                .confId(meeting.getConfId())
                .title(meeting.getTitle())
                .startTime(meeting.getStartTime())
                .endTime(meeting.getEndTime())
                .summary(meeting.getSummary())
                .sttSign(meeting.getSttSign())
                .participants(meeting.getParticipants())
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

        // 2. 관련 MeetingUser 가져오기
        List<MeetingUser> users = meetingUserRepository.findByConfId(confId);
        Map<Long, String> userNameMapping = users.stream()
                .collect(Collectors.toMap(MeetingUser::getCuserId, MeetingUser::getName));

        // 3. 관련 MeetingLog 가져오기
        List<MeetingLog> logs = meetingLogRepository.findByMeetingUserCuserIdInOrderByStartTime(
                userNameMapping.keySet());

        // 4. name과 content 조합
        List<Map<String, Object>> jsonData = logs.stream()
                .map(log -> Map.of(
                        "speaker", (Object) userNameMapping.get(log.getMeetingUser().getCuserId()),
                        "text", (Object) log.getContent()
                ))
                .collect(Collectors.toList());
        logger.info("JSON data prepared for summarization: {}", jsonData);

        // 5. AI 요약 호출
        List<String> summaryResults = summarizer.summarizeData(jsonData);
        logger.info("Summary results received: {}", summaryResults);

        // 6. 요약 결과 저장
        String finalSummary = summaryResults.get(0);
        meeting.setSummary(finalSummary);
        logger.info("Final summary set to meeting: {}", finalSummary);

        meetingRepository.save(meeting);
        logger.info("Meeting saved to database: {}", meeting);
    }
}