package com.sayup.SayUp.kakao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)  // JSON에서 이 클래스에 정의되지 않은 필드가 있어도 무시
public class KakaoTokenResponseDto {

    @JsonProperty("token_type")
    public String tokenType;

    @JsonProperty("access_token")
    public String accessToken;

    @JsonProperty("id_token")
    public String idToken;

    @JsonProperty("expires_in")
    public Integer expiresIn;

    @JsonProperty("refresh_token")
    public String refreshToken;

    @JsonProperty("refresh_token_expires_in")
    public Integer refreshTokenExpiresIn;

    @JsonProperty("scope")
    public String scope;
}