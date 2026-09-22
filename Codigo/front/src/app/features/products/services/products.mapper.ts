import { Category, Product } from '../../../core/models/domain.models';
import { ProductFormValue } from '../models/product.models';
import { ApiCreateProductRequest, ApiProduct, ApiProductListItem, ApiUpdateProductRequest } from '../models/product-api.models';

export function toProduct(api: ApiProduct | ApiProductListItem): Product {
  return {
    id: api.id,
    name: api.name,
    category: api.category?.name ?? 'Sem categoria',
    categoryId: api.category?.id,
    price: Number(api.salePrice),
    cost: 'currentCost' in api ? Number(api.currentCost) : 0,
    stock: api.stockQuantity,
    unit: api.unit,
    barcode: api.barcode ?? undefined,
    perishable: 'perishable' in api ? api.perishable : false,
    frequent: 'frequent' in api ? api.frequent : false,
    status: api.status,
    version: 'version' in api ? api.version : undefined
  };
}

export function toCategory(api: { id: string; name: string }): Category {
  return { id: api.id, name: api.name };
}

export function toCreateRequest(value: ProductFormValue): ApiCreateProductRequest {
  return {
    name: value.name.trim(),
    categoryId: value.categoryId,
    salePrice: value.price,
    currentCost: value.cost,
    unit: value.unit,
    barcode: value.barcode?.trim() || undefined,
    perishable: value.perishable,
    frequent: value.frequent,
    status: value.status,
    initialStock: value.stock
  };
}

export function toUpdateRequest(value: ProductFormValue): ApiUpdateProductRequest {
  return {
    name: value.name.trim(),
    categoryId: value.categoryId,
    salePrice: value.price,
    currentCost: value.cost,
    unit: value.unit,
    barcode: value.barcode?.trim() || undefined,
    perishable: value.perishable,
    frequent: value.frequent
  };
}
