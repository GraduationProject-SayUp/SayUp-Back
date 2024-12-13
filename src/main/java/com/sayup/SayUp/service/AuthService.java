package com.sayup.SayUp.service;

import com.sayup.SayUp.dto.UserDto;
import com.sayup.SayUp.model.User;
import com.sayup.SayUp.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class AuthService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 회원가입 로직
    public void register(UserDto userDto) {
        if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists!");
        }

        User user = new User();
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword())); // 비밀번호 암호화

        userRepository.save(user);
    }

    // UserDetailsService 구현: 사용자 정보 로드
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(Collections.emptyList()) // 권한 설정
                .build();
    }
}
