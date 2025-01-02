package com.ibkpoc.amn.repository;

import com.ibkpoc.amn.entity.MeetingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface MeetingLogRepository extends JpaRepository<MeetingLog, Long> {
    @Query("SELECT l FROM MeetingLog l WHERE l.meetingUser.confId = :confId")
    List<MeetingLog> findByConfId(@Param("confId") Long confId);
}