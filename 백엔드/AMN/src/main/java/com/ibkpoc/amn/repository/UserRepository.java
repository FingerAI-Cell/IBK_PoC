package com.ibkpoc.amn.repository;

import com.ibkpoc.amn.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    List<User> findByIsActive(boolean isActive);
    List<User> findByUserIdIn(List<Integer> userIds);
}
