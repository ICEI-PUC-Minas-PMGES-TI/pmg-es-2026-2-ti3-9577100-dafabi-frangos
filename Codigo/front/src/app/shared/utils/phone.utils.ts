/** Telefone brasileiro com DDD: (31) 3333-4444 ou (31) 99999-9999. */
export const PHONE_PATTERN = /^\(?\d{2}\)?[ -]?9?\d{4}-?\d{4}$/;

/** Formata somente números como (DD) 9XXXX-XXXX; mantém o texto se não for um telefone reconhecível. */
export function formatPhone(value: string): string {
  const digits = (value ?? '').replace(/\D/g, '');
  if (digits.length === 11) return `(${digits.slice(0, 2)}) ${digits.slice(2, 7)}-${digits.slice(7)}`;
  if (digits.length === 10) return `(${digits.slice(0, 2)}) ${digits.slice(2, 6)}-${digits.slice(6)}`;
  return value;
}
