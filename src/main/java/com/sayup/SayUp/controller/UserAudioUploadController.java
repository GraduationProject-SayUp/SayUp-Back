package com.sayup.SayUp.controller;

import com.sayup.SayUp.repository.UserVoiceRepository;
import com.sayup.SayUp.service.UserVoiceService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

// 사용자 음성 파일 업로드 컨트롤러
// 클라이언트에서 음성 파일을 업로드하고, 이를 비동기적으로 처리하여 TTS 분석을 위한 데이터를 생성

@RestController
@RequestMapping("/api/users/audio")
@AllArgsConstructor
public class UserAudioUploadController {
    private final UserVoiceService userVoiceService;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<String> uploadFile(@RequestHeader("Authorization") String token, @RequestParam("file")MultipartFile file){
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is required.");
        }

        return userVoiceService.uploadFile(token, file);
    }
}
