package com.dafabi.products.dto;

import com.dafabi.products.domain.ProductStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateProductStatusRequest(
    @NotNull(message = "O status é obrigatório") ProductStatus status) {
}
