/** Textos em português para os códigos da API, em um só lugar para todas as telas. */
export const PAYMENT_LABELS: Record<string, string> = {
  CASH: 'Dinheiro',
  PIX: 'Pix',
  DEBIT: 'Débito',
  CREDIT: 'Crédito',
  BOLETO_TRANSFER: 'Boleto/transferência'
};

export function paymentLabel(value: string | null | undefined): string {
  return value ? PAYMENT_LABELS[value] ?? value : '—';
}
