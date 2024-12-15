package com.sayup.SayUp.service;

import com.sayup.SayUp.entity.User;
import com.sayup.SayUp.entity.UserVoice;
import com.sayup.SayUp.repository.UserRepository;
import com.sayup.SayUp.repository.UserVoiceRepository;
import com.sayup.SayUp.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class UserVoiceService {
    private static final Logger logger = LoggerFactory.getLogger(UserVoiceService.class);

    private final UserVoiceRepository userVoiceRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${file.upload-dir}") //파일이 저장되게 될 위치
    private String uploadDir;

    public ResponseEntity<String> uploadFile(
            String token,
            @RequestParam("file")MultipartFile file
    ){
        try{
            if(file.isEmpty()){
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
                logger.info("Created upload directory at: {}", uploadPath);
            }

            Path destination = Paths.get(uploadDir, originalFileName);
            file.transferTo(destination.toFile());
            logger.info("File saved at: {}", destination);

            // DB에 파일 정보 저장
            UserVoice userVoice = new UserVoice();
            userVoice.setUser(user);
            userVoice.setFileName(originalFileName);
            userVoice.setFilePath(destination.toString());
            userVoiceRepository.save(userVoice);

            logger.info("File information saved to DB for user: {}", email);
            return ResponseEntity.ok("File saved successfully: " + destination.toString());
        } catch (IOException e) {
            logger.error("Error saving file", e);
            return ResponseEntity.internalServerError().body("File save failed: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error occurred", e);
            return ResponseEntity.internalServerError().body("Unexpected error: " + e.getMessage());
        }
    }
}
