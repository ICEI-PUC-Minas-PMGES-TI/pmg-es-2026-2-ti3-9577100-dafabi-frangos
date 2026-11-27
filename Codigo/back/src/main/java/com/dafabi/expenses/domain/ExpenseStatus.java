package com.dafabi.expenses.domain;

public enum ExpenseStatus {
  PENDING,
  CONSOLIDATED,
  CANCELLED,
  /** Pagamento estornado: deixa de contar no resultado e devolve ao caixa o que saiu em dinheiro. */
  REVERSED
}