package com.dafabi.products.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductRequest(
        @NotBlank(message = "O nome do produto é obrigatório")
        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres")
        String name,

        UUID categoryId,

        @NotNull(message = "O preço de venda é obrigatório")
        @PositiveOrZero(message = "O preço de venda não pode ser negativo")
        BigDecimal salePrice,

        @NotNull(message = "O custo atual é obrigatório")
        @PositiveOrZero(message = "O custo atual não pode ser negativo")
        BigDecimal currentCost,

        String unit,

        String barcode,

        Boolean perishable,

        Boolean frequent,

        @PositiveOrZero(message = "O estoque inicial não pode ser negativo")
        Integer initialStock
) {
}
