import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { ApiCreateExpenseRequest, ApiExpense, ApiExpenseStatus, ApiUpdateExpenseRequest } from '../models/expense-api.models';
import { PaymentMethod } from '../../../core/models/domain.models';

@Injectable({ providedIn: 'root' })
export class ExpensesApiService {
  private readonly http = inject(HttpClient); private readonly baseUrl = `${environment.apiBaseUrl}/expenses`;
  list(filters: { startDate?: string; endDate?: string; category?: string; paymentMethod?: PaymentMethod | 'ALL'; status?: ApiExpenseStatus | 'ALL' } = {}) {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(filters)) if (value && value !== 'ALL') params = params.set(key, value);
    return this.http.get<ApiExpense[]>(this.baseUrl, { params });
  }
  getById(id: string) { return this.http.get<ApiExpense>(`${this.baseUrl}/${id}`); }
  create(request: ApiCreateExpenseRequest) { return this.http.post<ApiExpense>(this.baseUrl, request); }
  update(id: string, request: ApiUpdateExpenseRequest) { return this.http.put<ApiExpense>(`${this.baseUrl}/${id}`, request); }
  cancel(id: string) { return this.http.patch<ApiExpense>(`${this.baseUrl}/${id}/cancel`, {}); }
}
