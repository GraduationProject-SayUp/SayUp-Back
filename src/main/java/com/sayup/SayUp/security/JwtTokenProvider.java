package com.sayup.SayUp.security;

import com.sayup.SayUp.service.AuthService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {
    
    private Key secretKey;
    private long accessTokenValidityInMilliseconds;
    private long refreshTokenValidityInMilliseconds;
    private final AuthService authService;

    @Value("${jwt.secret}")
    private String secretKeyString;
    
    @Value("${jwt.expiration}")
    private String expirationStr;
    
    @Value("${jwt.refresh-expiration}")
    private String refreshExpirationStr;

    public JwtTokenProvider(@Lazy AuthService authService) {
        this.authService = authService;
    }

    @PostConstruct
    public void init() {
        if (secretKeyString.length() < 32) {
            throw new IllegalArgumentException("Secret key must be at least 32 characters long");
        }
        this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes());
        
        try {
            this.accessTokenValidityInMilliseconds = Long.parseLong(expirationStr.trim());
            this.refreshTokenValidityInMilliseconds = Long.parseLong(refreshExpirationStr.trim());
        } catch (NumberFormatException e) {
            log.error("Invalid JWT expiration configuration: expiration={}, refresh-expiration={}", 
                     expirationStr, refreshExpirationStr);
            throw new IllegalArgumentException("JWT expiration values must be valid numbers", e);
        }
        
        log.info("JWT Token Provider initialized - Access Token Expiration: {}ms, Refresh Token Expiration: {}ms", 
                accessTokenValidityInMilliseconds, refreshTokenValidityInMilliseconds);
    }

    /**
     * Access Token 생성
     */
    public String createToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return createTokenFromEmail(userDetails.getUsername());
    }

    /**
     * Refresh Token 생성
     */
    public String createRefreshToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return createRefreshTokenFromEmail(userDetails.getUsername());
    }

    /**
     * 이메일로 Access Token 생성
     */
    public String createTokenFromEmail(String email) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

        return Jwts.builder()
                .setSubject(email)
                .claim("type", "access")
                .claim("roles", "ROLE_USER")
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * 이메일로 Refresh Token 생성
     */
    public String createRefreshTokenFromEmail(String email) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

        return Jwts.builder()
                .setSubject(email)
                .claim("type", "refresh")
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Access Token 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            if (authService.isTokenBlacklisted(token)) {
                log.warn("Token is blacklisted");
                return false;
            }

            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Access Token 타입 확인
            String tokenType = claims.get("type", String.class);
            if (!"access".equals(tokenType)) {
                log.warn("Invalid token type: {}", tokenType);
                return false;
            }

            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token expired: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("Invalid token format: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Token is null or empty: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Refresh Token 유효성 검증
     */
    public boolean validateRefreshToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            // Refresh Token 타입 확인
            String tokenType = claims.get("type", String.class);
            if (!"refresh".equals(tokenType)) {
                log.warn("Invalid refresh token type: {}", tokenType);
                return false;
            }

            return true;
        } catch (Exception e) {
            log.warn("Invalid refresh token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * JWT 토큰에서 이메일 추출
     */
    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    /**
     * JWT 토큰에서 만료 시간 추출
     */
    public long getExpirationTime(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            
            Date expiration = claims.getExpiration();
            return expiration != null ? expiration.getTime() : System.currentTimeMillis() + accessTokenValidityInMilliseconds;
        } catch (Exception e) {
            return System.currentTimeMillis() + accessTokenValidityInMilliseconds;
        }
    }

    /**
     * Access Token 만료 시간 반환
     */
    public long getExpirationTime() {
        return accessTokenValidityInMilliseconds;
    }
}
