package com.sayup.SayUp.controller;

import com.sayup.SayUp.repository.UserVoiceRepository;
import com.sayup.SayUp.service.UserVoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// 사용자 음성 파일 업로드 컨트롤러
// 클라이언트에서 음성 파일을 업로드하고, 이를 비동기적으로 처리하여 TTS 분석을 위한 데이터를 생성

@RestController
@RequestMapping("/api/users/audio")
public class UserAudioUploadController {
    private static final Logger logger = LoggerFactory.getLogger(UserAudioUploadController.class);
    private final UserVoiceService userVoiceService;

    public UserAudioUploadController(UserVoiceService userVoiceService, UserVoiceRepository userVoiceRepository) {
        this.userVoiceService = userVoiceService;
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<String> uploadFile(
            @RequestHeader("Authorization") String token,
            @RequestParam("file")MultipartFile file){
        logger.info("File upload request received.");

        if (file.isEmpty()) {
            logger.warn("No file attached in the request.");
            return ResponseEntity.badRequest().body("File is required.");
        }

        try {
            ResponseEntity<String> response = userVoiceService.uploadFile(token, file);
            logger.info("File upload completed successfully.");
            return response;
        } catch (Exception e) {
            logger.error("Error occurred while uploading file.", e);
            return ResponseEntity.internalServerError().body("Error occurred while uploading file.");
        }
    }
}
