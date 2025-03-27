package com.sayup.SayUp.kakao.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        String kakaoLoginUrl = "https://kauth.kakao.com/oauth/authorize"
                + "?response_type=code"
                + "&client_id=" + clientId
                + "&redirect_uri=" + redirectUri;

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", kakaoLoginUrl);

        return ResponseEntity.status(302) // HTTP 302 Found (Redirect)
                .headers(headers)
                .build();
    }
}
