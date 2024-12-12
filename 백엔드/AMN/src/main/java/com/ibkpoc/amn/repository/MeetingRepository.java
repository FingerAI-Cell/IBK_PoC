package com.ibkpoc.amn.repository;

import com.ibkpoc.amn.entity.Meeting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface MeetingRepository extends JpaRepository<Meeting, Long> {
    boolean existsByIsActive(Boolean isActive);

    List<Meeting> findByIsActive(boolean isActive);
}
