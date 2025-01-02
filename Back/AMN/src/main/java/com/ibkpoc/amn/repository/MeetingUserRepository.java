package com.ibkpoc.amn.repository;
import com.ibkpoc.amn.entity.MeetingUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface MeetingUserRepository extends JpaRepository<MeetingUser, Long> {
    List<MeetingUser> findByMeeting_ConfId(Long confId);

    @Query("SELECT u FROM MeetingUser u WHERE u.meeting.confId = :confId")
    List<MeetingUser> findByMeetingId(@Param("confId") Long confId);
}