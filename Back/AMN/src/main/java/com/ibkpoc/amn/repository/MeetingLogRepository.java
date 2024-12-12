package com.ibkpoc.amn.repository;

import com.ibkpoc.amn.entity.MeetingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface MeetingLogRepository extends JpaRepository<MeetingLog, Long> {
    List<MeetingLog> findByMeetingUser_Meeting_ConfId(Long confId);
}