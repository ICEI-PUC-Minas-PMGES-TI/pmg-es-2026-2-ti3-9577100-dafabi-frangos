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
    const dashboardRequest = this.fetch(period, startDate, endDate);
    const chartRequest = period === 'today'
      ? this.fetch('month', startDate, endDate)
      : dashboardRequest;
    const [dashboard, chartDashboard] = await Promise.all([dashboardRequest, chartRequest]);
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

    const rawDaily = chartDashboard.daily.map(point => ({
      date: point.date,
      day: new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', timeZone: 'UTC' }).format(new Date(`${point.date}T12:00:00Z`)),
      value: Number(point.revenue)
    }));
    const periodDays = Math.max(1, Math.round((Date.parse(`${chartDashboard.endDate}T00:00:00Z`) - Date.parse(`${chartDashboard.startDate}T00:00:00Z`)) / 86_400_000) + 1);
    const chartGranularity: DashboardView['chartGranularity'] = periodDays <= 14 ? 'dia' : periodDays <= 90 ? 'semana' : 'mês';

    return {
      startDate: dashboard.startDate,
      endDate: dashboard.endDate,
      summary,
      otherMarketplaceCosts: Number(dashboard.summary.otherMarketplaceCosts ?? 0),
      marketplaceFeesComplete: dashboard.summary.marketplaceFeesComplete !== false,
      chartGranularity,
      daily: aggregateRevenue(rawDaily, chartGranularity),
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

  private fetch(period: string, startDate: string, endDate: string): Promise<ApiFinancialDashboardResponse> {
    let params = new HttpParams().set('period', period);
    if (period === 'custom') {
      params = params.set('startDate', startDate).set('endDate', endDate);
    }
    return firstValueFrom(this.http.get<ApiFinancialDashboardResponse>(this.endpoint, { params }));
  }
}

function aggregateRevenue(points: DashboardView['daily'], granularity: DashboardView['chartGranularity']): DashboardView['daily'] {
  if (granularity === 'dia') return points;

  const grouped = new Map<string, DashboardView['daily'][number]>();
  for (const point of points) {
    const date = new Date(`${point.date}T00:00:00Z`);
    let key: string;
    let label: string;
    if (granularity === 'semana') {
      const monday = new Date(date);
      monday.setUTCDate(monday.getUTCDate() - ((monday.getUTCDay() + 6) % 7));
      key = monday.toISOString().slice(0, 10);
      label = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', timeZone: 'UTC' }).format(monday);
    } else {
      key = `${date.getUTCFullYear()}-${String(date.getUTCMonth() + 1).padStart(2, '0')}`;
      const month = new Intl.DateTimeFormat('pt-BR', { month: 'short', timeZone: 'UTC' }).format(date).replace('.', '');
      label = `${month}/${String(date.getUTCFullYear()).slice(-2)}`;
    }

    const current = grouped.get(key);
    if (current) current.value += point.value;
    else grouped.set(key, { date: key, day: label, value: point.value });
  }
  return [...grouped.values()];
}
