package com.sayup.SayUp.service.auth;

import com.sayup.SayUp.dto.auth.AuthRequestDTO;
import com.sayup.SayUp.dto.auth.AuthResponseDTO;
import com.sayup.SayUp.entity.user.User;
import com.sayup.SayUp.kakao.dto.KakaoUserInfoResponseDto;
import com.sayup.SayUp.kakao.service.KakaoService;
import com.sayup.SayUp.repository.UserRepository;
import com.sayup.SayUp.security.CustomUserDetails;
import com.sayup.SayUp.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService implements UserDetailsService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final KakaoService kakaoService;

    /**
     * 회원가입 로직
     */
    public AuthResponseDTO register(AuthRequestDTO authRequestDTO) {
        // 이메일 중복 확인
        if (userRepository.existsByEmail(authRequestDTO.getEmail())) {
            log.warn("Registration attempt failed: Email already exists - {}", authRequestDTO.getEmail());
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 사용자 생성
        User user = User.builder()
                .email(authRequestDTO.getEmail())
                .username(authRequestDTO.getEmail().split("@")[0])
                .password(passwordEncoder.encode(authRequestDTO.getPassword()))
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User registered successfully with email: {}", authRequestDTO.getEmail());

        // 자동 로그인 처리
        return login(authRequestDTO);
    }

    /**
     * 로그인 로직
     */
    public AuthResponseDTO login(AuthRequestDTO authRequestDTO) {
        try {
            // 사용자 조회
            User user = userRepository.findByEmail(authRequestDTO.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다."));

            // 비밀번호 검증
            if (!passwordEncoder.matches(authRequestDTO.getPassword(), user.getPassword())) {
                log.warn("Login failed for email: {} - Invalid password", authRequestDTO.getEmail());
                throw new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
            }

            // 인증 토큰 생성
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user.getEmail(),
                    null,
                    Collections.singletonList(new SimpleGrantedAuthority(user.getRole()))
            );

            // JWT 토큰 생성
            String accessToken = jwtTokenProvider.createToken(authentication);
            String refreshToken = jwtTokenProvider.createRefreshToken(authentication);

            log.info("Login successful for email: {}", authRequestDTO.getEmail());

            // 응답 생성
            return AuthResponseDTO.builder()
                    .accessToken(accessToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getExpirationTime())
                    .refreshToken(refreshToken)
                    .userInfo(AuthResponseDTO.UserInfo.builder()
                            .userId(user.getUserId())
                            .email(user.getEmail())
                            .username(user.getUsername())
                            .role(user.getRole())
                            .build())
                    .build();

        } catch (BadCredentialsException e) {
            log.warn("Login failed for email: {} - Invalid credentials", authRequestDTO.getEmail());
            throw new BadCredentialsException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
    }

    /**
     * 카카오 로그인 처리
     */
    public AuthResponseDTO kakaoLogin(String code) {
        try {
            // 카카오 API로 사용자 정보 조회
            KakaoUserInfoResponseDto kakaoUserInfo = kakaoService.processKakaoLogin(code);
            
            // 이메일 검증
            String email = kakaoUserInfo.getEmail();
            if (!StringUtils.hasText(email)) {
                throw new IllegalArgumentException("카카오 계정에서 이메일 정보를 가져올 수 없습니다.");
            }

            // 이메일 인증 여부 확인 (선택사항)
            if (!kakaoUserInfo.isEmailVerified()) {
                log.warn("Kakao user email not verified: {}", email);
            }

            // 사용자 조회 또는 생성
            User user = userRepository.findByEmail(email)
                    .orElseGet(() -> createKakaoUser(kakaoUserInfo));

            // JWT 토큰 생성
            String accessToken = jwtTokenProvider.createTokenFromEmail(email);
            String refreshToken = jwtTokenProvider.createRefreshTokenFromEmail(email);

            log.info("Kakao login successful for user: {}", email);

            return AuthResponseDTO.builder()
                    .accessToken(accessToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtTokenProvider.getExpirationTime())
                    .refreshToken(refreshToken)
                    .userInfo(AuthResponseDTO.UserInfo.builder()
                            .userId(user.getUserId())
                            .email(user.getEmail())
                            .username(user.getUsername())
                            .role(user.getRole())
                            .build())
                    .build();

        } catch (Exception e) {
            log.error("Kakao login failed: {}", e.getMessage());
            throw new IllegalArgumentException("카카오 로그인 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 카카오 사용자 생성
     */
    private User createKakaoUser(KakaoUserInfoResponseDto kakaoUserInfo) {
        String email = kakaoUserInfo.getEmail();
        String nickname = kakaoUserInfo.getNickname();
        String username = StringUtils.hasText(nickname) ? nickname : email.split("@")[0];

        User newUser = User.builder()
                .email(email)
                .username(username)
                .password(passwordEncoder.encode(generateSecureRandomPassword()))
                .role("USER")
                .createdAt(LocalDateTime.now())
                .build();
        
        User savedUser = userRepository.save(newUser);
        log.info("New Kakao user created: {} (ID: {})", email, savedUser.getUserId());
        return savedUser;
    }

    /**
     * 토큰 갱신
     */
    public AuthResponseDTO refreshToken(String refreshToken) {
        if (!jwtTokenProvider.validateRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않은 리프레시 토큰입니다.");
        }

        String email = jwtTokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        String newAccessToken = jwtTokenProvider.createTokenFromEmail(email);
        String newRefreshToken = jwtTokenProvider.createRefreshTokenFromEmail(email);

        return AuthResponseDTO.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .refreshToken(newRefreshToken)
                .userInfo(AuthResponseDTO.UserInfo.builder()
                        .userId(user.getUserId())
                        .email(user.getEmail())
                        .username(user.getUsername())
                        .role(user.getRole())
                        .build())
                .build();
    }

    /**
     * 로그아웃
     */
    public void logout(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("토큰이 비어있습니다.");
        }

        if (!jwtTokenProvider.validateToken(token)) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }

        long expirationTime = jwtTokenProvider.getExpirationTime(token);
        tokenBlacklistService.addToBlacklist(token, expirationTime);
        
        log.info("User logged out successfully");
    }

    /**
     * 토큰 블랙리스트 확인
     */
    public boolean isTokenBlacklisted(String token) {
        if (!StringUtils.hasText(token)) {
            return false;
        }
        return tokenBlacklistService.isBlacklisted(token);
    }

    /**
     * Spring Security UserDetailsService 구현
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        return new CustomUserDetails(user);
    }

    /**
     * 보안을 위한 랜덤 비밀번호 생성
     */
    private String generateSecureRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 32; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
