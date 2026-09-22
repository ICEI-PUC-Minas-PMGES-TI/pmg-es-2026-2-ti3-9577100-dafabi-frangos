import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { environment } from '../../../../environments/environment';
import { ProductStatus } from '../../../core/models/domain.models';
import { ApiCategory, ApiCreateProductRequest, ApiProduct, ApiProductListItem, ApiUpdateProductRequest, SpringPage } from '../models/product-api.models';

export interface ProductQuery {
  query?: string;
  categoryId?: string;
  status?: ProductStatus;
  includeInactive?: boolean;
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class ProductsApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiBaseUrl}/products`;

  list(query: ProductQuery) {
    let params = new HttpParams()
      .set('page', query.page ?? 0)
      .set('size', query.size ?? 50);
    if (query.query?.trim()) params = params.set('query', query.query.trim());
    if (query.categoryId) params = params.set('categoryId', query.categoryId);
    if (query.status) params = params.set('status', query.status);
    if (query.includeInactive) params = params.set('includeInactive', 'true');
    return this.http.get<SpringPage<ApiProductListItem>>(this.baseUrl, { params });
  }

  getById(id: string) {
    return this.http.get<ApiProduct>(`${this.baseUrl}/${id}`);
  }

  getByBarcode(barcode: string) {
    return this.http.get<ApiProduct>(`${this.baseUrl}/by-barcode/${encodeURIComponent(barcode)}`);
  }

  categories() {
    return this.http.get<ApiCategory[]>(`${environment.apiBaseUrl}/categories`);
  }

  create(request: ApiCreateProductRequest) {
    return this.http.post<ApiProduct>(this.baseUrl, request);
  }

  update(id: string, request: ApiUpdateProductRequest) {
    return this.http.put<ApiProduct>(`${this.baseUrl}/${id}`, request);
  }

  updateStatus(id: string, status: ProductStatus) {
    return this.http.patch<ApiProduct>(`${this.baseUrl}/${id}/status`, { status });
  }
}
