import { ProductStatus } from '../../../core/models/domain.models';

export interface ApiCategory {
  id: string;
  name: string;
}

export interface ApiProduct {
  id: string;
  category: ApiCategory | null;
  name: string;
  salePrice: number;
  currentCost: number;
  unit: string;
  barcode: string | null;
  perishable: boolean;
  frequent: boolean;
  status: ProductStatus;
  stockQuantity: number;
  createdAt: string;
  updatedAt: string;
  version: number;
}

export interface ApiProductListItem {
  id: string;
  category: ApiCategory | null;
  name: string;
  salePrice: number;
  stockQuantity: number;
  unit: string;
  barcode: string | null;
  status: ProductStatus;
}

export interface SpringPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export interface ApiCreateProductRequest {
  name: string;
  categoryId: string;
  salePrice: number;
  currentCost: number;
  unit: string;
  barcode?: string;
  perishable: boolean;
  frequent: boolean;
  status: ProductStatus;
  initialStock: number;
}

export interface ApiUpdateProductRequest {
  name: string;
  categoryId: string;
  salePrice: number;
  currentCost: number;
  unit: string;
  barcode?: string;
  perishable: boolean;
  frequent: boolean;
}
