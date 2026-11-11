import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { DashboardSummary } from '../../../core/models/domain.models';
import { ApiFinancialDashboardResponse, DashboardView } from '../models/dashboard.models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);
  private readonly endpoint = `${environment.apiBaseUrl}/dashboard/financial-summary`;

  async load(period: string, startDate: string, endDate: string): Promise<DashboardView> {
    let params = new HttpParams().set('period', period);
    if (period === 'custom') {
      params = params.set('startDate', startDate).set('endDate', endDate);
    }

    const dashboard = await firstValueFrom(this.http.get<ApiFinancialDashboardResponse>(this.endpoint, { params }));
    const summary: DashboardSummary = {
      revenue: Number(dashboard.summary.revenue),
      expenses: Number(dashboard.summary.expenses),
      ifoodFees: Number(dashboard.summary.ifoodFees),
      food99Fees: Number(dashboard.summary.food99Fees),
      estimatedResult: Number(dashboard.summary.estimatedResult),
      salesCount: Number(dashboard.summary.salesCount),
      counterSales: Number(dashboard.summary.counterSales),
      ifoodSales: Number(dashboard.summary.ifoodSales),
      food99Sales: Number(dashboard.summary.food99Sales)
    };

    return {
      startDate: dashboard.startDate,
      endDate: dashboard.endDate,
      summary,
      otherMarketplaceCosts: Number(dashboard.summary.otherMarketplaceCosts ?? 0),
      marketplaceFeesComplete: dashboard.summary.marketplaceFeesComplete !== false,
      daily: dashboard.daily.map(point => ({
        date: point.date,
        day: new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', timeZone: 'UTC' }).format(new Date(`${point.date}T12:00:00Z`)),
        value: Number(point.revenue)
      })),
      recentSales: (dashboard.recentSales ?? []).map(sale => ({
        id: sale.id,
        number: sale.number,
        createdAt: sale.dateTime,
        origin: sale.origin === 'FOOD_99' ? '99FOOD' : sale.origin as 'COUNTER' | 'IFOOD',
        total: Number(sale.total),
        status: sale.status === 'CANCELLED' || sale.status === 'PARTIALLY_CANCELLED' ? 'CANCELED' : 'COMPLETED'
      }))
    };
  }
}
