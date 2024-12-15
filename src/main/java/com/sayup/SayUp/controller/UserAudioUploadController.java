package com.sayup.SayUp.controller;

import com.sayup.SayUp.repository.UserVoiceRepository;
import com.sayup.SayUp.service.UserVoiceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// 음성 파일을 저장 후 파이썬 비동기화 호출

@RestController
@RequestMapping("/api/audio")
public class UserVoiceController {
    private static final Logger logger = LoggerFactory.getLogger(UserVoiceController.class);
    private final UserVoiceService userVoiceService;

    public UserVoiceController(UserVoiceService userVoiceService, UserVoiceRepository userVoiceRepository) {
        this.userVoiceService = userVoiceService;
    }

    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
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
