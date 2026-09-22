import { Product, ProductStatus } from '../../../core/models/domain.models';
export interface ProductFilters { query: string; status: Product['status'] | 'ALL'; category: string; }
export interface ProductFormValue {
  name: string;
  categoryId: string;
  price: number;
  cost: number;
  stock: number;
  unit: string;
  barcode?: string;
  perishable: boolean;
  status: ProductStatus;
  frequent: boolean;
}

export interface ProductPageState {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}
