package com.nexus.nexus.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
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
            return parsedKey;
        } catch (Exception e) {
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
            return subject;
        }

        String email = claims.get("email", String.class);
        if (email != null && !email.isBlank()) {
            return email;
        }

        return null;
    }

    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userId = claims.get("user_id");
        if (userId == null) {
            return null;
        }

        if (userId instanceof Number number) {
            return number.longValue();
        }

        try {
            return Long.parseLong(String.valueOf(userId));
        } catch (NumberFormatException e) {
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
        return Jwts.parser()
                .verifyWith(getVerifyingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return (email != null && email.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public Boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
}
