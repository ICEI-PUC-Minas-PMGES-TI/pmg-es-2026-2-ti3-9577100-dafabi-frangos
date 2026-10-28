import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { ApiCreatePurchaseRequest, ApiPurchase, ApiPurchaseSummary } from '../models/purchase-api.models';

@Injectable({ providedIn: 'root' })
export class PurchasesApiService {
  private readonly http = inject(HttpClient); private readonly baseUrl = `${environment.apiBaseUrl}/purchases`;
  list(supplierId?: string) { let params = new HttpParams(); if (supplierId) params = params.set('supplierId', supplierId); return this.http.get<ApiPurchaseSummary[]>(this.baseUrl, { params }); }
  getById(id: string) { return this.http.get<ApiPurchase>(`${this.baseUrl}/${id}`); }
  create(request: ApiCreatePurchaseRequest) { return this.http.post<ApiPurchase>(this.baseUrl, request); }
}
