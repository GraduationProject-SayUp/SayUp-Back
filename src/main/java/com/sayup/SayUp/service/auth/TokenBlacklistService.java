package com.sayup.SayUp.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private static final String BLACKLIST_PREFIX = "blacklist:";
    
    /**
     * 토큰을 블랙리스트에 추가
     * @param token 무효화할 토큰
     * @param expirationTime 토큰 만료 시간 (밀리초)
     */
    public void addToBlacklist(String token, long expirationTime) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Attempted to blacklist null or empty token");
            return;
        }
        
        String key = BLACKLIST_PREFIX + token;
        long ttl = calculateTTL(expirationTime);
        
        redisTemplate.opsForValue().set(key, "blacklisted", ttl, TimeUnit.MILLISECONDS);
        log.info("Token added to blacklist with TTL: {} ms", ttl);
    }
    
    /**
     * 토큰이 블랙리스트에 있는지 확인
     * @param token 확인할 토큰
     * @return 블랙리스트 여부
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Attempted to check null or empty token for blacklist");
            return false;
        }
        
        String key = BLACKLIST_PREFIX + token;
        Boolean exists = redisTemplate.hasKey(key);
        
        if (Boolean.TRUE.equals(exists)) {
            log.debug("Token found in blacklist: {}", token);
            return true;
        }
        
        return false;
    }
    
    /**
     * 토큰을 블랙리스트에서 제거 (필요시 사용)
     * @param token 제거할 토큰
     */
    public void removeFromBlacklist(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Attempted to remove null or empty token from blacklist");
            return;
        }
        
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.delete(key);
        log.info("Token removed from blacklist: {}", token);
    }
    
    /**
     * TTL 계산 (토큰 만료 시간까지 남은 시간)
     * @param expirationTime 토큰 만료 시간
     * @return TTL (밀리초)
     */
    private long calculateTTL(long expirationTime) {
        long currentTime = System.currentTimeMillis();
        long ttl = expirationTime - currentTime;
        
        // 최소 1분, 최대 24시간으로 제한
        return Math.max(60000, Math.min(ttl, 86400000));
    }
}
