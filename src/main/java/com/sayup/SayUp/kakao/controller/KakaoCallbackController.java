package com.sayup.SayUp.kakao.controller;

import com.sayup.SayUp.dto.AuthResponseDTO;
import com.sayup.SayUp.entity.User;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/kakao")
public class KakaoCallbackController {

    private final KakaoService kakaoService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;

    @GetMapping("/callback")
    public ResponseEntity<AuthResponseDTO> callback(@RequestParam("code") String code) {
        String accessToken = kakaoService.getAccessTokenFromKakao(code);

        KakaoUserInfoResponseDto userInfo = kakaoService.getUserInfo(accessToken);

        if (userInfo.getKakaoAccount() == null || userInfo.getKakaoAccount().getEmail() == null) {
            return ResponseEntity.badRequest().body(new AuthResponseDTO(null, null, null));
        }

        String email = userInfo.getKakaoAccount().getEmail();
        User user = authService.loadOrCreateUser(email);
        String jwt = jwtTokenProvider.createTokenFromEmail(email);

        AuthResponseDTO response = new AuthResponseDTO(jwt, email, String.valueOf(user.getUserId()));
        return ResponseEntity.ok(response);
    }
}
