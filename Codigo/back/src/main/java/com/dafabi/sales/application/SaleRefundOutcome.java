package com.dafabi.sales.application;

import com.dafabi.sales.dto.SaleRefundResponse;

/**
 * Resultado do registro de estorno. {@code created} é falso quando a requisição foi
 * uma repetição idempotente de um estorno já registrado.
 */
public record SaleRefundOutcome(SaleRefundResponse refund, boolean created) {
}
