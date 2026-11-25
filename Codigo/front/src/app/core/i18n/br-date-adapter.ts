import { Injectable } from '@angular/core';
import { NativeDateAdapter } from '@angular/material/core';

/** Adaptador nativo que entende datas digitadas como dd/mm/aaaa e as exibe nesse formato. */
@Injectable()
export class BrDateAdapter extends NativeDateAdapter {
  override parse(value: unknown): Date | null {
    if (typeof value === 'string') {
      const match = /^\s*(\d{1,2})\/(\d{1,2})\/(\d{4})\s*$/.exec(value);
      if (match) {
        const date = new Date(Number(match[3]), Number(match[2]) - 1, Number(match[1]));
        return date.getDate() === Number(match[1]) ? date : this.invalid();
      }
      if (!value.trim()) return null;
    }
    return super.parse(value);
  }

  override format(date: Date, displayFormat: object): string {
    if (!this.isValid(date)) return '';
    if ((displayFormat as { year?: string; month?: string; day?: string }).day === 'numeric') {
      return `${String(date.getDate()).padStart(2, '0')}/${String(date.getMonth() + 1).padStart(2, '0')}/${date.getFullYear()}`;
    }
    return super.format(date, displayFormat);
  }
}
