import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { ApiCreateSaleRequest, ApiSale, ApiSalePage, SaleQuery } from '../models/sales-api.models';

@Injectable({ providedIn: 'root' })
export class SalesApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/sales`;

  create(request: ApiCreateSaleRequest) {
    return this.http.post<ApiSale>(this.baseUrl, request);
  }

  getById(id: string) {
    return this.http.get<ApiSale>(`${this.baseUrl}/${id}`);
  }

  list(query: SaleQuery = {}) {
    let params = new HttpParams()
      .set('page', query.page ?? 0)
      .set('size', query.size ?? 20);
    if (query.query?.trim()) params = params.set('query', query.query.trim());
    if (query.startDate) params = params.set('startDate', `${query.startDate}T00:00:00-03:00`);
    if (query.endDate) params = params.set('endDate', `${query.endDate}T23:59:59.999-03:00`);
    if (query.origin && query.origin !== 'ALL') params = params.set('origin', query.origin === '99FOOD' ? 'FOOD_99' : query.origin);
    if (query.paymentMethod && query.paymentMethod !== 'ALL') params = params.set('paymentMethod', query.paymentMethod);
    if (query.status && query.status !== 'ALL') params = params.set('status', query.status === 'CANCELED' ? 'CANCELLED' : query.status);
    return this.http.get<ApiSalePage>(this.baseUrl, { params });
  }
}
