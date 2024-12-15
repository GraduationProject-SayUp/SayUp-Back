package com.sayup.SayUp.security;

import com.sayup.SayUp.service.AuthService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private final Key secretKey;
    private final long validityInMilliseconds;
    private final AuthService authService;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKeyString,
            @Value("${jwt.expiration}") long validityInMilliseconds,
            @Lazy AuthService authService
    ) {
        if (secretKeyString.length() < 32) {
            throw new IllegalArgumentException("Secret key must be at least 32 characters long");
        }
        this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes());
        this.validityInMilliseconds = validityInMilliseconds;
        this.authService = authService;
    }

    /**
     * JWT 토큰 생성
     * @param authentication Spring Security Authentication 객체
     * @return 생성된 JWT 토큰
     */
    public String createToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String email = userDetails.getUsername(); // 이메일 반환

        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setSubject(email)  // 이메일을 subject로 설정
                .claim("roles", userDetails.getAuthorities().toString())
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * JWT 토큰 유효성 검증
     * @param token 검증할 JWT 토큰
     * @return 토큰이 유효하면 true, 그렇지 않으면 false
     */
    public boolean validateToken(String token) {
        try {
            // 블랙리스트에 있는지 확인
            if (authService.isTokenBlacklisted(token)) {
                System.err.println("Token is blacklisted");
                return false;
            }

            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            System.err.println("Token expired: " + e.getMessage());
        } catch (MalformedJwtException e) {
            System.err.println("Invalid token format: " + e.getMessage());
        } catch (UnsupportedJwtException e) {
            System.err.println("Unsupported token: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Token is null or empty: " + e.getMessage());
        }
        return false;
    }

    /**
     * JWT 토큰에서 이메일(사용자 식별자) 추출
     * @param token JWT 토큰
     * @return 토큰에서 추출한 이메일
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }
}
