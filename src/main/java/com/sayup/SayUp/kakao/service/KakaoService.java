package com.sayup.SayUp.kakao.service;

import com.sayup.SayUp.kakao.dto.KakaoTokenResponseDto;
import com.sayup.SayUp.kakao.dto.KakaoUserInfoResponseDto;
import com.sayup.SayUp.kakao.exception.KakaoApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
public class KakaoService {

    private static final String KAUTH_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAPI_USER_URL = "https://kapi.kakao.com/v2/user/me";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final String clientId;
    private final WebClient webClient;

    public KakaoService(@Value("${kakao.client_id}") String clientId) {
        this.clientId = clientId;
        this.webClient = WebClient.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
                .build();
    }

    /**
     * 카카오 인증 코드로 액세스 토큰 발급
     */
    public KakaoTokenResponseDto getAccessToken(String code) {
        validateCode(code);

        try {
            log.info("Requesting Kakao access token with code: {}...", 
                    StringUtils.truncate(code, 10));

            KakaoTokenResponseDto response = webClient.post()
                    .uri(KAUTH_TOKEN_URL)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .bodyValue(buildTokenRequest(code))
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, 
                            this::handleClientError)
                    .onStatus(HttpStatusCode::is5xxServerError, 
                            this::handleServerError)
                    .bodyToMono(KakaoTokenResponseDto.class)
                    .timeout(TIMEOUT)
                    .block();

            validateTokenResponse(response);
            log.info("Kakao access token obtained successfully");
            return response;

        } catch (Exception e) {
            log.error("Error obtaining Kakao access token: {}", e.getMessage());
            throw new KakaoApiException("Failed to authenticate with Kakao: " + e.getMessage());
        }
    }

    /**
     * 액세스 토큰으로 카카오 사용자 정보 조회
     */
    public KakaoUserInfoResponseDto getUserInfo(String accessToken) {
        validateAccessToken(accessToken);

        try {
            log.info("Requesting Kakao user info");

            KakaoUserInfoResponseDto response = webClient.get()
                    .uri(KAPI_USER_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, 
                            this::handleClientError)
                    .onStatus(HttpStatusCode::is5xxServerError, 
                            this::handleServerError)
                    .bodyToMono(KakaoUserInfoResponseDto.class)
                    .timeout(TIMEOUT)
                    .block();

            validateUserInfoResponse(response);
            log.info("Kakao user info obtained successfully for user ID: {}", response.getId());
            return response;

        } catch (Exception e) {
            log.error("Error obtaining Kakao user info: {}", e.getMessage());
            throw new KakaoApiException("Failed to get user info from Kakao: " + e.getMessage());
        }
    }

    /**
     * 카카오 로그인 처리 (토큰 발급 + 사용자 정보 조회)
     */
    public KakaoUserInfoResponseDto processKakaoLogin(String code) {
        KakaoTokenResponseDto tokenResponse = getAccessToken(code);
        return getUserInfo(tokenResponse.getAccessToken());
    }

    // Private helper methods

    private void validateCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("Authorization code is required");
        }
    }

    private void validateAccessToken(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalArgumentException("Access token is required");
        }
    }

    private void validateTokenResponse(KakaoTokenResponseDto response) {
        if (response == null || !response.isValid()) {
            throw new KakaoApiException("Failed to obtain valid access token from Kakao");
        }
    }

    private void validateUserInfoResponse(KakaoUserInfoResponseDto response) {
        if (response == null || !response.isValid()) {
            throw new KakaoApiException("Failed to obtain valid user info from Kakao");
        }
    }

    private String buildTokenRequest(String code) {
        return String.format("grant_type=authorization_code&client_id=%s&code=%s", 
                clientId, code);
    }

    private Mono<? extends Throwable> handleClientError(org.springframework.web.reactive.function.client.ClientResponse response) {
        return Mono.error(new KakaoApiException("Kakao API client error: " + response.statusCode()));
    }

    private Mono<? extends Throwable> handleServerError(org.springframework.web.reactive.function.client.ClientResponse response) {
        return Mono.error(new KakaoApiException("Kakao API server error: " + response.statusCode()));
    }
}
