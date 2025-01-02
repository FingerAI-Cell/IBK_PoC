package com.ibkpoc.amn.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ibk_poc_conf_user")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class MeetingUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cuserId;

    private String name;
    private String company;

    @Column(name = "conf_id")
    private Long meeting;

    private String speakerId;
}