import { Injectable } from '@angular/core';
import { Expense } from '../../../core/models/domain.models';
import { MockDatabaseService } from '../../../core/services/mock-database.service';
import { ExpenseFormValue } from '../models/expense.models';

@Injectable({ providedIn: 'root' })
export class ExpensesService {
  readonly expenses = this.db.expenses.asReadonly();

  constructor(private readonly db: MockDatabaseService) {}

  find(id: string) { return this.db.expenses().find(expense => expense.id === id); }

  async save(value: ExpenseFormValue, id?: string) {
    await new Promise(resolve => setTimeout(resolve, 450));
    const item: Expense = { ...value, id: id ?? `d${Date.now()}` };
    this.db.expenses.update(items => id
      ? items.map(expense => expense.id === id ? item : expense)
      : [item, ...items]);
    this.syncCashMovement(item);
    return item;
  }

  async cancel(id: string) {
    await new Promise(resolve => setTimeout(resolve, 300));
    const expense = this.find(id);
    if (!expense || expense.status === 'CONSOLIDATED') throw new Error('CONSOLIDATED');
    this.db.expenses.update(items => items.map(item => item.id === id ? { ...item, status: 'CANCELED' } : item));
    this.db.cashMovements.update(items => items.filter(movement => movement.referenceId !== id));
  }

  private syncCashMovement(expense: Expense) {
    this.db.cashMovements.update(items => {
      const withoutCurrent = items.filter(movement => movement.referenceId !== expense.id);
      if (expense.paymentMethod !== 'CASH' || expense.status === 'CANCELED') return withoutCurrent;
      return [{
        id: `mov-exp-${expense.id}`, referenceId: expense.id, date: `${expense.date}T12:00:00`,
        description: expense.description, type: 'OUT' as const, amount: expense.amount,
        origin: 'Despesa em dinheiro'
      }, ...withoutCurrent];
    });
  }
}
