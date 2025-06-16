package com.sayup.SayUp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Slf4j
@Service
public class OpenaiService {
    
    @Value("${openai.api.url}")
    private String openaiApiUrl;

    @Value("${openai.api.key}")
    private String openaiApiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenaiService(RestTemplateBuilder builder, ObjectMapper objectMapper) {
        this.restTemplate = builder.build();
        this.objectMapper = objectMapper;
    }

    public String getChatResponse(String userMessage) {
        try {
            // 입력 검증
            if (userMessage == null || userMessage.trim().isEmpty()) {
                log.warn("Empty user message received");
                return "메시지를 입력해주세요.";
            }

            // 입력 길이 제한
            if (userMessage.length() > 2000) {
                log.warn("User message too long: {} characters", userMessage.length());
                return "메시지가 너무 깁니다. 2000자 이내로 입력해주세요.";
            }

            // API 키 검증
            if (openaiApiKey == null || openaiApiKey.trim().isEmpty()) {
                log.error("OpenAI API key is not configured");
                return "서비스 설정 오류가 발생했습니다. 관리자에게 문의해주세요.";
            }

            // 한국어 학습용 프롬프트 최적화
            String systemMessage = "You are a helpful Korean language tutor. Assist users with learning Korean. " +
                    "Your responses should be clear, polite, and tailored to help a non-native speaker improve their Korean. " +
                    "Include explanations when needed, use simple sentences, and provide translations. " +
                    "Always respond in Korean unless specifically asked to use another language.";

            // 요청 Body 설정
            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-3.5-turbo",
                    "messages", Arrays.asList(
                            Map.of("role", "system", "content", systemMessage),
                            Map.of("role", "user", "content", userMessage.trim())
                    ),
                    "temperature", 0.7,
                    "max_tokens", 1000
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("Sending request to OpenAI API for user message: {}", userMessage.substring(0, Math.min(50, userMessage.length())));

            // OpenAI API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                    openaiApiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // JSON 응답에서 content만 추출
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.path("choices");
                
                if (choices.isArray() && choices.size() > 0) {
                    String content = choices.get(0).path("message").path("content").asText();
                    log.info("OpenAI API response received successfully");
                    return content;
                } else {
                    log.error("Invalid response format from OpenAI API");
                    return "응답 처리 중 오류가 발생했습니다. 다시 시도해주세요.";
                }
            } else {
                log.error("OpenAI API returned error status: {}", response.getStatusCode());
                return "서비스 일시적 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
            }
        } catch (Exception e) {
            log.error("Error calling OpenAI API: ", e);
            return "서비스 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
        }
    }
}
