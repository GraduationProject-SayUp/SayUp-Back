package com.sayup.SayUp.kakao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class KakaoUserInfoResponseDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("connected_at")
    private Date connectedAt;

    @JsonProperty("properties")
    private Map<String, String> properties;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KakaoAccount {

        @JsonProperty("profile")
        private Profile profile;

        @JsonProperty("email")
        private String email;

        @JsonProperty("is_email_verified")
        private Boolean isEmailVerified;

        @JsonProperty("name")
        private String name;

        @JsonProperty("phone_number")
        private String phoneNumber;

        @JsonProperty("gender")
        private String gender;

        @JsonProperty("birthyear")
        private String birthYear;

        @JsonProperty("birthday")
        private String birthDay;

        @JsonProperty("age_range")
        private String ageRange;

        @Getter
        @NoArgsConstructor
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Profile {

            @JsonProperty("nickname")
            private String nickname;

            @JsonProperty("thumbnail_image_url")
            private String thumbnailImageUrl;

            @JsonProperty("profile_image_url")
            private String profileImageUrl;

            @JsonProperty("is_default_image")
            private Boolean isDefaultImage;
        }
    }

    /**
     * 사용자 이메일 추출 (우선순위: kakao_account.email > properties.email)
     */
    public String getEmail() {
        if (kakaoAccount != null && kakaoAccount.getEmail() != null) {
            return kakaoAccount.getEmail();
        }
        if (properties != null && properties.containsKey("email")) {
            return properties.get("email");
        }
        return null;
    }

    /**
     * 사용자 닉네임 추출 (우선순위: kakao_account.profile.nickname > properties.nickname)
     */
    public String getNickname() {
        if (kakaoAccount != null && kakaoAccount.getProfile() != null) {
            String profileNickname = kakaoAccount.getProfile().getNickname();
            if (profileNickname != null) {
                return profileNickname;
            }
        }
        if (properties != null && properties.containsKey("nickname")) {
            return properties.get("nickname");
        }
        return null;
    }

    /**
     * 사용자 프로필 이미지 URL 추출
     */
    public String getProfileImageUrl() {
        if (kakaoAccount != null && kakaoAccount.getProfile() != null) {
            return kakaoAccount.getProfile().getProfileImageUrl();
        }
        return null;
    }

    /**
     * 사용자 정보 유효성 검증
     */
    public boolean isValid() {
        return id != null && getEmail() != null;
    }

    /**
     * 이메일 인증 여부 확인
     */
    public boolean isEmailVerified() {
        return kakaoAccount != null && 
               kakaoAccount.getIsEmailVerified() != null && 
               kakaoAccount.getIsEmailVerified();
    }
}
