package com.ibkpoc.amn.repository;

import com.ibkpoc.amn.entity.MeetingLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface MeetingLogRepository extends JpaRepository<MeetingLog, Long> {
    @Query("SELECT COUNT(DISTINCT ml.userId) FROM MeetingLog ml WHERE ml.confId = :confId")
    int countDistinctUserIdByConfId(Long confId);

    @Query("SELECT DISTINCT ml.userId FROM MeetingLog ml WHERE ml.confId = :confId")
    List<Integer> findDistinctUserIdsByConfId(Long confId);

    List<Record> findByConfId(Long confId);
}