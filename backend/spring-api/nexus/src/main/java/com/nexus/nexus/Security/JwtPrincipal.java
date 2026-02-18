package com.nexus.nexus.Security;

public record JwtPrincipal(
        Long userId,
        String email,
        String name,
        boolean verified,
        String tokenId
) {
}
