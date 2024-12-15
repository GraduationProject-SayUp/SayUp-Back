package com.sayup.SayUp.controller;

import com.sayup.SayUp.entity.User;
import com.sayup.SayUp.repository.UserRepository;
import com.sayup.SayUp.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

// 사용자 TTS 벡터 저장 컨트롤러
// 사용자의 JWT를 통해 인증된 이메일을 기반으로 TTS 벡터를 비동기로 저장

@RestController
@RequestMapping("/api/users/tts")
public class UserTTSVectorController {
    private static final Logger logger = LoggerFactory.getLogger(UserTTSVectorController.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    public UserTTSVectorController(JwtTokenProvider jwtTokenProvider,
                                   UserRepository userRepository) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
    }

    @Async
    public CompletableFuture<Void> processAndSaveTTSVector(String email, String ttsVector) {
        return CompletableFuture.runAsync(() -> {
            try {
                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

                // TTS 벡터 저장
                user.setTtsVector(ttsVector);
                userRepository.save(user);

                logger.info("TTS Vector successfully saved for user: {}", email);
            } catch (Exception ex) {
                logger.error("Error saving TTS Vector", ex);
            }
        });
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> initiateVectorSave(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> body) {

        String ttsVector = body.get("ttsVector");
        logger.info("Received request to save TTS Vector");

        try {
            // JWT 토큰에서 사용자 이메일 추출
            String email = jwtTokenProvider.getUsernameFromToken(token.substring(7));

            // 비동기 처리 시작
            processAndSaveTTSVector(email, ttsVector);

            return ResponseEntity.accepted().body(Map.of(
                    "status", "processing",
                    "message", "TTS Vector processing started"
            ));

        } catch (Exception ex) {
            logger.error("Initial TTS Vector processing error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", "Failed to initiate TTS Vector processing"
                    ));
        }
    }
}