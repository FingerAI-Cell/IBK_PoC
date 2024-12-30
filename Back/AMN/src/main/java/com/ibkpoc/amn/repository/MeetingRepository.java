package com.ibkpoc.amn.repository;

import com.ibkpoc.amn.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    Optional<Meeting> findByConfIdAndIsActiveTrue(Long confId);
    List<Meeting> findAllByOrderByStartTimeDesc();
}