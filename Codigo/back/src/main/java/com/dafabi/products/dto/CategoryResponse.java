package com.dafabi.products.dto;

import java.util.UUID;

public record CategoryResponse(
        UUID id,
        String name
) {
}
