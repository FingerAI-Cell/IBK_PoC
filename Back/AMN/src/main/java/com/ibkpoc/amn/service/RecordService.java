package com.ibkpoc.amn.service;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.*;
import org.springframework.beans.factory.DisposableBean;

// service/RecordService.java
@Slf4j
@Service
@RequiredArgsConstructor
public class RecordService implements DisposableBean {
    private final MeetingService meetingService;  // 의존성 추가

    @Value("${app.record.base-path:#{systemProperties['user.dir']}/meeting_records}")
    private String baseRecordPath;

    private final Map<Long, RecordingInfo> activeRecordings = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new ThreadFactoryBuilder().setNameFormat("recording-scheduler-%d").build()
    );

    @Data
    private static class RecordingInfo {
        private final Long meetingId;  // meetingId 필드 추가
        private final Path filePath;
        private long totalBytes = 0;
        private final LocalDateTime startTime;
        private ScheduledFuture<?> timeoutFuture;
        private boolean endSignalReceived = false;

        public RecordingInfo(Long meetingId, Path filePath, LocalDateTime startTime) {
            this.meetingId = meetingId;
            this.filePath = filePath;
            this.startTime = startTime;
        }
    }

    @Async
    public void saveRecordChunk(Long meetingId, Long chunkStartTime, Long duration, MultipartFile file) {
        log.info("청크 저장 시작: meetingId={}, chunkStartTime={}, duration={}, size={}",
                meetingId, chunkStartTime, duration, file.getSize());

        RecordingInfo info = activeRecordings.computeIfAbsent(meetingId, k -> {
            try {
                LocalDateTime now = LocalDateTime.now();
                String datePath = String.format("%d/%02d/%02d",
                        now.getYear(), now.getMonthValue(), now.getDayOfMonth());
                Path directory = Paths.get(baseRecordPath, datePath);
                Files.createDirectories(directory);
                Path pcmPath = directory.resolve(String.format("meeting_%d.pcm", meetingId));
                log.info("새 녹음 시작: meetingId={}, path={}", meetingId, pcmPath);
                return new RecordingInfo(meetingId, pcmPath, now);
            } catch (IOException e) {
                log.error("녹음 초기화 실패: meetingId={}", meetingId, e);
                throw new RuntimeException("디렉토리 생성 실패", e);
            }
        });

        try {
            resetTimeout(info);
            saveChunkData(info, file, duration);
            log.info("청크 저장 완료: meetingId={}, totalBytes={}", meetingId, info.getTotalBytes());
        } catch (Exception e) {
            log.error("청크 저장 중 오류 (계속 진행): meetingId={}, chunkStartTime={}", meetingId, chunkStartTime, e);
        }
    }

    private void saveChunkData(RecordingInfo info, MultipartFile file, Long duration) throws IOException {
        try (InputStream inputStream = file.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(info.getFilePath().toFile(), true)) {

            byte[] buffer = new byte[8192];
            long totalBytesRead = 0;
            int bytesRead;

            try {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    info.totalBytes += bytesRead;
                }
            } catch (IOException e) {
                log.error("청크 데이터 읽기/쓰기 실패: meetingId={}", info.getMeetingId());

                // 실패한 부분을 무음으로 채움
                long expectedBytes = (duration * 44100 * 16 / 1000) / 8;
                long remainingBytes = expectedBytes - totalBytesRead;

                if (remainingBytes > 0) {
                    byte[] silence = new byte[8192];
                    while (remainingBytes > 0) {
                        int writeSize = (int) Math.min(8192, remainingBytes);
                        outputStream.write(silence, 0, writeSize);
                        remainingBytes -= writeSize;
                        info.totalBytes += writeSize;
                    }
                    log.info("실패한 청크 무음 처리: {} bytes", expectedBytes - totalBytesRead);
                }
            }
            outputStream.flush();
        }
    }

    private void resetTimeout(RecordingInfo info) {
        if (info.getTimeoutFuture() != null) {
            info.getTimeoutFuture().cancel(false);
        }

        ScheduledFuture<?> timeoutFuture = scheduler.schedule(() -> {
            if (!info.isEndSignalReceived()) {
                try {
                    log.warn("타임아웃으로 인한 강제 종료: meetingId={}", info.getMeetingId());
                    finalizeRecording(info.getMeetingId(), true);
                    meetingService.endMeeting(info.getMeetingId());
                } catch (IOException e) {
                    log.error("강제 종료 중 오류", e);
                }
            }
        }, 10, TimeUnit.SECONDS);

        info.setTimeoutFuture(timeoutFuture);
    }

    @Async
    public void markEndSignal(Long meetingId) {
        log.info("녹음 종료 신호 수신: meetingId={}", meetingId);
        RecordingInfo info = activeRecordings.get(meetingId);
        if (info != null) {
            info.setEndSignalReceived(true);
            log.info("녹음 종료 처리 시작: meetingId={}, totalDuration={}ms",
                    meetingId,
                    java.time.Duration.between(info.getStartTime(), LocalDateTime.now()).toMillis());

            if (info.getTimeoutFuture() != null) {
                info.getTimeoutFuture().cancel(false);
                log.debug("타임아웃 타이머 취소됨: meetingId={}", meetingId);
            }

            scheduler.schedule(() -> {
                try {
                    finalizeRecording(meetingId, false);
                } catch (IOException e) {
                    log.error("녹음 종료 처리 실패: meetingId={}", meetingId, e);
                }
            }, 2, TimeUnit.SECONDS);
        } else {
            log.warn("존재하지 않는 녹음에 대한 종료 신호: meetingId={}", meetingId);
        }
    }

    private synchronized void finalizeRecording(Long meetingId, boolean forcedEnd) throws IOException {
        log.info("녹음 파일 변환 시작: meetingId={}, forcedEnd={}", meetingId, forcedEnd);
        RecordingInfo info = activeRecordings.remove(meetingId);
        if (info != null) {
            if (info.getTimeoutFuture() != null) {
                info.getTimeoutFuture().cancel(false);
            }

            Path pcmFile = info.getFilePath();
            Path wavFile = pcmFile.resolveSibling(
                    pcmFile.getFileName().toString().replace(".pcm", forcedEnd ? "_forced.wav" : ".wav")
            );

            convertPcmToWav(pcmFile, wavFile, info.getTotalBytes());
            Files.delete(pcmFile);
            log.info("녹음 파일 변환 완료: meetingId={}, outputFile={}, totalBytes={}",
                    meetingId, wavFile.getFileName(), info.getTotalBytes());
        } else {
            log.warn("존재하지 않는 녹음에 대한 종료 처리: meetingId={}", meetingId);
        }
    }


    private void convertPcmToWav(Path pcmFile, Path wavFile, long pcmLength) throws IOException {
        final int sampleRate = 44100;
        final int channels = 1;
        final int bitsPerSample = 16;
        final int byteRate = sampleRate * channels * bitsPerSample / 8;
        final long totalDataLen = pcmLength + 36;

        try (FileInputStream pcmInput = new FileInputStream(pcmFile.toFile());
             FileOutputStream wavOutput = new FileOutputStream(wavFile.toFile())) {

            byte[] header = new byte[44];

            // RIFF 헤더
            header[0] = 'R'; header[1] = 'I'; header[2] = 'F'; header[3] = 'F';
            putLittleEndianInt(header, 4, (int) totalDataLen);
            header[8] = 'W'; header[9] = 'A'; header[10] = 'V'; header[11] = 'E';

            // fmt 청크
            header[12] = 'f'; header[13] = 'm'; header[14] = 't'; header[15] = ' ';
            putLittleEndianInt(header, 16, 16);
            putLittleEndianShort(header, 20, (short) 1);
            putLittleEndianShort(header, 22, (short) channels);
            putLittleEndianInt(header, 24, sampleRate);
            putLittleEndianInt(header, 28, byteRate);
            putLittleEndianShort(header, 32, (short) (channels * bitsPerSample / 8));
            putLittleEndianShort(header, 34, (short) bitsPerSample);

            // 데이터 청크
            header[36] = 'd'; header[37] = 'a'; header[38] = 't'; header[39] = 'a';
            putLittleEndianInt(header, 40, (int) pcmLength);

            wavOutput.write(header);

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = pcmInput.read(buffer)) != -1) {
                wavOutput.write(buffer, 0, bytesRead);
            }
        }
    }

    private void putLittleEndianInt(byte[] data, int offset, int value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
        data[offset + 2] = (byte) ((value >> 16) & 0xFF);
        data[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }

    private void putLittleEndianShort(byte[] data, int offset, short value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
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
            Thread.currentThread().interrupt();  // 인터럽트 상태 복원
        }
    }
}