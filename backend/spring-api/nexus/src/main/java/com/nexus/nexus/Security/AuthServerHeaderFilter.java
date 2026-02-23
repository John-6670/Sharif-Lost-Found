package com.nexus.nexus.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class AuthServerHeaderFilter extends OncePerRequestFilter {

    private final String headerName;
    private final String sharedSecret;

    public AuthServerHeaderFilter(
            @Value("${security.auth-server.header-name:X-Auth-Server-Token}") String headerName,
            @Value("${security.auth-server.shared-secret:}") String sharedSecret
    ) {
        this.headerName = headerName;
        this.sharedSecret = sharedSecret;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path == null || !path.startsWith("/api/users");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (sharedSecret != null && !sharedSecret.isBlank()) {
            String provided = request.getHeader(headerName);
            if (provided != null && provided.equals(sharedSecret)
                    && SecurityContextHolder.getContext().getAuthentication() == null) {
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        "auth-server",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_AUTH_SERVER"))
                );
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
