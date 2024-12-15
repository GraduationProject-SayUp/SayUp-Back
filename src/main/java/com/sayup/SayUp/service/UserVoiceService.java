package com.sayup.SayUp.service;

import com.sayup.SayUp.entity.User;
import com.sayup.SayUp.entity.UserVoice;
import com.sayup.SayUp.repository.UserRepository;
import com.sayup.SayUp.repository.UserVoiceRepository;
import com.sayup.SayUp.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class UserVoiceService {
    private static final Logger logger = LoggerFactory.getLogger(UserVoiceService.class);

    private final UserVoiceRepository userVoiceRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RestTemplate restTemplate;

    @Value("${file.upload-dir}") // 파일 저장 위치
    private String uploadDir;

    @Value("${python.server.url}") // Python 서버 URL
    private String pythonServerUrl;

    public ResponseEntity<String> uploadFile(String token, MultipartFile file) {
        try {
            if (file.isEmpty()) {
                logger.warn("Upload failed: No file attached.");
                return ResponseEntity.badRequest().body("File is empty");
            }

            // JWT 토큰에서 이메일 추출
            String email = jwtTokenProvider.getUsernameFromToken(token.substring(7));
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

            // 파일 저장
            String originalFileName = file.getOriginalFilename();
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("Upload directory created at: {}", uploadPath);
            }

            Path destination = Paths.get(uploadDir, originalFileName);
            file.transferTo(destination.toFile());
            logger.info("File saved at: {}", destination);

            // 기존 UserVoice 확인
            UserVoice userVoice = userVoiceRepository.findByUser(user)
                    .orElse(new UserVoice()); // 없으면 새로 생성

            // UserVoice 업데이트 또는 새로 설정
            userVoice.setUser(user);
            userVoice.setFileName(originalFileName);
            userVoice.setFilePath(destination.toString());
            userVoiceRepository.save(userVoice);

            logger.info("File information saved to DB for user: {}", email);
            // 비동기로 파이썬 서버 호출
            processFileAsync(token, destination.toString(), user);

            return ResponseEntity.ok("File upload successful");
        } catch (IOException e) {
            logger.error("Error saving file", e);
            return ResponseEntity.internalServerError().body("File save failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
            return ResponseEntity.internalServerError().body("Unexpected error: " + e.getMessage());
        }
    }

    @Async
    public CompletableFuture<Void> processFileAsync(String token, String filePath, User user) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 파이썬 서버 호출
                String pythonResponse = sendToPythonServer(token, filePath);

                logger.info("Python server response: {}", pythonResponse);

            } catch (Exception e) {
                logger.error("Error processing file asynchronously", e);
            }
        });
    }

    private String sendToPythonServer(String token, String filePath) {
        try {
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Authorization", token);

            // 요청 본문 설정
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(filePath));

            // HTTP 요청 생성
            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            String pythonEndpoint = pythonServerUrl + "/upload";

            // Python 서버에 POST 요청
            ResponseEntity<String> response = restTemplate.postForEntity(pythonEndpoint, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                logger.info("Python server response received successfully.");
                return response.getBody();
            } else {
                logger.warn("Python server returned non-200 status: {}", response.getStatusCode());
                throw new RuntimeException("Python server error: " + response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error sending file to Python server", e);
            throw new RuntimeException("Error sending file to Python server: " + e.getMessage(), e);
        }
    }
}