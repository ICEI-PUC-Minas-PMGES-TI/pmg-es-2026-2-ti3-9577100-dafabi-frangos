export interface StockAdjustmentRequest { productId: string; newQuantity: number; reason: string; }
export interface PerishableRequest { productId: string; batch: string; receivedAt: string; expiry: string; quantity: number; }
