package com.dafabi.auth.dto;

public record LogoutRequest(
        String refreshToken
) {}
