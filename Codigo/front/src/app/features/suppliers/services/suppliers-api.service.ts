import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { ApiCreateSupplierRequest, ApiSupplier, ApiUpdateSupplierRequest } from '../models/supplier-api.models';

@Injectable({ providedIn: 'root' })
export class SuppliersApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/suppliers`;

  list(query?: string, active?: boolean) {
    let params = new HttpParams();
    if (query?.trim()) params = params.set('query', query.trim());
    if (active !== undefined) params = params.set('active', active);
    return this.http.get<ApiSupplier[]>(this.baseUrl, { params });
  }
  getById(id: string) { return this.http.get<ApiSupplier>(`${this.baseUrl}/${id}`); }
  create(request: ApiCreateSupplierRequest) { return this.http.post<ApiSupplier>(this.baseUrl, request); }
  update(id: string, request: ApiUpdateSupplierRequest) { return this.http.put<ApiSupplier>(`${this.baseUrl}/${id}`, request); }
  updateStatus(id: string, active: boolean) { return this.http.patch<ApiSupplier>(`${this.baseUrl}/${id}/status`, { active }); }
}
