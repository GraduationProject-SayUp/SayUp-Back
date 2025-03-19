package com.sayup.SayUp.controller;

import com.sayup.SayUp.entity.User;
import com.sayup.SayUp.repository.UserRepository;
import com.sayup.SayUp.security.JwtTokenProvider;
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
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

            // TTS 벡터 저장
            user.setTtsVector(ttsVector);
            userRepository.save(user);
        });
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> initiateVectorSave(@RequestHeader("Authorization") String token, @RequestBody Map<String, String> body) {

        String ttsVector = body.get("ttsVector");

        // "Bearer " 제거 후 실제 JWT 토큰 추출
        String jwt = token.startsWith("Bearer ") ? token.substring(7) : token;

        // JWT 토큰에서 사용자 이메일 추출
        String email = jwtTokenProvider.getUsernameFromToken(jwt);

        // 비동기 처리 시작
        processAndSaveTTSVector(email, ttsVector);

        return ResponseEntity.accepted().body(Map.of(
                "status", "processing",
                "message", "TTS Vector processing started"
        ));
    }
}