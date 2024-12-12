package com.ibkpoc.amn.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;


// RecordService.java
@Service
@RequiredArgsConstructor
public class RecordService {

    @Value("${app.record.base-path:#{systemProperties['user.dir']}/meeting_records}")
    private String baseRecordPath;

    public void saveRecordFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new IllegalArgumentException("파일명이 없습니다.");
        }

        // 파일명에서 정보 파싱 (meetingId_userId_timestamp.wav)
        String[] parts = originalFilename.split("_");
        if (parts.length < 3) {
            throw new IllegalArgumentException("잘못된 파일명 형식입니다.");
        }

        // 현재 날짜로 디렉토리 구조 생성
        LocalDateTime now = LocalDateTime.now();
        String datePath = String.format("%d/%02d/%02d",
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth());

        // 최종 저장 경로 생성
        Path directory = Paths.get(baseRecordPath, datePath);
        Files.createDirectories(directory);

        // 파일 저장
        Path filePath = directory.resolve(originalFilename);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}