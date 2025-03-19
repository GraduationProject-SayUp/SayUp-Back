package com.sayup.SayUp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@AllArgsConstructor
public class AuthRequestDTO {
    @NotBlank(message = "Email cannot be blank")
    @Pattern(regexp="^[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])*@[0-9a-zA-Z]([-_.]?[0-9a-zA-Z])+[.][a-zA-Z]{2,3}$", message="이메일 주소 양식을 확인해주세요")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 4, message = "Password must be at least 4 characters long")
    private String password;
}


