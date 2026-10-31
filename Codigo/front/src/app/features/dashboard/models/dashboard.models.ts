import { DashboardSummary } from '../../../core/models/domain.models';
export interface DailyRevenue { date: string; day: string; value: number; }
export interface DashboardRecentSale { id: string; number: string; createdAt: string; origin: string; net: number | null; status: string; }
export interface DashboardView {
  summary: DashboardSummary & { financialDataComplete: boolean; additionalMarketplaceCosts: number; marketplacePayouts: number };
  daily: DailyRevenue[];
  recentSales: DashboardRecentSale[];
}
