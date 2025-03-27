package com.sayup.SayUp.kakao.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/kakao")
public class KakaoRedirectController {

    @Value("${kakao.client_id}")
    private String clientId;

    @Value("${kakao.redirect_uri}")
    private String redirectUri;

    @GetMapping("/login-url")
    public ResponseEntity<Void> redirectToKakaoLogin() {
        String kakaoLoginUrl = String.format(
                "https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=%s&redirect_uri=%s",
                clientId, redirectUri
        );

        return ResponseEntity.status(302)
                .location(URI.create(kakaoLoginUrl))
                .build();
    }
}
