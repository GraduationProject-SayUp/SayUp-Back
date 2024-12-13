package com.sayup.SayUp.controller;

import com.sayup.SayUp.security.JwtTokenProvider;
import com.sayup.SayUp.dto.AuthRequestDTO;
import com.sayup.SayUp.dto.AuthResponseDTO;
import com.sayup.SayUp.dto.UserDto;
import com.sayup.SayUp.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;

    public AuthController(
            AuthenticationManager authenticationManager,
            JwtTokenProvider jwtTokenProvider,
            AuthService authService
    ) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO authRequestDTO) {
        logger.info("Login attempt with email: {}", authRequestDTO.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            authRequestDTO.getEmail(),
                            authRequestDTO.getPassword()
                    )
            );

            String jwt = jwtTokenProvider.createToken(authentication);
            logger.info("Login successful for email: {}", authRequestDTO.getEmail());

            return ResponseEntity.ok(new AuthResponseDTO(jwt));
        } catch (Exception ex) {
            logger.warn("Login failed for email: {}", authRequestDTO.getEmail());
            throw ex;
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody UserDto userDto) {
        logger.info("Register attempt with email: {}", userDto.getEmail());

        try {
            authService.register(userDto);
            logger.info("Registration successful for email: {}", userDto.getEmail());
            return ResponseEntity.ok("User registered successfully!");
        } catch (Exception ex) {
            logger.error("Registration failed for email: {}", userDto.getEmail(), ex);
            return ResponseEntity.badRequest().body("Registration failed: " + ex.getMessage());
        }
    }
}
