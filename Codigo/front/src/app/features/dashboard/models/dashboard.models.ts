import { DashboardSummary } from '../../../core/models/domain.models';
export interface DailyRevenue { day: string; value: number; }
export interface DashboardView { summary: DashboardSummary; daily: DailyRevenue[]; }
