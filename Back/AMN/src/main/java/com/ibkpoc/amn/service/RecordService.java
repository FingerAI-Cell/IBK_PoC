package com.ibkpoc.amn.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.DisposableBean;
import com.ibkpoc.amn.dto.WavUploadRequest;

// service/RecordService.java
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordService implements DisposableBean {
    private final MeetingService meetingService;

    @Value("${app.record.base-path:#{systemProperties['user.dir']}/meeting_records}")
    private String baseRecordPath;

    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    private final Map<Long, RecordingInfo> activeRecordings = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new ThreadFactoryBuilder().setNameFormat("recording-scheduler-%d").build()
    );

    @PostConstruct
    public void init() {
        log.info("RecordService init 시작");  // 추가
        try {
            Path basePath = Paths.get(baseRecordPath);
            log.info("기본 경로: {}", basePath.toAbsolutePath());  // 추가

            if (!Files.exists(basePath)) {
                Files.createDirectories(basePath);
                log.info("기본 녹음 디렉토리 생성됨: {}", baseRecordPath);
            } else {
                log.info("기존 녹음 디렉토리 존재: {}", baseRecordPath);  // 추가
            }

            if (!Files.isWritable(basePath)) {
                log.error("녹음 디렉토리에 쓰기 권한 없음: {}", baseRecordPath);
                throw new RuntimeException("녹음 디렉토리 권한 오류");
            }
            log.info("녹음 디렉토리 권한 확인 완료");  // 추가
        } catch (IOException e) {
            log.error("녹음 디렉토리 초기화 실패: {}", e.getMessage(), e);  // 스택트레이스 추가
            throw new RuntimeException("녹음 디렉토리 초기화 실패", e);
        }
    }

    @Data
    private static class RecordingInfo {
        private final Long meetingId;
        private final Path filePath;
        private long totalBytes = 0;
        private final LocalDateTime startTime;
        private ScheduledFuture<?> timeoutFuture;
        private final TreeMap<Integer, byte[]> wavChunks = new TreeMap<>();
        private int totalWavChunks = 0;
        private String originalStartTime;  // 클라이언트가 보낸 원래 시작 시간 저장
        private Long duration;  // 녹음 duration 저장

        public RecordingInfo(Long meetingId, Path filePath, LocalDateTime startTime) {
            this.meetingId = meetingId;
            this.filePath = filePath;
            this.startTime = startTime;
        }

        public boolean isWavComplete() {
            return wavChunks.size() == totalWavChunks && totalWavChunks > 0;
        }
    }

    public void saveWavFile(WavUploadRequest request) throws IOException {
        log.info("WAV 청크 저장 시작: meetingId={}, chunk={}/{}, size={}, 파일명={}",
                request.getMeetingId(),
                request.getCurrentChunk(),
                request.getTotalChunks(),
                request.getFile().getSize(),
                request.getFile().getOriginalFilename());  // 파일명 로깅 추가

        RecordingInfo info = activeRecordings.computeIfAbsent(request.getMeetingId(), k -> {
            try {
                LocalDateTime now = LocalDateTime.now();
                String datePath = "/";
                Path directory = Paths.get(baseRecordPath, datePath);
                log.info("디렉토리 생성 시도: {}", directory.toAbsolutePath());  // 추가

                Files.createDirectories(directory);

                Path wavPath = directory.resolve(String.format("meeting_%d_%s.wav",
                        request.getMeetingId(),
                        request.getStartTime()));
                log.info("WAV 파일 경로 생성: {}", wavPath.toAbsolutePath());  // 추가
                return new RecordingInfo(request.getMeetingId(), wavPath, now);
            } catch (IOException e) {
                log.error("WAV 파일 초기화 실패: meetingId={}, error={}",
                        request.getMeetingId(), e.getMessage(), e);
                throw new RuntimeException("디렉토리 생성 실패", e);
            }
        });

        try {
            resetTimeout(info);
            info.totalWavChunks = request.getTotalChunks();
            info.wavChunks.put(request.getCurrentChunk(), request.getFile().getBytes());
            log.info("청크 저장됨: meetingId={}, chunk={}/{}, 현재 청크 수={}",
                    request.getMeetingId(),
                    request.getCurrentChunk(),
                    request.getTotalChunks(),
                    info.wavChunks.size());  // 추가

            if (info.isWavComplete()) {
                log.info("모든 청크 도착, 파일 병합 시작: meetingId={}", request.getMeetingId());  // 추가
                try (FileOutputStream fos = new FileOutputStream(info.getFilePath().toFile())) {
                    for (byte[] chunk : info.wavChunks.values()) {
                        fos.write(chunk);
                        info.totalBytes += chunk.length;
                    }
                }
                // 2. 타임아웃 타이머 취소 (모든 청크 수신 완료)
                if (info.getTimeoutFuture() != null) {
                    info.getTimeoutFuture().cancel(false);
                }

                // 3. DB 업데이트 및 정리 작업
                finalizeRecording(info.getMeetingId(), false);

                log.info("WAV 파일 처리 완료: meetingId={}, 경로={}, totalBytes={}",
                        request.getMeetingId(),
                        info.getFilePath().toAbsolutePath(),
                        info.getTotalBytes());
            }
        } catch (Exception e) {
            log.error("WAV 청크 저장 실패: meetingId={}, chunk={}, error={}",
                    request.getMeetingId(), request.getCurrentChunk(), e.getMessage(), e);
            throw e;
        }
    }

    private void resetTimeout(RecordingInfo info) {
        if (info.getTimeoutFuture() != null) {
            info.getTimeoutFuture().cancel(false);
        }

        ScheduledFuture<?> timeoutFuture = scheduler.schedule(() -> {
            try {
                log.warn("타임아웃으로 인한 강제 종료: meetingId={}", info.getMeetingId());
                finalizeRecording(info.getMeetingId(), true);
            } catch (IOException e) {
                log.error("강제 종료 중 오류", e);
            }
        }, 180, TimeUnit.SECONDS);

        info.setTimeoutFuture(timeoutFuture);
    }

    private synchronized void finalizeRecording(Long meetingId, boolean forcedEnd) throws IOException {
        log.info("녹음 파일 변환 시작: meetingId={}, forcedEnd={}", meetingId, forcedEnd);
        RecordingInfo info = activeRecordings.remove(meetingId);
        if (info != null) {
            if (info.getTimeoutFuture() != null) {
                info.getTimeoutFuture().cancel(false);
            }
            // WAV 파일이 완성되지 않았다면 처리
            if (!info.isWavComplete()) {
                log.warn("미완성 WAV 파일 감지: meetingId={}, 받은 청크={}/{}",
                        meetingId, info.getWavChunks().size(), info.getTotalWavChunks());
            }
            if (forcedEnd){
                try {
                    // 파일 병합 시도
                    try (FileOutputStream fos = new FileOutputStream(info.getFilePath().toFile())) {
                        for (byte[] chunk : info.getWavChunks().values()) {
                            fos.write(chunk);
                            info.totalBytes += chunk.length;
                        }
                    }
                    // 🔹 WAV 헤더 업데이트 (파일 크기 반영)
                    updateWavHeader(info.getFilePath(), info.totalBytes);

                    log.info("WAV 파일 저장 완료: meetingId={}, 경로={}, totalBytes={}",
                            meetingId, info.getFilePath().toAbsolutePath(), info.getTotalBytes());
                } catch (Exception e) {
                    log.error("WAV 파일 저장 실패: meetingId={}, error={}", meetingId, e.getMessage(), e);
                    if (!forcedEnd) {
                        throw e; // 강제 종료가 아닌 경우 예외 재던지기
                    }
                }
            }
            log.info("녹음 파일 처리 완료: meetingId={}, file={}, totalBytes={}, duration={}ms",
                    meetingId,
                    info.getFilePath().getFileName(),
                    info.getTotalBytes(),
                    info.getDuration());

            try {
                Path absoluteFilePath = info.getFilePath().toAbsolutePath(); // 절대 경로 포함
                String absolutePathString = absoluteFilePath.toString();     // String으로 변환
                log.info("WAV 파일 생성 완료: meetingId={}, 절대 경로={}", meetingId, absolutePathString);

                // MeetingService를 통해 wavSrc 필드 업데이트
                meetingService.updateWavSrc(meetingId, absolutePathString);
                TimeUnit.SECONDS.sleep(1);
                // STT 요청 비동기로 처리
                CompletableFuture.runAsync(() -> {
                    try {
                        meetingService.processSttRequest(meetingId);
                        log.info("STT 요청 비동기 처리 완료: meetingId={}", meetingId);
                    } catch (Exception e) {
                        log.error("STT 요청 비동기 처리 중 오류 발생: meetingId={}, error={}", meetingId, e.getMessage(), e);
                    }
                }, executorService);
                log.info("녹음 파일 처리 완료: meetingId={}, 파일={}, 총 바이트={}, 시간={}ms",
                        meetingId, info.getFilePath().getFileName(), info.getTotalBytes(), info.getDuration());
            } catch (Exception e) {
                log.error("녹음 파일 경로 업데이트 실패: meetingId={}, error={}", meetingId, e.getMessage(), e);
            }

        } else {
            log.warn("존재하지 않는 녹음에 대한 종료 처리: meetingId={}", meetingId);
        }
    }

    private void updateWavHeader(Path filePath, long audioDataSize) throws IOException {
        try (RandomAccessFile wavFile = new RandomAccessFile(filePath.toFile(), "rw")) {
            long chunkSize = 36 + audioDataSize;  // 전체 파일 크기 - 8
            wavFile.seek(4);
            wavFile.writeInt(Integer.reverseBytes((int) chunkSize)); // "ChunkSize" 업데이트

            wavFile.seek(40);
            wavFile.writeInt(Integer.reverseBytes((int) audioDataSize)); // "Subchunk2Size" 업데이트
        }
        log.info("WAV 헤더 업데이트 완료: 파일={}, 총 오디오 데이터 크기={}", filePath.getFileName(), audioDataSize);
    }

    @Override
    public void destroy() throws Exception {
        log.info("서버 종료 감지: 진행 중인 녹음 처리 시작");

        for (Long meetingId : new ArrayList<>(activeRecordings.keySet())) {
            try {
                finalizeRecording(meetingId, true);
                meetingService.endMeeting(meetingId);
                log.info("서버 종료 중 녹음 처리 완료: {}", meetingId);
            } catch (Exception e) {
                log.error("서버 종료 중 녹음 처리 실패: {}", meetingId, e);
            }
        }

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                log.warn("스케줄러 강제 종료");
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            log.error("스케줄러 종료 중 인터럽트", e);
            Thread.currentThread().interrupt();
        }
    }
}