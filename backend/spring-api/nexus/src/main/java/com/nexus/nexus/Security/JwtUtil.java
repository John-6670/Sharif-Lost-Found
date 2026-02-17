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
import java.util.function.Function;

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
            // Supports env var style PEM with escaped newlines.
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

    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);

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

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
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

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
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

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String email = extractEmail(token);
            boolean valid = email != null && email.equals(userDetails.getUsername()) && !isTokenExpired(token);
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
            boolean valid = !isTokenExpired(token);
            if (valid) {
                log.debug("JWT validated successfully");
            } else {
                log.debug("JWT rejected: expired");
            }
            return valid;
        } catch (Exception e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }
}
