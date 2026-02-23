package com.nexus.nexus.Security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            filterChain.doFilter(request, response);
            return;
        }                                

        final String authorizationHeader = request.getHeader("Authorization");

        String email = null;
        String name = null;
        Boolean isVerified = null;
        Long userId = null;
        String jti = null;
        String jwt = null;
        String path = request.getRequestURI();

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                Claims claims = jwtUtil.parseClaims(jwt);
                String tokenType = jwtUtil.extractTokenType(claims);

                if (!Objects.equals("access", tokenType)) {
                    log.warn("JWT rejected on path {}: token_type is not access", path);
                } else {
                    email = jwtUtil.extractEmail(claims);
                    name = jwtUtil.extractName(claims);
                    isVerified = jwtUtil.extractIsVerified(claims);
                    userId = jwtUtil.extractUserId(claims);
                    jti = jwtUtil.extractJti(claims);
                }

                log.debug("JWT extracted on path {}: email={}, userId={}, isVerified={}", path, email, userId, isVerified);
            } catch (Exception e) {
                log.warn("JWT parsing failed on path {}: {}", path, e.getMessage());
            }
        } else {
            log.debug("No Bearer token provided on path {}", path);
        }

        boolean claimsAreUsable = email != null && !email.isBlank() && Boolean.TRUE.equals(isVerified);
        if (claimsAreUsable && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtUtil.validateToken(jwt)) {
                JwtPrincipal principal = new JwtPrincipal(userId, email, name, true, jti);
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(principal, null, new ArrayList<>());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.info("JWT authentication success on path {} (email={}, userId={})", path, email, userId);
            } else {
                log.warn("JWT rejected during validation on path {} (email={}, userId={})", path, email, userId);
            }
        } else if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            log.warn("JWT rejected on path {} due to missing required claims (email/is_verified)", path);
        }

        filterChain.doFilter(request, response);
    }
}
