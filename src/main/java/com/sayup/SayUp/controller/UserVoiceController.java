package com.sayup.SayUp.controller;

import com.sayup.SayUp.entity.User;
import com.sayup.SayUp.repository.UserVoiceRepository;
import com.sayup.SayUp.service.UserVoiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/audio")
public class UserVoiceController {

    private final UserVoiceService userVoiceService;

    public UserVoiceController(UserVoiceService userVoiceService, UserVoiceRepository userVoiceRepository) {
        this.userVoiceService = userVoiceService;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(User user, @RequestParam("file")MultipartFile file){
        return userVoiceService.uploadFile(user,file);
    }
}
