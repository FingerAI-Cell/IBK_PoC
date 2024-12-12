package com.ibkpoc.amn.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "ibk_poc_conf_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MeetingLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long convId;

    private Long confId;

    private int userId;

    private String content;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
