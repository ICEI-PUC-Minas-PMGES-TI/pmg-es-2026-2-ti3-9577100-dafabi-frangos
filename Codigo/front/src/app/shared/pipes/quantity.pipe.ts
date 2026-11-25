import { Pipe, PipeTransform } from '@angular/core';

const formatter = new Intl.NumberFormat('pt-BR', { maximumFractionDigits: 3 });

/** Exibe quantidades de estoque: 176 → "176", 0.5 → "0,5". */
@Pipe({ name: 'qty', standalone: true })
export class QuantityPipe implements PipeTransform {
  transform(value: number | string | null | undefined): string {
    const numeric = Number(value ?? 0);
    return formatter.format(Number.isFinite(numeric) ? numeric : 0);
  }
}
