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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final MeetingRepository meetingRepository;
    private final MeetingUserRepository meetingUserRepository; // MeetingUser 엔티티용 JPA Repository
    private final MeetingLogRepository meetingLogRepository;

//    private final AISummarizer summarizer;

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
        List<MeetingUser> users = meetingUserRepository.findByMeetingId(confId);

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



    public List<SttContentDto> readSttContent(String filePath) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new FileNotFoundException("파일이 존재하지 않습니다: " + filePath);
        }

        try {
            // 파일 존재 여부 로그
            System.out.println("파일 경로 확인: " + filePath);

            // JSON 파일을 SttContentDto 리스트로 변환
            List<SttContentDto> sttContentList = objectMapper.readValue(
                    file,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, SttContentDto.class)
            );

            // 변환된 리스트 크기 확인 로그
            System.out.println("JSON 파일 읽기 성공: " + sttContentList.size() + "개의 항목이 변환되었습니다.");

            return sttContentList;
        } catch (JsonParseException | JsonMappingException e) {
            System.err.println("잘못된 JSON 형식입니다: " + e.getMessage());
            throw new IllegalArgumentException("잘못된 JSON 형식입니다.", e);
        } catch (IOException e) {
            System.err.println("파일 읽기 중 오류 발생: " + e.getMessage());
            throw new IOException("파일 읽기 중 오류가 발생했습니다.", e);
        }
    }

    public String generateSummary(Long confId) throws Exception {
        // 1. DB에서 conf_id에 해당하는 Meeting 가져오기
        Meeting meeting = meetingRepository.findById(confId)
                .orElseThrow(() -> new FileNotFoundException("해당 회의를 찾을 수 없습니다."));

        // 2. STT 파일 경로 확인 및 읽기
        String sttSrcPath = meeting.getSttSrc();
        File sttFile = new File(sttSrcPath);
        if (!sttFile.exists()) {
            throw new FileNotFoundException("STT 파일을 찾을 수 없습니다: " + sttSrcPath);
        }

        ObjectMapper objectMapper = new ObjectMapper();
//        List<Map<String, Object>> sttData = objectMapper.readValue(sttFile, new TypeReference<List<Map<String, Object>>>() {});


        // 3. 필요한 데이터만 추출 (speaker와 text)
//        List<Map<String, String>> filteredData = sttData.stream()
//                .map(entry -> Map.of(
//                        "speaker", (String) entry.get("speaker"),
//                        "text", (String) entry.get("text")
//                ))
//                .collect(Collectors.toList());

        // 4. speaker 치환: MeetingUser 데이터를 기반으로 매핑
        Map<String, String> speakerMapping = meetingUserRepository.findByMeetingId(confId).stream()
                .collect(Collectors.toMap(
                        user -> "SPEAKER_" + String.format("%02d", user.getSpeakerId()),
                        user -> user.getName() // 명확히 String 반환
                ));



//        filteredData.forEach(entry -> {
//            String speaker = entry.get("speaker");
//            if (speakerMapping.containsKey(speaker)) {
//                entry.put("speaker", speakerMapping.get(speaker));
//            }
//        });

        // 5. OpenAI 요약 API 호출
//        String summary = summarizer.summarizeData(filteredData);

        // 6. DB에 요약 저장
//        meeting.setSummary(summary);
        meetingRepository.save(meeting);

        return "ad";
    }
}