import { Injectable, computed } from '@angular/core';
import { AuthService } from '../../../core/auth/auth.service';
import { MockDatabaseService } from '../../../core/services/mock-database.service';
import { CloseCashRequest, OpenCashRequest } from '../models/cash.models';

@Injectable({ providedIn: 'root' })
export class CashService {
  readonly register = this.db.cashRegister;
  readonly movements = this.db.cashMovements.asReadonly();
  readonly cashSales = computed(() => this.db.sales()
    .filter(sale => sale.payment.method === 'CASH' && sale.status === 'COMPLETED')
    .reduce((sum, sale) => sum + sale.net, 0));
  readonly cashExpenses = computed(() => this.db.expenses()
    .filter(expense => expense.paymentMethod === 'CASH' && expense.status !== 'CANCELED')
    .reduce((sum, expense) => sum + expense.amount, 0));
  readonly cashRefunds = computed(() => this.db.refunds()
    .filter(refund => refund.paymentMethod === 'CASH' && refund.status === 'COMPLETED')
    .reduce((sum, refund) => sum + refund.amount, 0));
  readonly expected = computed(() => {
    const register = this.register();
    if (!register) return 0;
    if (register.status === 'CLOSED') return register.expectedBalance;
    return register.initialBalance + this.cashSales() - this.cashExpenses() - this.cashRefunds();
  });

  constructor(private readonly db: MockDatabaseService, private readonly auth: AuthService) {}

  async open(request: OpenCashRequest) {
    await this.delay();
    this.register.set({
      id: `cx-${Date.now()}`, operator: this.auth.currentUser()?.name ?? 'Operador',
      openedAt: new Date().toISOString(), initialBalance: request.initialBalance,
      expectedBalance: request.initialBalance, status: 'OPEN'
    });
  }

  async close(request: CloseCashRequest) {
    await this.delay();
    const expectedBalance = this.expected();
    this.register.update(register => register ? {
      ...register, closedAt: new Date().toISOString(), expectedBalance,
      countedBalance: request.countedBalance,
      difference: request.countedBalance - expectedBalance,
      justification: request.justification,
      status: 'CLOSED'
    } : register);
  }

  private delay() { return new Promise(resolve => setTimeout(resolve, 550)); }
}
