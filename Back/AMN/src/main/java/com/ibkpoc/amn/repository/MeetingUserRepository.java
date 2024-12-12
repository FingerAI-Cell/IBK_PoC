package com.ibkpoc.amn.repository;
import com.ibkpoc.amn.entity.MeetingUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MeetingUserRepository extends JpaRepository<MeetingUser, Long> {
    List<MeetingUser> findByMeeting_ConfId(Long confId);
}