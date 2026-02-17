package com.nexus.nexus.Security;

import com.nexus.nexus.Entity.User;
import com.nexus.nexus.Repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authorizationHeader = request.getHeader("Authorization");

        String email = null;
        Long userId = null;
        String jwt = null;
        String path = request.getRequestURI();

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            jwt = authorizationHeader.substring(7);
            try {
                email = jwtUtil.extractEmail(jwt);
                if (email == null) {
                    userId = jwtUtil.extractUserId(jwt);
                }
                log.debug("JWT extracted on path {}: email={}, userId={}", path, email, userId);
            } catch (Exception e) {
                log.warn("JWT parsing failed on path {}: {}", path, e.getMessage());
            }
        } else {
            log.debug("No Bearer token provided on path {}", path);
        }

        if ((email != null || userId != null) && SecurityContextHolder.getContext().getAuthentication() == null) {
            Optional<User> userOptional = email != null
                    ? userRepository.findByEmail(email)
                    : userRepository.findById(userId);

            if (userOptional.isPresent() && jwtUtil.validateToken(jwt)) {
                User user = userOptional.get();

                UserDetails userDetails = org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPassword())
                        .authorities(new ArrayList<>())
                        .build();

                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.info("JWT authentication success on path {} for user {}", path, user.getEmail());
            } else if (userOptional.isEmpty()) {
                log.warn("JWT claims resolved but no local user found on path {} (email={}, userId={})", path, email, userId);
            } else {
                log.warn("JWT rejected during validation on path {} (email={}, userId={})", path, email, userId);
            }
        }

        filterChain.doFilter(request, response);
    }
}
