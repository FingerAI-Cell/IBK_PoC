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

    private String content;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String name;
    private String company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cuser_id")
    private MeetingUser meetingUser;
}
