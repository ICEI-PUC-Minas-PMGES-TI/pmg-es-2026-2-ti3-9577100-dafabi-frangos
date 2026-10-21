package com.dafabi.suppliers.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSupplierRequest(
        @NotBlank(message = "O nome do fornecedor é obrigatório.")
        @Size(min = 3, max = 150, message = "O nome do fornecedor deve ter entre 3 e 150 caracteres.")
        String name,

        @NotBlank(message = "O telefone do fornecedor é obrigatório.")
        @Size(max = 30, message = "O telefone deve ter no máximo 30 caracteres.")
        String phone,

        @NotBlank(message = "O e-mail do fornecedor é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
        String email,

        @Size(max = 500, message = "As observações devem ter no máximo 500 caracteres.")
        String notes,

        Boolean active
) {
}
