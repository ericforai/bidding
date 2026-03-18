// Input: JWT 密钥、过期时间和声明数据
// Output: Token 生成、解析和校验结果
// Pos: Auth/Token 工具层
// 维护声明: 仅维护 JWT 编解码能力；密钥策略变更请同步配置与登录流程.
package com.xiyu.bid.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);
    private static final int MIN_SECRET_LENGTH = 32; // 256 bits = 32 bytes
    private static final String TOKEN_ID_CLAIM = "tokenId";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";
    private static final long REFRESH_TOKEN_MULTIPLIER = 7L;

    private final SecretKey secretKey;
    private final long expiration;
    private final Set<String> revokedTokenIds = ConcurrentHashMap.newKeySet();

    public JwtUtil(String secret, long expiration) {
        // 验证密钥长度
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalArgumentException(
                "JWT secret must be at least " + MIN_SECRET_LENGTH + " characters for HMAC-SHA256. " +
                "Current length: " + (secret != null ? secret.length() : 0) + ". " +
                "Please set JWT_SECRET environment variable with a strong random key."
            );
        }

        this.expiration = expiration;
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        log.info("JWT initialized with expiration: {} ms", expiration);
    }

    public String generateToken(String username) {
        return generateToken(username, ACCESS_TOKEN_TYPE, expiration);
    }

    public String generateRefreshToken(String username) {
        return generateToken(username, REFRESH_TOKEN_TYPE, expiration * REFRESH_TOKEN_MULTIPLIER);
    }

    private String generateToken(String username, String tokenType, long ttlMillis) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + ttlMillis);

        return Jwts.builder()
                .subject(username)
                .claim(TOKEN_ID_CLAIM, UUID.randomUUID().toString())
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public String extractUsername(String token) {
        String subject = extractAllClaims(token).getSubject();
        return subject != null ? subject : "";
    }

    public Long extractExpiration(String token) {
        return extractAllClaims(token).getExpiration().getTime();
    }

    public String extractTokenId(String token) {
        Object tokenId = extractAllClaims(token).get(TOKEN_ID_CLAIM);
        return tokenId != null ? tokenId.toString() : "";
    }

    public String extractTokenType(String token) {
        Object tokenType = extractAllClaims(token).get(TOKEN_TYPE_CLAIM);
        return tokenType != null ? tokenType.toString() : ACCESS_TOKEN_TYPE;
    }

    public boolean validateToken(String token, String username) {
        try {
            String extractedUsername = extractUsername(token);
            return extractedUsername.equals(username)
                    && ACCESS_TOKEN_TYPE.equals(extractTokenType(token))
                    && !isTokenExpired(token)
                    && !isTokenRevoked(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            return REFRESH_TOKEN_TYPE.equals(extractTokenType(token))
                    && !isTokenExpired(token)
                    && !isTokenRevoked(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Refresh token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public void revokeToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }

        try {
            String tokenId = extractTokenId(token);
            if (!tokenId.isBlank()) {
                revokedTokenIds.add(tokenId);
            }
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Ignoring invalid token during revocation: {}", e.getMessage());
        }
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("JWT token is expired: {}", e.getMessage());
            throw e;
        } catch (UnsupportedJwtException e) {
            log.debug("JWT token is unsupported: {}", e.getMessage());
            throw e;
        } catch (MalformedJwtException e) {
            log.debug("JWT token is malformed: {}", e.getMessage());
            throw e;
        } catch (SecurityException e) {
            log.debug("JWT token signature is invalid: {}", e.getMessage());
            throw e;
        } catch (IllegalArgumentException e) {
            log.debug("JWT token is invalid: {}", e.getMessage());
            throw e;
        }
    }

    private boolean isTokenExpired(String token) {
        try {
            Date expiration = extractAllClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    private boolean isTokenRevoked(String token) {
        String tokenId = extractTokenId(token);
        return !tokenId.isBlank() && revokedTokenIds.contains(tokenId);
    }
}
