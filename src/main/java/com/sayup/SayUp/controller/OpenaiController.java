package com.sayup.SayUp.controller;

import com.sayup.SayUp.security.JwtTokenProvider;
import com.sayup.SayUp.service.OpenaiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class OpenaiController {

    private final OpenaiService openAIService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping(value = "/generate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> generateChatResponse(
            @RequestHeader("Authorization") String token,
            @RequestBody String userMessage) {

        // 토큰 값 검증
        if (token == null || !token.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body("Invalid authorization header");
        }

        String actualToken = token.substring(7);

        // 토큰이 유효한지 검증
        if (!jwtTokenProvider.validateToken(actualToken)) {
            return ResponseEntity.status(401).body("Invalid or expired token");
        }

        log.info("Chat request from user: {}", jwtTokenProvider.getUsernameFromToken(actualToken));

        // 토큰 검증 후 사용자 메시지 처리
        String content = openAIService.getChatResponse(userMessage);
        return ResponseEntity.ok(content);
    }
}
