import { Injectable } from '@angular/core';
import { MockDatabaseService } from '../../../core/services/mock-database.service';
import { DashboardView } from '../models/dashboard.models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  constructor(private readonly db: MockDatabaseService) {}

  async load(period: string, customStart?: string, customEnd?: string): Promise<DashboardView> {
    await new Promise(resolve => setTimeout(resolve, 500));
    const completed = this.db.sales().filter(sale => sale.status === 'COMPLETED');
    const multiplier = period === 'today' ? 1 : period === '7d' ? 5.8 : period === 'custom' ? 2.4 : 18.4;
    const revenue = completed.reduce((sum, sale) => sum + sale.gross, 0) * multiplier;
    const expenses = this.db.expenses().filter(expense => expense.status !== 'CANCELED')
      .reduce((sum, expense) => sum + expense.amount, 0) * (period === 'today' ? .18 : period === 'custom' ? .4 : 1);
    const ifoodFees = completed.filter(sale => sale.origin === 'IFOOD').reduce((sum, sale) => sum + sale.fees, 0) * multiplier;
    const food99Fees = completed.filter(sale => sale.origin === '99FOOD').reduce((sum, sale) => sum + sale.fees, 0) * multiplier;
    const countByOrigin = (origin: 'COUNTER' | 'IFOOD' | '99FOOD') =>
      Math.round(completed.filter(sale => sale.origin === origin).length * multiplier);
    const label = period === 'custom' && customStart && customEnd ? `${customStart} a ${customEnd}` : period;
    return {
      summary: {
        revenue, expenses, ifoodFees, food99Fees,
        estimatedResult: revenue - expenses - ifoodFees - food99Fees,
        salesCount: Math.round(completed.length * multiplier),
        counterSales: countByOrigin('COUNTER'), ifoodSales: countByOrigin('IFOOD'), food99Sales: countByOrigin('99FOOD')
      },
      daily: [
        { day: 'Seg', value: 820 }, { day: 'Ter', value: 1040 }, { day: 'Qua', value: 790 },
        { day: 'Qui', value: 1260 }, { day: 'Sex', value: 1510 }, { day: 'Sáb', value: 1840 },
        { day: period === 'custom' ? label : 'Hoje', value: 1335 }
      ]
    };
  }
}
