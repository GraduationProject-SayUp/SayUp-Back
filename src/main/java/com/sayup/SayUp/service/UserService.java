package com.sayup.SayUp.service;

import com.sayup.SayUp.entity.User;
import com.sayup.SayUp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public Long findUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(User::getUserId)
                .orElseThrow(() -> new UsernameNotFoundException("해당 이메일로 사용자를 찾을 수 없습니다: " + email));
    }

    public Long findUserIdByEmailOrUsername(String emailOrUsername) {
        List<User> users = userRepository.findByEmailOrUsername(emailOrUsername, emailOrUsername);
        if (users.isEmpty()) {
            throw new UsernameNotFoundException("해당 이메일 또는 사용자명으로 사용자를 찾을 수 없습니다: " + emailOrUsername);
        }
        return users.get(0).getUserId();
    }
}
