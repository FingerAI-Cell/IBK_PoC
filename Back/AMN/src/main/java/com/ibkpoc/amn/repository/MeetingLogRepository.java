package com.ibkpoc.amn.repository;

import com.ibkpoc.amn.entity.MeetingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;


@Repository
public interface MeetingLogRepository extends JpaRepository<MeetingLog, Long> {

    @Query("SELECT l FROM MeetingLog l LEFT JOIN l.meetingUser u WHERE (u.confId = :confId OR u IS NULL)")
    List<MeetingLog> findByConfId(@Param("confId") Long confId);


    List<MeetingLog> findByMeetingUserCuserIdInOrderByStartTime(Set<Long> cuserIds);
}