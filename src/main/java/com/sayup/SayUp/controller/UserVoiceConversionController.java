package com.sayup.SayUp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sayup.SayUp.entity.User;
import com.sayup.SayUp.repository.UserRepository;
import com.sayup.SayUp.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/voice/conversion")
@RequiredArgsConstructor
public class UserVoiceConversionController {

    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${python.server.url}")
    private String pythonServerUrl; // Python 서버 URL

    @PostMapping
    public ResponseEntity<?> convertVoice(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> requestBody) {

        try {
            // 토큰 검증
            if (token == null || !token.startsWith("Bearer ")) {
                log.warn("Invalid authorization header format");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid authorization header format"));
            }

            String actualToken = token.substring(7);
            if (!jwtTokenProvider.validateToken(actualToken)) {
                log.warn("Invalid or expired token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid or expired token"));
            }

            // JWT 토큰에서 사용자 이메일 검증
            String email = jwtTokenProvider.getUsernameFromToken(actualToken);
            if (email == null || email.trim().isEmpty()) {
                log.warn("Invalid email from token");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid token content"));
            }

            // 사용자 정보 조회
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

            // 사용자 활성 상태 확인
            if (!user.getIsActive()) {
                log.warn("Inactive user attempted voice conversion: {}", email);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Account is not active"));
            }

            // 벡터 확인 (DB에서 가져옴)
            String vector = user.getTtsVector();
            if (vector == null || vector.trim().isEmpty()) {
                log.warn("No TTS vector found for user: {}", email);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "error", "No TTS vector found for the user. Please upload your voice sample first."
                ));
            }

            // 요청 데이터 검증
            String sentence = requestBody.get("sentence");
            if (sentence == null || sentence.trim().isEmpty()) {
                log.warn("Empty sentence provided for voice conversion");
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Sentence is required and cannot be empty."
                ));
            }

            // 입력 길이 제한
            if (sentence.length() > 1000) {
                log.warn("Sentence too long for user: {}, length: {}", email, sentence.length());
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Sentence is too long. Maximum 1000 characters allowed."
                ));
            }

            log.info("Processing voice conversion for user: {}", email);

            // Python 서버로 HTTP 요청
            String pythonEndpoint = pythonServerUrl + "/convert";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", token);

            String vectorJson = objectMapper.writeValueAsString(objectMapper.readValue(vector, Object.class));

            Map<String, String> request = Map.of(
                    "text", sentence.trim(),
                    "vector", vectorJson
            );
            HttpEntity<Map<String, String>> entity = new HttpEntity<>(request, headers);

            // 파일 스트림 요청
            ResponseEntity<byte[]> pythonResponse = restTemplate.exchange(
                    pythonEndpoint,
                    HttpMethod.POST,
                    entity,
                    byte[].class
            );

            if (pythonResponse.getStatusCode().is2xxSuccessful() && pythonResponse.getBody() != null) {
                log.info("Voice conversion completed successfully for user: {}", email);
                
                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType("audio/wav"))
                        .header("Content-Disposition", "attachment; filename=\"converted_voice.wav\"")
                        .body(pythonResponse.getBody());
            } else {
                log.error("Python server returned error status: {} for user: {}", pythonResponse.getStatusCode(), email);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "Failed to process voice conversion. Please try again later."));
            }

        } catch (UsernameNotFoundException e) {
            log.error("User not found during voice conversion: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "User not found"));
        } catch (Exception ex) {
            log.error("Error during voice conversion: ", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Voice conversion failed. Please try again later."
            ));
        }
    }
}
