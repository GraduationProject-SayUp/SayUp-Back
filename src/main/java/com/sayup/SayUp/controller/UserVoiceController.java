package com.sayup.SayUp.controller;

import com.sayup.SayUp.entity.User;
import com.sayup.SayUp.repository.UserVoiceRepository;
import com.sayup.SayUp.service.UserVoiceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/audio")
public class UserVoiceController {

    private final UserVoiceService userVoiceService;

    public UserVoiceController(UserVoiceService userVoiceService, UserVoiceRepository userVoiceRepository) {
        this.userVoiceService = userVoiceService;
    }

    @PostMapping(value = "/upload", consumes = {"multipart/form-data"})
    public ResponseEntity<String> uploadFile(User user, @RequestParam("file")MultipartFile file){
        System.out.println("Request received to upload file");
        return userVoiceService.uploadFile(user,file);
    }
}
