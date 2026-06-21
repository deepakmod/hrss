package com.mod.hrss.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private final SecretKey key;

    public TokenBlacklistService(
            StringRedisTemplate redisTemplate,
            @Value("${app.jwt.secret:dGhpcy1pcy1hLXN1cGVyLXNlY3JldC1rZXktZm9yLWhybXMtYmFja2VuZC1hcHBsaWNhdGlvbi1zZWN1cml0eQ==}") String jwtSecret
    ) {
        this.redisTemplate = redisTemplate;
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtSecret);
        } catch (IllegalArgumentException e) {
            keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public void blacklistToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Date expiration = claims.getExpiration();
            long diff = expiration.getTime() - System.currentTimeMillis();

            if (diff > 0) {
                redisTemplate.opsForValue().set(
                        getBlacklistKey(token),
                        "blacklisted",
                        diff,
                        TimeUnit.MILLISECONDS
                );
                log.info("Token blacklisted. Remaining time: {} ms", diff);
            }
        } catch (Exception e) {
            log.warn("Could not blacklist token in Redis: {}", e.getMessage());
        }
    }

    public boolean isTokenBlacklisted(String token) {
        try {
            Boolean hasKey = redisTemplate.hasKey(getBlacklistKey(token));
            return Boolean.TRUE.equals(hasKey);
        } catch (Exception e) {
            log.warn("Could not check blacklist in Redis: {}", e.getMessage());
            return false;
        }
    }

    private String getBlacklistKey(String token) {
        return "blacklist:token:" + token;
    }
}
