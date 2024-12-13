package com.sayup.SayUp.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {
    private final Key secretKey;
    private final long validityInMilliseconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secretKeyString,
            @Value("${jwt.expiration}") long validityInMilliseconds
    ) {
        if (secretKeyString.length() < 32) {
            throw new IllegalArgumentException("Secret key must be at least 32 characters long");
        }
        this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes());
        this.validityInMilliseconds = validityInMilliseconds;
    }

    public String createToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMilliseconds);

        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("roles", userDetails.getAuthorities().toString())
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
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
        } catch (Exception e) {
            System.err.println("Invalid token: " + e.getMessage());
        }
        return false;
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims != null ? claims.getSubject() : null;
    }
}
