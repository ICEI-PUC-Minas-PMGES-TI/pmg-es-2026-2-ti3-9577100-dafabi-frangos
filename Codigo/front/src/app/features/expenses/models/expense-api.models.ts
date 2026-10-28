import { PaymentMethod } from '../../../core/models/domain.models';

export type ApiExpenseStatus = 'PENDING' | 'CONSOLIDATED' | 'CANCELLED';
export interface ApiExpense {
  id: string; description: string; category: string; amount: number; date: string;
  paymentMethod: PaymentMethod; observation?: string | null; status: ApiExpenseStatus;
  userId?: string | null; createdAt: string; updatedAt: string;
}
export interface ApiCreateExpenseRequest { description: string; category: string; amount: number; date: string; paymentMethod: PaymentMethod; observation?: string; userId?: string; }
export type ApiUpdateExpenseRequest = Omit<ApiCreateExpenseRequest, 'userId'>;
