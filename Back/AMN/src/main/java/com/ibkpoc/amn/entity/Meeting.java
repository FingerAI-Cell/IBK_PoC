package com.ibkpoc.amn.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ibk_poc_conf")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Meeting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long confId;

    private String title;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean isActive;
    private String summary;
    @Column(name = "overall_summary")
    private String overallSummary; // 전체 요약 추가

    private String topic;
    @Column(name = "participant_count") // 컬럼명 명시
    private Integer participants; // 참가자 수
    private String wavSrc; // 녹음 파일 위치 추가
    private String sttSrc; // 녹음 파일 위치 추가
    private Boolean sttSign;
}