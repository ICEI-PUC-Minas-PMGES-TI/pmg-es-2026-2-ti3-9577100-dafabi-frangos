import { DashboardSummary, SaleOrigin, SaleStatus } from '../../../core/models/domain.models';

export interface DailyRevenue {
  date: string;
  day: string;
  value: number;
}

export interface DashboardRecentSale {
  id: string;
  number: string;
  createdAt: string;
  origin: SaleOrigin;
  total: number;
  status: SaleStatus;
}

export interface DashboardView {
  startDate: string;
  endDate: string;
  summary: DashboardSummary;
  otherMarketplaceCosts: number;
  marketplaceFeesComplete: boolean;
  chartGranularity: 'dia' | 'semana' | 'mês';
  daily: DailyRevenue[];
  recentSales: DashboardRecentSale[];
}

export interface ApiFinancialDashboardResponse {
  startDate: string;
  endDate: string;
  summary: {
    revenue: number;
    expenses: number;
    fees: number;
    ifoodFees: number;
    food99Fees: number;
    otherMarketplaceCosts?: number;
    marketplaceFeesComplete?: boolean;
    estimatedResult: number;
    salesCount: number;
    counterSales: number;
    ifoodSales: number;
    food99Sales: number;
    cashIn: number;
    cashOut: number;
  };
  salesByOrigin: Record<string, number>;
  daily: Array<{ date: string; revenue: number }>;
  recentSales?: Array<{ id: string; number: string; dateTime: string; origin: string; total: number; status: string }>;
}
