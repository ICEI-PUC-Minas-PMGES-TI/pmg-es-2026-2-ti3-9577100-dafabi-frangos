import { PerishableProduct, Product, StockMovement } from '../../../core/models/domain.models';
import { ApiInventoryLot, ApiInventoryPosition, ApiStockMovement } from '../models/inventory-api.models';

export function toStockMovement(api: ApiStockMovement): StockMovement {
  return {
    id: api.id,
    productId: api.productId,
    productName: api.productName,
    date: api.date,
    previousQuantity: api.previousQuantity,
    newQuantity: api.newQuantity,
    difference: api.difference,
    origin: api.origin,
    user: api.user,
    reason: api.reason
  };
}

export function toInventoryProduct(api: ApiInventoryPosition): Product {
  return {
    id: api.productId,
    name: api.productName,
    category: api.categoryName,
    price: 0,
    cost: Number(api.currentCost),
    stock: api.stockQuantity,
    unit: api.unit,
    perishable: api.perishable,
    status: api.productStatus
  };
}

export function toPerishableProduct(api: ApiInventoryLot): PerishableProduct {
  return {
    id: api.id,
    productId: api.productId,
    productName: api.productName,
    batch: api.batch,
    receivedAt: api.receivedAt,
    expiry: api.expiry,
    quantity: api.quantity,
    status: api.status
  };
}
