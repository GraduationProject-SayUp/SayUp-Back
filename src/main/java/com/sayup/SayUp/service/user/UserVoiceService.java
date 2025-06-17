package com.sayup.SayUp.service.user;

import com.sayup.SayUp.entity.user.User;
import com.sayup.SayUp.entity.user.UserVoice;
import com.sayup.SayUp.repository.UserRepository;
import com.sayup.SayUp.repository.UserVoiceRepository;
import com.sayup.SayUp.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class UserVoiceService {

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
            // 입력 검증
            if (token == null || !token.startsWith("Bearer ")) {
                log.warn("Invalid authorization header format");
                return ResponseEntity.badRequest().body("Invalid authorization header format");
            }

            if (file == null || file.isEmpty()) {
                log.warn("Upload failed: No file attached or file is empty");
                return ResponseEntity.badRequest().body("File is required and cannot be empty");
            }

            // JWT 토큰에서 이메일 추출
            String actualToken = token.substring(7);
            if (!jwtTokenProvider.validateToken(actualToken)) {
                log.warn("Invalid or expired token during file upload");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
            }

            String email = jwtTokenProvider.getUsernameFromToken(actualToken);
            if (email == null || email.trim().isEmpty()) {
                log.warn("Invalid email from token during file upload");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token content");
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

            // 사용자 활성 상태 확인
            if (!user.getIsActive()) {
                log.warn("Inactive user attempted file upload: {}", email);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Account is not active");
            }

            // 파일 저장
            String originalFileName = file.getOriginalFilename();
            if (originalFileName == null || originalFileName.trim().isEmpty()) {
                log.warn("Invalid file name provided");
                return ResponseEntity.badRequest().body("Invalid file name");
            }

            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Upload directory created at: {}", uploadPath);
            }

            Path destination = Paths.get(uploadDir, originalFileName);
            file.transferTo(destination.toFile());
            log.info("File saved at: {} for user: {}", destination, email);

            // 기존 UserVoice 확인
            UserVoice userVoice = userVoiceRepository.findByUser(user)
                    .orElse(new UserVoice()); // 없으면 새로 생성

            // UserVoice 업데이트 또는 새로 설정
            userVoice.setUser(user);
            userVoice.setFileName(originalFileName);
            userVoice.setFilePath(destination.toString());
            userVoiceRepository.save(userVoice);

            log.info("File information saved to DB for user: {}", email);
            
            // 비동기로 파이썬 서버 호출
            processFileAsync(token, destination.toString(), user);

            return ResponseEntity.ok("File upload successful");
        } catch (UsernameNotFoundException e) {
            log.error("User not found during file upload: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        } catch (IOException e) {
            log.error("Error saving file: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("File save failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error occurred during file upload: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error: " + e.getMessage());
        }
    }

    @Async
    public CompletableFuture<Void> processFileAsync(String token, String filePath, User user) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 파이썬 서버 호출
                String pythonResponse = sendToPythonServer(token, filePath);
                log.info("Python server response for user {}: {}", user.getEmail(), pythonResponse);
            } catch (Exception e) {
                log.error("Error processing file asynchronously for user {}: {}", user.getEmail(), e.getMessage());
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
                log.info("Python server response received successfully");
                return response.getBody();
            } else {
                log.warn("Python server returned non-200 status: {}", response.getStatusCode());
                throw new IllegalStateException("Python server error: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error sending file to Python server: {}", e.getMessage());
            throw new IllegalStateException("Error sending file to Python server: " + e.getMessage(), e);
        }
    }
}
