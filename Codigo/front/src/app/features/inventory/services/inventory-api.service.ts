import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { ApiCreateInventoryLotRequest, ApiInventoryLot, ApiInventoryPosition, ApiStockAdjustmentRequest, ApiStockMovement } from '../models/inventory-api.models';

@Injectable({ providedIn: 'root' })
export class InventoryApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/inventory`;

  positions(query?: string) {
    let params = new HttpParams();
    if (query?.trim()) params = params.set('query', query.trim());
    return this.http.get<ApiInventoryPosition[]>(this.baseUrl, { params });
  }

  movements(productId?: string) {
    let params = new HttpParams();
    if (productId) params = params.set('productId', productId);
    return this.http.get<ApiStockMovement[]>(`${this.baseUrl}/movements`, { params });
  }

  adjust(request: ApiStockAdjustmentRequest) {
    return this.http.post<ApiStockMovement>(`${this.baseUrl}/adjustments`, request);
  }

  lots() {
    return this.http.get<ApiInventoryLot[]>(`${this.baseUrl}/lots`);
  }

  createLot(request: ApiCreateInventoryLotRequest) {
    return this.http.post<ApiInventoryLot>(`${this.baseUrl}/lots`, request);
  }
}
