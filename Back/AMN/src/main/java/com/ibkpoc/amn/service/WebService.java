package com.ibkpoc.amn.service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibkpoc.amn.dto.MeetingResponseDto;
import com.ibkpoc.amn.dto.SttContentDto;
import com.ibkpoc.amn.entity.Meeting;
import com.ibkpoc.amn.repository.MeetingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WebService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final MeetingRepository meetingRepository;

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
                .sttSrc(meeting.getSttSrc())
                .participants(meeting.getParticipants())
                .build();
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
}