package com.sayup.SayUp.controller;

import com.sayup.SayUp.model.User;
import com.sayup.SayUp.repository.UserRepository;
import com.sayup.SayUp.security.JwtTokenProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public UserController(JwtTokenProvider jwtTokenProvider, UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @PostMapping("/tts-vector")
    public ResponseEntity<Map<String, String>> saveTTSVector(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> body) {
        String ttsVector = body.get("ttsVector"); // 문자열 그대로 가져옴

        logger.info("Received request to save TTS Vector");
        Map<String, String> response = new HashMap<>();
        try {
            // JWT 토큰에서 사용자 이메일 추출
            String email = jwtTokenProvider.getUsernameFromToken(token.substring(7));

            // 사용자 조회 및 벡터 저장
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

            user.setTtsVector(ttsVector);
            userRepository.save(user);

            logger.info("TTS Vector successfully saved for user: {}", email);
            response.put("message", "TTS Vector saved successfully");
            return ResponseEntity.ok(response);
        } catch (UsernameNotFoundException ex) {
            logger.error("User not found: {}", ex.getMessage());
            response.put("error", "User not found");
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalArgumentException ex) {
            logger.error("Invalid request: {}", ex.getMessage());
            response.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception ex) {
            logger.error("Error saving TTS Vector", ex);
            response.put("error", "Internal server error: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
