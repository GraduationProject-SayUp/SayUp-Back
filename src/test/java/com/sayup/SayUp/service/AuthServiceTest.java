package com.sayup.SayUp.service;

import com.sayup.SayUp.dto.auth.AuthRequestDTO;
import com.sayup.SayUp.dto.auth.AuthResponseDTO;
import com.sayup.SayUp.entity.user.User;
import com.sayup.SayUp.kakao.service.KakaoService;
import com.sayup.SayUp.repository.UserRepository;
import com.sayup.SayUp.security.JwtTokenProvider;
import com.sayup.SayUp.service.auth.AuthService;
import com.sayup.SayUp.service.auth.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private KakaoService kakaoService;

    @InjectMocks
    private AuthService authService;

    private AuthRequestDTO authRequestDTO;
    private User user;
    private AuthResponseDTO authResponseDTO;

    @BeforeEach
    void setUp() {
        authRequestDTO = new AuthRequestDTO();
        authRequestDTO.setEmail("test@example.com");
        authRequestDTO.setPassword("password123");

        user = new User();
        user.setUserId(1L);
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setUsername("test");
        user.setRole("USER");

        authResponseDTO = AuthResponseDTO.builder()
                .accessToken("accessToken")
                .refreshToken("refreshToken")
                .tokenType("Bearer")
                .expiresIn(3600L)
                .build();
    }

    @Test
    void register_WithNewEmail_ShouldRegisterSuccessfully() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenProvider.createToken(any())).thenReturn("accessToken");
        when(jwtTokenProvider.createRefreshToken(any())).thenReturn("refreshToken");
        when(jwtTokenProvider.getExpirationTime()).thenReturn(3600L);

        // When
        AuthResponseDTO result = authService.register(authRequestDTO);

        // Then
        assertNotNull(result);
        assertEquals("accessToken", result.getAccessToken());
        assertEquals("refreshToken", result.getRefreshToken());
        verify(userRepository).existsByEmail(authRequestDTO.getEmail());
        verify(passwordEncoder).encode(authRequestDTO.getPassword());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_WithExistingEmail_ShouldThrowException() {
        // Given
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        // When & Then
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.register(authRequestDTO)
        );

        assertEquals("이미 사용 중인 이메일입니다.", exception.getMessage());
        verify(userRepository).existsByEmail(authRequestDTO.getEmail());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_WithValidCredentials_ShouldReturnAuthResponse() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtTokenProvider.createToken(any())).thenReturn("accessToken");
        when(jwtTokenProvider.createRefreshToken(any())).thenReturn("refreshToken");
        when(jwtTokenProvider.getExpirationTime()).thenReturn(3600L);

        // When
        AuthResponseDTO result = authService.login(authRequestDTO);

        // Then
        assertNotNull(result);
        assertEquals("accessToken", result.getAccessToken());
        assertEquals("refreshToken", result.getRefreshToken());
        verify(userRepository).findByEmail(authRequestDTO.getEmail());
        verify(passwordEncoder).matches(authRequestDTO.getPassword(), user.getPassword());
    }

    @Test
    void login_WithInvalidEmail_ShouldThrowBadCredentialsException() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // When & Then
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(authRequestDTO)
        );

        assertEquals("이메일 또는 비밀번호가 올바르지 않습니다.", exception.getMessage());
        verify(userRepository).findByEmail(authRequestDTO.getEmail());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void login_WithInvalidPassword_ShouldThrowBadCredentialsException() {
        // Given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // When & Then
        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> authService.login(authRequestDTO)
        );

        assertEquals("이메일 또는 비밀번호가 올바르지 않습니다.", exception.getMessage());
        verify(userRepository).findByEmail(authRequestDTO.getEmail());
        verify(passwordEncoder).matches(authRequestDTO.getPassword(), user.getPassword());
    }

    @Test
    void isTokenBlacklisted_WithValidToken_ShouldReturnFalse() {
        // Given
        String token = "valid.token.here";
        when(tokenBlacklistService.isBlacklisted(token)).thenReturn(false);

        // When
        boolean result = authService.isTokenBlacklisted(token);

        // Then
        assertFalse(result);
        verify(tokenBlacklistService).isBlacklisted(token);
    }

    @Test
    void isTokenBlacklisted_WithEmptyToken_ShouldReturnFalse() {
        // Given
        String token = "";

        // When
        boolean result = authService.isTokenBlacklisted(token);

        // Then
        assertFalse(result);
        verify(tokenBlacklistService, never()).isBlacklisted(anyString());
    }
} 