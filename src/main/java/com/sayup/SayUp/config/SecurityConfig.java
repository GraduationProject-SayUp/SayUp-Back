package com.sayup.SayUp.config;

import com.sayup.SayUp.security.JwtAuthenticationFilter;
import com.sayup.SayUp.service.AuthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Collections;

@Configuration
@EnableWebSecurity  // Spring Security의 설정을 활성화
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;  // JWT 인증 필터
    private final AuthService authService;  // 사용자 인증 서비스 (사용자 정보를 로드하고 인증 처리)
    private final PasswordEncoder passwordEncoder;  // 비밀번호 암호화 인코더

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, AuthService authService, PasswordEncoder passwordEncoder) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 보안 필터 체인 구성
     * HTTP 요청에 대한 보안 규칙 정의
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                // 세션 비활성화 (JWT 사용)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 인증 및 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/swagger-ui/**",
                                "v3/api-docs/**").permitAll() // 인증 없이 접근 가능 경로
                        .anyRequest().authenticated() // 나머지 요청 인증 필요
                )
                // CORS 설정
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // JWT 필터 추가
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder
                .userDetailsService(authService)
                .passwordEncoder(passwordEncoder);
        return authenticationManagerBuilder.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Collections.singletonList("*")); // 모든 Origin 허용
        configuration.setAllowedMethods(Collections.singletonList("*")); // 모든 HTTP Method 허용
        configuration.setAllowedHeaders(Collections.singletonList("*")); // 모든 Header 허용
        configuration.setAllowCredentials(true); // 쿠키 전송 허용

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); // 모든 경로에 적용
        return source;
    }
}


