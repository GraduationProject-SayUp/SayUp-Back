package com.sayup.SayUp.config;

import com.sayup.SayUp.security.JwtAuthenticationFilter;
import com.sayup.SayUp.service.AuthService;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;  // JWT 인증 필터
    private final AuthService authService;  // 사용자 인증 서비스 (사용자 정보를 로드하고 인증 처리)
    private final PasswordEncoder passwordEncoder;  // 비밀번호 암호화 인코더

    /**
     * 보안 필터 체인 구성
     * HTTP 요청에 대한 보안 규칙 정의
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 공개 API 경로 - 인증 없이 접근 가능한 경로 목록
        final String[] PUBLIC_URLS = {
                "/api/auth/**",      // 인증 관련 API
                "/swagger-ui/**",    // Swagger UI
                "/v3/api-docs/**",   // OpenAPI 문서
                "/callback/**"       // 카카오 로그인
        };

        return http
                // CSRF 보호 비활성화 (REST API는 CSRF 공격에 덜 취약함)
                .csrf(csrf -> csrf.disable())

                // 세션 관리 설정 - JWT 사용으로 세션 상태 저장 안함
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 요청 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()  // 공개 URL 허용
                        .anyRequest().authenticated()              // 나머지 요청은 인증 필요
                )

                // CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // JWT 필터 추가 (UsernamePasswordAuthenticationFilter 이전에 실행)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }

    /**
     * 인증 관리자 Bean 설정
     * 사용자 인증 처리를 위한 AuthenticationManager 구성
     */
    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authenticationManagerBuilder = http.getSharedObject(AuthenticationManagerBuilder.class);
        authenticationManagerBuilder
                .userDetailsService(authService)  // 사용자 정보 서비스 설정
                .passwordEncoder(passwordEncoder);  // 비밀번호 인코더 설정
        return authenticationManagerBuilder.build();
    }

    /**
     * CORS 설정 소스 Bean 설정
     * Cross Origin Resource Sharing 정책 구성
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(Collections.singletonList("*")); // 모든 Origin 허용
        configuration.setAllowedMethods(Collections.singletonList("*")); // 모든 HTTP Method 허용
        configuration.setAllowedHeaders(Collections.singletonList("*")); // 모든 Header 허용

        configuration.setAllowCredentials(false); // 쿠키 사용 X

        // 모든 경로에 적용
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}


