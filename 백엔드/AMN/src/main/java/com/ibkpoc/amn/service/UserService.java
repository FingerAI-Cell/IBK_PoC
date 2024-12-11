package com.ibkpoc.amn.service;

import com.ibkpoc.amn.dto.UserValidationResponse;
import com.ibkpoc.amn.entity.User;
import com.ibkpoc.amn.repository.MeetingRepository;
import com.ibkpoc.amn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    @Autowired
    private UserRepository userRepository;
    private final MeetingRepository meetingRepository;

    public UserValidationResponse validateUser(int userId) {
        boolean isValid = userRepository.existsById(userId);
        if (!isValid) {
            return new UserValidationResponse(userId, false, false, false);
        }

        User user = userRepository.findById(userId).get();
        boolean isActive = user.getIsActive(); // 사용자 active 여부
        boolean isMeetingActive = meetingRepository.existsByIsActive(true); // 미팅 active 여부

        return new UserValidationResponse(userId, true, isActive, isMeetingActive);
    }
}
