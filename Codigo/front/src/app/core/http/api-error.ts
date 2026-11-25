import { HttpErrorResponse } from '@angular/common/http';

export interface ApiFieldError {
  field: string;
  message: string;
}

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
    readonly fieldErrors: ApiFieldError[] = []
  ) {
    super(message);
  }
}

export function toApiError(error: unknown): ApiError {
  if (error instanceof ApiError) return error;

  if (error instanceof HttpErrorResponse) {
    const payload = error.error as { code?: string; detail?: string; fieldErrors?: ApiFieldError[] } | null;
    const fieldErrors = payload?.fieldErrors ?? [];
    // Erros de validação: mostra o motivo de cada campo em vez da mensagem genérica.
    const fieldMessage = [...new Set(fieldErrors.map(item => item.message).filter(Boolean))].join(' ');
    return new ApiError(
      error.status,
      payload?.code ?? (error.status === 0 ? 'API_UNAVAILABLE' : 'HTTP_ERROR'),
      fieldMessage || payload?.detail || (error.status === 0
        ? 'Não foi possível conectar ao servidor. Verifique se o backend está em execução.'
        : 'Não foi possível concluir a operação.'),
      fieldErrors
    );
  }

  return new ApiError(0, 'UNKNOWN_ERROR', 'Não foi possível concluir a operação.');
}
