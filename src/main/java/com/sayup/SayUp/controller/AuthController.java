package com.sayup.SayUp.controller;

import com.sayup.SayUp.dto.AuthRequestDTO;
import com.sayup.SayUp.dto.AuthResponseDTO;
import com.sayup.SayUp.dto.UserDTO;
import com.sayup.SayUp.service.AuthService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 사용자 회원가입 처리
     * @param userDTO 사용자 이메일 및 비밀번호 정보
     * @return 회원가입 성공 또는 실패 메시지
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid UserDTO userDTO) {
        logger.info("Register attempt with email: {}", userDTO.getEmail());
        try {
            authService.register(userDTO);
            return ResponseEntity.ok("User registered successfully!");
        } catch (Exception ex) {
            logger.error("Registration failed for email: {}", userDTO.getEmail(), ex);
            return ResponseEntity.badRequest().body("Registration failed: " + ex.getMessage());
        }
    }

    /**
     * 사용자 로그인 처리
     * @param authRequestDTO 사용자의 이메일 및 비밀번호
     * @return JWT 토큰 및 사용자 정보
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody @Valid AuthRequestDTO authRequestDTO) {
        logger.info("Login attempt with email: {}", authRequestDTO.getEmail());
        try {
            AuthResponseDTO response = authService.login(authRequestDTO);
            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            logger.error("Login failed for email: {}", authRequestDTO.getEmail(), ex);
            return ResponseEntity.badRequest().body(null);
        }
    }

    /**
     * 로그아웃 엔드포인트
     * @param token 클라이언트가 전달한 JWT
     * @return 성공 여부
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String token) {
        if (token == null || token.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Authorization header is missing or empty");
        }

        // "Bearer " 제거 후 토큰 추출
        token = token.substring(7);

        // 이미 블랙리스트에 있는지 확인
        if (authService.isTokenBlacklisted(token)) {
            return ResponseEntity.badRequest().body("Token is already invalidated.");
        }

        // 토큰 블랙리스트에 추가
        authService.invalidateToken(token);
        logger.info("Token invalidated successfully: {}", token);
        return ResponseEntity.ok("Logout successful");
    }
}
