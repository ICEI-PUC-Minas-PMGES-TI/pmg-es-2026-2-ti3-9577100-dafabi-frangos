package com.dafabi.auth.dto;

import java.time.OffsetDateTime;

public record AuthTokenResponse(
        String accessToken,
        String token,
        String refreshToken,
        String tokenType,
        long expiresIn,
        OffsetDateTime expiresAt,
        AuthenticatedUserResponse user
) {}
