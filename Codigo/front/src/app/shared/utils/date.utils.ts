/** Formata uma data local no padrão aceito por inputs HTML do tipo date. */
export function toDateInputValue(date = new Date()): string {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return localDate.toISOString().slice(0, 10);
}

/** Retorna o primeiro dia do mês da data informada no padrão yyyy-MM-dd. */
export function firstDayOfMonthInputValue(date = new Date()): string {
  return toDateInputValue(new Date(date.getFullYear(), date.getMonth(), 1));
}

/** Converte `yyyy-MM-dd` em uma data local (meia-noite); devolve null quando vazio ou inválido. */
export function fromDateInputValue(value: string | null | undefined): Date | null {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value ?? '');
  if (!match) return null;
  const date = new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
  return Number.isNaN(date.getTime()) ? null : date;
}
