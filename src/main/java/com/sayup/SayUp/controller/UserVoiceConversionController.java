package com.sayup.SayUp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sayup.SayUp.entity.User;
import com.sayup.SayUp.repository.UserRepository;
import com.sayup.SayUp.security.JwtTokenProvider;
import com.sayup.SayUp.service.UserVoiceService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

// 서비스로 분리해야 할 것 같은데 일단 보류 ...

@RestController
@RequestMapping("/api/voice/conversion")
public class UserVoiceConversionController {

    private static final Logger logger = LoggerFactory.getLogger(UserVoiceService.class);

    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;


    @Value("${python.server.url}")
    private String pythonServerUrl; // Python 서버 URL

    public UserVoiceConversionController(JwtTokenProvider jwtTokenProvider, RestTemplate restTemplate, UserRepository userRepository, ObjectMapper objectMapper) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.restTemplate = restTemplate;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<?> convertVoice(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> requestBody) {

        try {
            // JWT 토큰에서 사용자 이메일 검증
            String email = jwtTokenProvider.getUsernameFromToken(token.substring(7));

            // 사용자 정보 조회
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

            // 벡터 확인 (DB에서 가져옴)
            String vector = user.getTtsVector();
            if (vector == null || vector.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "status", "error",
                        "message", "No TTS vector found for the user."
                ));
            }

            // 요청 데이터 확인
            String sentence = requestBody.get("sentence");

            if (sentence == null || sentence.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", "error",
                        "message", "Both 'sentence' and 'vector' are required."
                ));
            }

            logger.info("Sending request to Python server for voice conversion.");

            // Python 서버로 HTTP 요청
            String pythonEndpoint = pythonServerUrl + "/convert";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", token); // Authorization 헤더 추가

            ObjectMapper objectMapper = new ObjectMapper();
            String vectorJson = objectMapper.writeValueAsString(objectMapper.readValue(vector, Object.class)); // JSON 문자열로 변환

            logger.info("Sending vector: " + vectorJson);

            Map<String, String> request = Map.of(
                    "text", sentence,
                    "vector", vectorJson // JSON으로 변환된 벡터 값
            );
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            // 파일 스트림 요청
            ResponseEntity<byte[]> pythonResponse = restTemplate.exchange(
                    pythonEndpoint,
                    HttpMethod.POST,
                    entity,
                    byte[].class
            );

            if (pythonResponse.getStatusCode().is2xxSuccessful()) {
                logger.info("Voice conversion completed, returning audio file.");

                // 파일 반환
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("audio/wav"))
                        .body(pythonResponse.getBody());
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("status", "error", "message", "Failed to process voice conversion."));
            }

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", "error",
                    "message", "Voice conversion request failed."
            ));
        }
    }
}