package com.sayup.SayUp.kakao.controller;

import com.sayup.SayUp.dto.AuthResponseDTO;
import com.sayup.SayUp.kakao.dto.KakaoUserInfoResponseDto;
import com.sayup.SayUp.kakao.exception.KakaoApiException;
import com.sayup.SayUp.kakao.service.KakaoService;
import com.sayup.SayUp.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth/kakao")
public class KakaoCallbackController {

    private final KakaoService kakaoService;
    private final AuthService authService;

    @GetMapping("/callback")
    public ResponseEntity<?> callback(@RequestParam("code") String code) {
        try {
            log.info("Kakao login callback received with code: {}", code.substring(0, Math.min(10, code.length())));
            
            // 카카오 액세스 토큰 획득
            String accessToken = kakaoService.getAccessToken(code).getAccessToken();
            
            // 카카오 사용자 정보 획득
            KakaoUserInfoResponseDto userInfo = kakaoService.getUserInfo(accessToken);
            
            // 이메일 검증
            if (userInfo.getKakaoAccount() == null || 
                userInfo.getKakaoAccount().getEmail() == null ||
                userInfo.getKakaoAccount().getEmail().trim().isEmpty()) {
                
                log.warn("Kakao login failed: Email not provided by user");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "이메일 정보 제공에 동의해주세요."));
            }

            String email = userInfo.getKakaoAccount().getEmail();
            
            // 이메일 인증 여부 확인
            if (userInfo.getKakaoAccount().getIsEmailVerified() != null && 
                !userInfo.getKakaoAccount().getIsEmailVerified()) {
                
                log.warn("Kakao login failed: Email not verified for user: {}", email);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("error", "인증된 이메일을 사용해주세요."));
            }

            // 사용자 로그인/등록 처리
            AuthResponseDTO response = authService.kakaoLogin(email);
            
            log.info("Kakao login successful for user: {}", email);
            
            return ResponseEntity.ok(response);
            
        } catch (KakaoApiException e) {
            log.error("Kakao API error during login: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "카카오 로그인 중 오류가 발생했습니다. 다시 시도해주세요."));
        } catch (Exception e) {
            log.error("Unexpected error during Kakao login: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "로그인 처리 중 오류가 발생했습니다."));
        }
    }
}
