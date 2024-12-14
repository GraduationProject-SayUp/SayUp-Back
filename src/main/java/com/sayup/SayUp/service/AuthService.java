package com.sayup.SayUp.service;

import com.sayup.SayUp.controller.AuthController;
import com.sayup.SayUp.dto.AuthRequestDTO;
import com.sayup.SayUp.dto.AuthResponseDTO;
import com.sayup.SayUp.dto.UserDto;
import com.sayup.SayUp.model.User;
import com.sayup.SayUp.repository.UserRepository;
import com.sayup.SayUp.security.JwtTokenProvider;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;


@Service
public class AuthService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Lazy
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);


    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, @Lazy AuthenticationManager authenticationManager,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
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

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    // 로그인 로직 추가
    public AuthResponseDTO login(AuthRequestDTO authRequestDTO) {
        Optional<User> userOptional = userRepository.findByEmail(authRequestDTO.getEmail());

        if (userOptional.isEmpty()) {
            throw new BadCredentialsException("User not found");
        }

        User user = userOptional.get();

        // 비밀번호 검증
        if (!passwordEncoder.matches(authRequestDTO.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        // 인증 토큰 생성
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(new ArrayList<>()) // 권한 설정
                .build();

        // Authentication 객체 생성
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities()
        );


        // JWT 토큰 생성
        String jwt = jwtTokenProvider.createToken(authentication);

        logger.info("Login successful for email: {}", authRequestDTO.getEmail());
        return new AuthResponseDTO(jwt, user.getEmail());
    }
}
