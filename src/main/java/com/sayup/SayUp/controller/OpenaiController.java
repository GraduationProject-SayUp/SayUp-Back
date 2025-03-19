package com.sayup.SayUp.controller;

import com.sayup.SayUp.security.JwtTokenProvider;
import com.sayup.SayUp.service.OpenaiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class OpenaiController {

    private final OpenaiService openAIService;
    private final JwtTokenProvider jwtTokenProvider;

    @Autowired
    public OpenaiController(OpenaiService openAIService, JwtTokenProvider jwtTokenProvider) {
        this.openAIService = openAIService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @PostMapping(value = "/generate",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> generateChatResponse(
            @RequestHeader("Authorization") String token,
            @RequestBody String userMessage) {

        // 토큰 값 검증
        if (token.startsWith("Bearer ")) {
            token = token.substring(7); // "Bearer " 제거
        }

        // 토큰이 유효한지 검증
        if (!jwtTokenProvider.validateToken(token)) {
            return ResponseEntity.status(401).body("Invalid or expired token");
        }

        // 토큰 검증 후 사용자 메시지 처리
        String content = openAIService.getChatResponse(userMessage);
        return ResponseEntity.ok(content); // content 값만 반환
    }
}
