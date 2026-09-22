import { Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Category, Product, ProductStatus } from '../../../core/models/domain.models';
import { ProductFormValue, ProductPageState } from '../models/product.models';
import { toCategory, toCreateRequest, toProduct, toUpdateRequest } from './products.mapper';
import { ProductsApiService } from './products-api.service';

export interface ProductListQuery {
  query?: string;
  categoryId?: string;
  status?: ProductStatus | 'ALL';
  page?: number;
  size?: number;
}

@Injectable({ providedIn: 'root' })
export class ProductsService {
  readonly products = signal<Product[]>([]);
  readonly categories = signal<Category[]>([]);
  readonly pageState = signal<ProductPageState>({ page: 0, size: 50, totalElements: 0, totalPages: 0 });

  constructor(private readonly api: ProductsApiService) {}

  async load(query: ProductListQuery = {}): Promise<void> {
    const status = query.status === 'ALL' ? undefined : query.status;
    const response = await firstValueFrom(this.api.list({
      query: query.query,
      categoryId: query.categoryId,
      status,
      includeInactive: query.status === 'ALL',
      page: query.page ?? 0,
      size: query.size ?? 50
    }));
    this.products.set(response.content.map(toProduct));
    this.pageState.set({
      page: response.number,
      size: response.size,
      totalElements: response.totalElements,
      totalPages: response.totalPages
    });
  }

  async loadCategories(): Promise<void> {
    const response = await firstValueFrom(this.api.categories());
    this.categories.set(response.map(toCategory));
  }

  async findById(id: string): Promise<Product> {
    const product = toProduct(await firstValueFrom(this.api.getById(id)));
    this.upsert(product);
    return product;
  }

  async findByBarcode(barcode: string): Promise<Product> {
    const product = toProduct(await firstValueFrom(this.api.getByBarcode(barcode)));
    this.upsert(product);
    return product;
  }

  async save(value: ProductFormValue, id?: string): Promise<Product> {
    const response = id
      ? await firstValueFrom(this.api.update(id, toUpdateRequest(value)))
      : await firstValueFrom(this.api.create(toCreateRequest(value)));
    const product = toProduct(response);
    this.upsert(product);
    return product;
  }

  async updateStatus(id: string, status: ProductStatus): Promise<Product> {
    const product = toProduct(await firstValueFrom(this.api.updateStatus(id, status)));
    this.upsert(product);
    return product;
  }

  async inactivate(id: string): Promise<Product> {
    return this.updateStatus(id, 'INACTIVE');
  }

  private upsert(product: Product): void {
    this.products.update(items => {
      const index = items.findIndex(item => item.id === product.id);
      return index === -1
        ? [product, ...items]
        : items.map(item => item.id === product.id ? product : item);
    });
  }
}
