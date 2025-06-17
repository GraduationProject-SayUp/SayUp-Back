package com.sayup.SayUp.config;

import com.sayup.SayUp.security.JwtAuthenticationFilter;
import com.sayup.SayUp.security.JwtTokenProvider;
import com.sayup.SayUp.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
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

import java.util.Arrays;

@Configuration
@EnableWebSecurity  // Spring Security 설정 활성화
@RequiredArgsConstructor
public class SecurityConfig {
    private final AuthService authService;  // 사용자 인증 서비스 (사용자 정보를 로드하고 인증 처리)
    private final PasswordEncoder passwordEncoder;  // 비밀번호 암호화 인코더
    private final JwtTokenProvider jwtTokenProvider;  // JWT 토큰 제공자

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    @Value("${frontend.url:}")
    private String frontendUrl;

    /**
     * JWT 인증 필터 Bean 생성
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, authService);
    }

    /**
     * 보안 필터 체인 구성
     * HTTP 요청에 대한 보안 규칙 정의
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // 공개 API 경로 - 인증 없이 접근 가능한 경로 목록
        final String[] PUBLIC_URLS = {
                "/api/auth/**",       // 인증 관련 API
                "/swagger-ui/**",     // Swagger UI
                "/v3/api-docs/**",    // OpenAPI 문서
                "/callback/**",       // 카카오 로그인
                "/actuator/health"    // 헬스체크
        };

        return http
                // CSRF 보호 비활성
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
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)

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
                .userDetailsService(authService)    // 사용자 정보 서비스 설정
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

        // 환경별 CORS 설정
        if ("dev".equals(activeProfile)) {
            // 개발 환경: 로컬호스트 허용
            configuration.setAllowedOrigins(Arrays.asList(
                    "http://localhost:3000",
                    "http://localhost:8080",
                    "http://127.0.0.1:3000",
                    "http://127.0.0.1:8080"
            ));
        } else {
            // 운영 환경: 환경변수에서 프론트엔드 URL 가져오기
            if (frontendUrl != null && !frontendUrl.trim().isEmpty()) {
                configuration.setAllowedOrigins(Arrays.asList(frontendUrl));
            } else {
                // 도메인 변경 필요
            }
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
