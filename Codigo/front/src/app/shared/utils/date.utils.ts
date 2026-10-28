/** Formata uma data local no padrão aceito por inputs HTML do tipo date. */
export function toDateInputValue(date = new Date()): string {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return localDate.toISOString().slice(0, 10);
}

/** Retorna o primeiro dia do mês da data informada no padrão yyyy-MM-dd. */
export function firstDayOfMonthInputValue(date = new Date()): string {
  return toDateInputValue(new Date(date.getFullYear(), date.getMonth(), 1));
}
