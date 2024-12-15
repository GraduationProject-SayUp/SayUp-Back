package com.sayup.SayUp.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class OpenaiService {
    @Value("${openai.api.url}")
    private String openaiApiUrl;

    @Value("${openai.api.key}")
    private String openaiApiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public OpenaiService(RestTemplateBuilder builder, ObjectMapper objectMapper) {
        this.restTemplate = builder.build();
        this.objectMapper = objectMapper;
    }

    public String getChatResponse(String userMessage) {
        try {
            // 한국어 학습용 프롬프트 최적화
            String systemMessage = "You are a helpful Korean language tutor. Assist users with learning Korean. " +
                    "Your responses should be clear, polite, and tailored to help a non-native speaker improve their Korean. " +
                    "Include explanations when needed, use simple sentences, and provide translations.";

            // 요청 Body 설정
            Map<String, Object> requestBody = Map.of(
                    "model", "gpt-3.5-turbo",
                    "messages", Arrays.asList(
                            Map.of("role", "system", "content", systemMessage),
                            Map.of("role", "user", "content", userMessage)
                    ),
                    "temperature", 0.7
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // OpenAI API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                    openaiApiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                // JSON 응답에서 content만 추출
                JsonNode root = objectMapper.readTree(response.getBody());
                String content = root.path("choices")
                        .get(0)
                        .path("message")
                        .path("content")
                        .asText();

                return content;
            } else {
                throw new RuntimeException("OpenAI API failed: " + response.getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred while calling OpenAI API.";
        }
    }
}
