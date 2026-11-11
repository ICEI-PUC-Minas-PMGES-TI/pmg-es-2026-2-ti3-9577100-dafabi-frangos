import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../../environments/environment';
import {
  ImportBatch,
  IntegrationOverview,
  IntegrationProvider,
  MarketplaceMapping,
  MarketplaceMappingComponent,
  MarketplaceStockEffect,
  MarketplaceStockBaseline,
  MarketplaceSettlement,
  MarketplaceItemSnapshot,
  MarketplaceOrder
} from '../models/integration.models';

@Injectable({ providedIn: 'root' })
export class IntegrationsService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/integrations`;

  overview(): Observable<IntegrationOverview[]> {
    return this.http.get<IntegrationOverview[]>(`${this.baseUrl}/overview`);
  }

  upload(provider: IntegrationProvider, file: File): Observable<ImportBatch> {
    const body = new FormData();
    body.append('file', file, file.name);
    return this.http.post<ImportBatch>(`${this.baseUrl}/${provider}/imports`, body);
  }

  getBatch(id: string): Observable<ImportBatch> {
    return this.http.get<ImportBatch>(`${this.baseUrl}/imports/${encodeURIComponent(id)}`);
  }

  confirm(id: string): Observable<ImportBatch> {
    return this.http.post<ImportBatch>(`${this.baseUrl}/imports/${encodeURIComponent(id)}/confirm`, {});
  }

  history(provider: IntegrationProvider): Observable<ImportBatch[]> {
    return this.http.get<ImportBatch[]>(`${this.baseUrl}/${provider}/imports`);
  }

  orders(provider: IntegrationProvider, startDate = '', endDate = ''): Observable<MarketplaceOrder[]> {
    const params: Record<string, string> = {};
    if (startDate) params['startDate'] = startDate;
    if (endDate) params['endDate'] = endDate;
    return this.http.get<MarketplaceOrder[]>(`${this.baseUrl}/${provider}/orders`, { params });
  }

  items(provider: IntegrationProvider): Observable<MarketplaceItemSnapshot[]> {
    return this.http.get<MarketplaceItemSnapshot[]>(`${this.baseUrl}/${provider}/items`);
  }

  mappings(provider: IntegrationProvider, externalStoreId: string): Observable<MarketplaceMapping[]> {
    return this.http.get<MarketplaceMapping[]>(`${this.baseUrl}/${provider}/mappings`, { params: { externalStoreId } });
  }

  saveMapping(provider: IntegrationProvider, externalStoreId: string, externalItemName: string,
    value: { ignored: boolean; components: Pick<MarketplaceMappingComponent, 'productId' | 'quantityPerItem'>[] }): Observable<MarketplaceMapping> {
    return this.http.put<MarketplaceMapping>(`${this.baseUrl}/${provider}/mappings`, value,
      { params: { externalStoreId, externalItemName } });
  }

  getStockBaseline(provider: IntegrationProvider, externalStoreId: string): Observable<MarketplaceStockBaseline | null> {
    return this.http.get<MarketplaceStockBaseline | null>(`${this.baseUrl}/${provider}/stock-baseline`, { params: { externalStoreId } });
  }

  setStockBaseline(provider: IntegrationProvider, externalStoreId: string, startDate: string): Observable<MarketplaceStockBaseline> {
    return this.http.put<MarketplaceStockBaseline>(`${this.baseUrl}/${provider}/stock-baseline`, { externalStoreId, startDate });
  }

  stockEffects(provider: IntegrationProvider, externalStoreId: string, startDate: string, endDate: string): Observable<MarketplaceStockEffect[]> {
    return this.http.get<MarketplaceStockEffect[]>(`${this.baseUrl}/${provider}/stock-effects`,
      { params: { externalStoreId, startDate, endDate } });
  }

  applyStock(provider: IntegrationProvider, externalStoreId: string, startDate: string, endDate: string,
    confirmReturnedProducts: boolean): Observable<MarketplaceStockEffect[]> {
    return this.http.post<MarketplaceStockEffect[]>(`${this.baseUrl}/${provider}/stock-effects/apply`,
      { externalStoreId, startDate, endDate, confirmReturnedProducts });
  }

  settlements(provider: IntegrationProvider, externalStoreId: string): Observable<MarketplaceSettlement[]> {
    return this.http.get<MarketplaceSettlement[]>(`${this.baseUrl}/${provider}/settlements`, { params: { externalStoreId } });
  }

  saveSettlement(provider: IntegrationProvider, value: Omit<MarketplaceSettlement,'id'|'provider'|'updatedAt'>): Observable<MarketplaceSettlement> {
    return this.http.put<MarketplaceSettlement>(`${this.baseUrl}/${provider}/settlements`, value);
  }

  static apiProvider(provider: 'IFOOD' | '99FOOD'): IntegrationProvider {
    return provider === '99FOOD' ? 'FOOD_99' : 'IFOOD';
  }
}
