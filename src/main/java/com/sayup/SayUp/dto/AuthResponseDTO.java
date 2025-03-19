package com.sayup.SayUp.dto;

import lombok.*;

@Data
@AllArgsConstructor
public class AuthResponseDTO {
    private final String token;
    private final String email;
}
