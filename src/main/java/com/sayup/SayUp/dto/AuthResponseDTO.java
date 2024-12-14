package com.sayup.SayUp.dto;

import lombok.*;

@Data
public class AuthResponseDTO {
    private String token;
    private String email;

    public AuthResponseDTO(String token, String email) {
        this.token = token;
        this.email = email;
    }
}
