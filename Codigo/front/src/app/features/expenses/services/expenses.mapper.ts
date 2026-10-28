import { Expense } from '../../../core/models/domain.models';
import { ExpenseFormValue } from '../models/expense.models';
import { ApiCreateExpenseRequest, ApiExpense, ApiUpdateExpenseRequest } from '../models/expense-api.models';

export function toExpense(api: ApiExpense): Expense {
  return { id: api.id, description: api.description, category: api.category, amount: Number(api.amount), date: api.date, paymentMethod: api.paymentMethod, notes: api.observation ?? undefined, status: api.status === 'CANCELLED' ? 'CANCELED' : api.status };
}
export function toExpenseRequest(value: ExpenseFormValue, userId?: string): ApiCreateExpenseRequest | ApiUpdateExpenseRequest {
  return { description: value.description.trim(), category: value.category.trim(), amount: Number(value.amount), date: value.date, paymentMethod: value.paymentMethod, observation: value.notes?.trim() || undefined, ...(userId ? { userId } : {}) };
}
