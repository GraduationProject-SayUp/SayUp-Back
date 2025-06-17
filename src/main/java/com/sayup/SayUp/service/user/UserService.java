package com.sayup.SayUp.service.user;

import com.sayup.SayUp.entity.user.User;
import com.sayup.SayUp.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;

    /**
     * 이메일로 사용자 ID 조회
     */
    @Transactional(readOnly = true)
    public Long findUserIdByEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일은 null이거나 비어있을 수 없습니다.");
        }

        log.debug("Finding user ID by email: {}", email);
        
        return userRepository.findByEmail(email)
                .map(User::getUserId)
                .orElseThrow(() -> new UsernameNotFoundException("해당 이메일로 사용자를 찾을 수 없습니다: " + email));
    }

    /**
     * 이메일 또는 사용자명으로 사용자 ID 조회
     */
    @Transactional(readOnly = true)
    public Long findUserIdByEmailOrUsername(String emailOrUsername) {
        if (emailOrUsername == null || emailOrUsername.trim().isEmpty()) {
            throw new IllegalArgumentException("이메일 또는 사용자명은 null이거나 비어있을 수 없습니다.");
        }

        log.debug("Finding user ID by email or username: {}", emailOrUsername);
        
        List<User> users = userRepository.findByEmailOrUsername(emailOrUsername, emailOrUsername);
        if (users.isEmpty()) {
            throw new UsernameNotFoundException("해당 이메일 또는 사용자명으로 사용자를 찾을 수 없습니다: " + emailOrUsername);
        }
        
        User user = users.get(0);
        if (!user.getIsActive()) {
            throw new UsernameNotFoundException("비활성 사용자입니다: " + emailOrUsername);
        }
        
        return user.getUserId();
    }

    /**
     * 사용자 정보 조회
     */
    @Transactional(readOnly = true)
    public User getUserById(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("사용자 ID는 null일 수 없습니다.");
        }

        log.debug("Finding user by ID: {}", userId);
        
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
    }

    /**
     * 사용자 활성 상태 확인
     */
    @Transactional(readOnly = true)
    public boolean isUserActive(Long userId) {
        if (userId == null) {
            return false;
        }

        return userRepository.findById(userId)
                .map(User::getIsActive)
                .orElse(false);
    }
}
