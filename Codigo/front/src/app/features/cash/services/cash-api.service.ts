import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { ApiCashMovement, ApiCashRegister, ApiCashSummary, ApiCloseCashRequest, ApiOpenCashRequest } from '../models/cash-api.models';

@Injectable({ providedIn: 'root' })
export class CashApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/cash-registers`;

  async current(storeId?: string): Promise<ApiCashRegister | null> {
    let params = new HttpParams();
    if (storeId) params = params.set('storeId', storeId);
    const response = await firstValueFrom(this.http.get<ApiCashRegister>(`${this.baseUrl}/current`, { params, observe: 'response' }));
    return response.status === 204 ? null : response.body ?? null;
  }

  open(request: ApiOpenCashRequest) {
    return this.http.post<ApiCashRegister>(this.baseUrl, request);
  }

  summary(id: string) {
    return this.http.get<ApiCashSummary>(`${this.baseUrl}/${id}/summary`);
  }

  movements(id: string) {
    return this.http.get<ApiCashMovement[]>(`${this.baseUrl}/${id}/movements`);
  }

  close(id: string, request: ApiCloseCashRequest) {
    return this.http.post<ApiCashRegister>(`${this.baseUrl}/${id}/close`, request);
  }
}
