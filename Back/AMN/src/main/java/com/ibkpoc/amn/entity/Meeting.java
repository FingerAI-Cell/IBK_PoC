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
    private String topic;

    // createdBy 필드 제거 (더 이상 사용하지 않음)
}