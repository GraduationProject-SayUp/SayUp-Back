package com.sayup.SayUp.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponseDTO {
    private String accessToken;
    private String tokenType;
    private Long expiresIn;
    private String refreshToken;
    private UserInfo userInfo;
    
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class UserInfo {
        private Long userId;
        private String email;
        private String username;
        private String role;
    }
}
