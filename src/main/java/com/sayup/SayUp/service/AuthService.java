package com.sayup.SayUp.service;

import com.sayup.SayUp.controller.AuthController;
import com.sayup.SayUp.dto.AuthRequestDTO;
import com.sayup.SayUp.dto.AuthResponseDTO;
import com.sayup.SayUp.dto.UserDTO;
import com.sayup.SayUp.entity.User;
import com.sayup.SayUp.repository.UserRepository;
import com.sayup.SayUp.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


@Service
public class AuthService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final Set<String> tokenBlacklist = new HashSet<>();

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);


    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       @Lazy AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 회원가입 로직
     * @param userDto 회원가입 요청 DTO
     */
    public void register(UserDTO userDto) {
        userRepository.findByEmail(userDto.getEmail()).ifPresent(user -> {
            logger.info("Registration attempt failed: Email already exists - {}", userDto.getEmail());
            throw new IllegalArgumentException("The email address is already in use. Please try another one.");
        });

        User user = new User();
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword())); // 비밀번호 암호화

        userRepository.save(user);
        logger.info("User registered successfully with email: {}", userDto.getEmail());
    }

    /**
     * Spring Security UserDetailsService 구현
     * @param email 사용자 이메일
     * @return UserDetails 객체
     * @throws UsernameNotFoundException 사용자 찾지 못했을 때 예외
     */
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

    /**
     * 로그인 로직
     * @param authRequestDTO 로그인 요청 DTO
     * @return AuthResponseDTO (JWT와 사용자 이메일)
     */
    public AuthResponseDTO login(AuthRequestDTO authRequestDTO) {
        // Authentication 객체 생성 및 인증
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequestDTO.getEmail(),
                        authRequestDTO.getPassword()
                )
        );

        String jwt = jwtTokenProvider.createToken(authentication);

        logger.info("Login successful for email: {}", authRequestDTO.getEmail());
        return new AuthResponseDTO(jwt, authRequestDTO.getEmail());
    }

    /**
     * 토큰을 블랙리스트에 추가
     * @param token 무효화할 토큰
     */
    public void invalidateToken(String token) {
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            tokenBlacklist.add(token);
            logger.info("Token invalidated and added to blacklist: {}", token);
        } else {
            logger.warn("Attempted to invalidate an invalid or empty token.");
            throw new IllegalArgumentException("Invalid token. Cannot invalidate.");
        }
    }


    /**
     * 토큰이 블랙리스트에 있는지 확인
     * @param token 확인할 토큰
     * @return 블랙리스트 여부
     */
    public boolean isTokenBlacklisted(String token) {
        if (!StringUtils.hasText(token)) {
            logger.warn("Empty token checked for blacklist.");
            throw new IllegalArgumentException("Token cannot be empty.");
        }
        return tokenBlacklist.contains(token);
    }
}
