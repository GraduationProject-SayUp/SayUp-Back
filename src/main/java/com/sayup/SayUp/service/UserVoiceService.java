package com.sayup.SayUp.service;

import com.sayup.SayUp.entity.User;
import com.sayup.SayUp.entity.UserVoice;
import com.sayup.SayUp.repository.UserVoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.AudioFileFormat;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class UserVoiceService {
    private final UserVoiceRepository userVoiceRepository;

    @Value("$(file.upload-dir)") //파일이 저장되게 될 위치
    private String uploadDir;

    public ResponseEntity<String> uploadFile(User user, @RequestParam("file")MultipartFile file){
        try{
            if(file.isEmpty()){
                return ResponseEntity.badRequest().body("File is empty");
            }

            String originalFileName = file.getOriginalFilename();
            Path uploadPath = Paths.get(uploadDir);
            if (!uploadPath.toFile().exists()){
                uploadPath.toFile().mkdirs();
            }

            Path destination = Paths.get(uploadDir,originalFileName);
            file.transferTo(destination.toFile());

            UserVoice userVoice = new UserVoice();
            userVoice.setUser(user);
            userVoice.setFileName(originalFileName);
            userVoice.setFilePath(destination.toString());
            userVoiceRepository.save(userVoice);

            return ResponseEntity.ok("File save successfully :" + destination.toString());

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("File save fail");
        }
    }
}
