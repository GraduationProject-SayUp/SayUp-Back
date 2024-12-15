package com.sayup.SayUp.controller;

import com.sayup.SayUp.service.OpenaiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class OpenaiController {

    private final OpenaiService openAIService;

    @Autowired
    public OpenaiController(OpenaiService openAIService) {
        this.openAIService = openAIService;
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateChatResponse(@RequestParam("message") String userMessage) {
        String content = openAIService.getChatResponse(userMessage);
        return ResponseEntity.ok(content); // content 값만 반환
    }
}
