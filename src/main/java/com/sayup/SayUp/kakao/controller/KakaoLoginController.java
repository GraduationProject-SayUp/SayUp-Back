package com.sayup.SayUp.kakao.controller;

import com.sayup.SayUp.dto.AuthResponseDTO;
import com.sayup.SayUp.kakao.dto.KakaoUserInfoResponseDto;
import com.sayup.SayUp.kakao.service.KakaoService;
import com.sayup.SayUp.security.JwtTokenProvider;
import com.sayup.SayUp.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("")
public class KakaoLoginController {

    private final KakaoService kakaoService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;

    @GetMapping("/callback")
    public ResponseEntity<?> callback(@RequestParam("code") String code) throws IOException {
        String accessToken = kakaoService.getAccessTokenFromKakao(code);

        KakaoUserInfoResponseDto userInfo = kakaoService.getUserInfo(accessToken);

        String email = null;
        String jwt = null;
        if (userInfo.getKakaoAccount() != null) {
            email = userInfo.getKakaoAccount().getEmail();
            authService.loadOrCreateUser(email);
            jwt = jwtTokenProvider.createTokenFromEmail(email);
        }

        return ResponseEntity.ok(new AuthResponseDTO(jwt, email));
    }
}
