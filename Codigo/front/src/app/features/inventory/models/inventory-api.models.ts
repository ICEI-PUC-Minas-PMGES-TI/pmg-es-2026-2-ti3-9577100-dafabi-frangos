import { ProductStatus } from '../../../core/models/domain.models';

export interface ApiStockMovement {
  id: string;
  productId: string;
  productName: string;
  date: string;
  type: string;
  previousQuantity: number;
  newQuantity: number;
  difference: number;
  origin: string;
  user: string;
  reason: string;
}

export interface ApiInventoryPosition {
  productId: string;
  productName: string;
  categoryName: string;
  stockQuantity: number;
  unit: string;
  currentCost: number;
  perishable: boolean;
  productStatus: ProductStatus;
  lastMovement: ApiStockMovement | null;
}

export interface ApiInventoryLot {
  id: string;
  productId: string;
  productName: string;
  batch: string;
  receivedAt: string;
  expiry: string;
  quantity: number;
  status: 'VALID' | 'EXPIRED' | 'USED';
}

export interface ApiStockAdjustmentRequest {
  productId: string;
  newQuantity: number;
  reason: string;
  performedBy?: string;
}

export interface ApiCreateInventoryLotRequest {
  productId: string;
  batch: string;
  receivedAt: string;
  expiry: string;
  quantity: number;
  performedBy?: string;
}
