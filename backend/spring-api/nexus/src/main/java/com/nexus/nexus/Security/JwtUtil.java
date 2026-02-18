package com.nexus.nexus.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.Objects;

@Component
public class JwtUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.verifying-key:}")
    private String verifyingKeyPem;

    @Value("${jwt.verifying-key-path:}")
    private String verifyingKeyPath;

    private volatile PublicKey cachedVerifyingKey;

    private PublicKey getVerifyingKey() {
        if (cachedVerifyingKey != null) {
            return cachedVerifyingKey;
        }

        try {
            String pem = !verifyingKeyPem.isBlank() ? verifyingKeyPem : loadPemFromPath();
            pem = pem.replace("\\n", "\n");
            String cleaned = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");

            byte[] decoded = Base64.getDecoder().decode(cleaned);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            PublicKey parsedKey = KeyFactory.getInstance("RSA").generatePublic(spec);
            cachedVerifyingKey = parsedKey;
            log.info("JWT verifying key loaded successfully");
            return parsedKey;
        } catch (Exception e) {
            log.error("Failed to load JWT verifying key: {}", e.getMessage());
            throw new IllegalStateException("Invalid jwt verifying key configuration", e);
        }
    }

    private String loadPemFromPath() throws Exception {
        if (verifyingKeyPath.isBlank()) {
            throw new IllegalStateException("Set jwt.verifying-key or jwt.verifying-key-path");
        }

        if (verifyingKeyPath.startsWith("classpath:")) {
            String classpathLocation = verifyingKeyPath.substring("classpath:".length());
            return new String(new ClassPathResource(classpathLocation).getInputStream().readAllBytes());
        }

        return Files.readString(Path.of(verifyingKeyPath));
    }

    public String extractEmail(Claims claims) {
        if (claims == null) {
            return null;
        }

        String subject = claims.getSubject();
        if (subject != null && !subject.isBlank()) {
            log.debug("JWT parsed: using subject as email");
            return subject;
        }

        String email = claims.get("email", String.class);
        if (email != null && !email.isBlank()) {
            log.debug("JWT parsed: using email claim");
            return email;
        }

        log.debug("JWT parsed but no subject/email claim found");
        return null;
    }

    public String extractName(Claims claims) {
        if (claims == null) {
            return null;
        }
        String name = claims.get("name", String.class);
        return name != null && !name.isBlank() ? name : null;
    }

    public Boolean extractIsVerified(Claims claims) {
        if (claims == null) {
            return null;
        }
        Object value = claims.get("is_verified");
        if (value instanceof Boolean boolValue) {
            return boolValue;
        }
        if (value instanceof String stringValue) {
            return Boolean.parseBoolean(stringValue);
        }
        return null;
    }

    public String extractTokenType(Claims claims) {
        if (claims == null) {
            return null;
        }
        return claims.get("token_type", String.class);
    }

    public String extractJti(Claims claims) {
        if (claims == null) {
            return null;
        }
        return claims.getId();
    }

    public Long extractUserId(Claims claims) {
        if (claims == null) {
            return null;
        }
        Object userId = claims.get("user_id");
        if (userId == null) {
            log.debug("JWT parsed but no user_id claim found");
            return null;
        }

        if (userId instanceof Number number) {
            return number.longValue();
        }

        try {
            Long parsed = Long.parseLong(String.valueOf(userId));
            log.debug("JWT parsed: using user_id claim {}", parsed);
            return parsed;
        } catch (NumberFormatException e) {
            log.warn("JWT user_id claim is not numeric: {}", userId);
            return null;
        }
    }

    public Date extractExpiration(Claims claims) {
        if (claims == null) {
            return null;
        }
        return claims.getExpiration();
    }

    public Claims parseClaims(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getVerifyingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            log.debug("JWT signature verified successfully");
            return claims;
        } catch (JwtException e) {
            log.warn("JWT parse/verify failed: {}", e.getMessage());
            throw e;
        }
    }

    private Boolean isTokenExpired(Claims claims) {
        Date expiration = extractExpiration(claims);
        return expiration == null || expiration.before(new Date());
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            Claims claims = parseClaims(token);
            final String email = extractEmail(claims);
            boolean valid = email != null && email.equals(userDetails.getUsername()) && !isTokenExpired(claims);
            if (!valid) {
                log.debug("JWT invalid for userDetails username={}", userDetails.getUsername());
            }
            return valid;
        } catch (Exception e) {
            log.warn("JWT validation failed for userDetails: {}", e.getMessage());
            return false;
        }
    }

    public Boolean validateToken(String token) {
        try {
            Claims claims = parseClaims(token);
            String tokenType = extractTokenType(claims);
            boolean valid = !isTokenExpired(claims) && Objects.equals("access", tokenType);
            if (valid) {
                log.debug("JWT validated successfully");
            } else {
                log.debug("JWT rejected: invalid token_type or expired");
            }
            return valid;
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }
}
