import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { Expense, PaymentMethod } from '../../../core/models/domain.models';
import { ExpenseFormValue } from '../models/expense.models';
import { ApiExpenseStatus } from '../models/expense-api.models';
import { ExpensesApiService } from './expenses-api.service';
import { toExpense, toExpenseRequest } from './expenses.mapper';

@Injectable({ providedIn: 'root' })
export class ExpensesService {
  private readonly api = inject(ExpensesApiService); private readonly auth = inject(AuthService);
  readonly expenses = signal<Expense[]>([]); readonly loading = signal(false); readonly error = signal<string | null>(null);

  async load(filters: { startDate?: string; endDate?: string; category?: string; paymentMethod?: PaymentMethod | 'ALL'; status?: ApiExpenseStatus | 'ALL' } = {}): Promise<void> {
    this.loading.set(true); this.error.set(null);
    try { this.expenses.set((await firstValueFrom(this.api.list(filters))).map(toExpense)); }
    catch (error) { this.error.set(error instanceof Error ? error.message : 'Não foi possível carregar as despesas.'); }
    finally { this.loading.set(false); }
  }
  async find(id: string): Promise<Expense> { return toExpense(await firstValueFrom(this.api.getById(id))); }
  async save(value: ExpenseFormValue, id?: string): Promise<Expense> {
    const request = toExpenseRequest(value, id ? undefined : this.auth.currentUser()?.id);
    const response = id ? await firstValueFrom(this.api.update(id, request)) : await firstValueFrom(this.api.create(request));
    const expense = toExpense(response); this.upsert(expense); return expense;
  }
  async cancel(id: string): Promise<Expense> { const expense = toExpense(await firstValueFrom(this.api.cancel(id))); this.upsert(expense); return expense; }
  private upsert(expense: Expense): void { this.expenses.update(items => { const index = items.findIndex(item => item.id === expense.id); return index < 0 ? [expense, ...items] : items.map(item => item.id === expense.id ? expense : item); }); }
}
