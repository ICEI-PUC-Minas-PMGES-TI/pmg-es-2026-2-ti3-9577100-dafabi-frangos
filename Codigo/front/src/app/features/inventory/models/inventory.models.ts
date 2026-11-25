export interface StockAdjustmentRequest { productId: string; newQuantity: number; reason: string; lotId?: string; batch?: string; expiry?: string; }
export interface PerishableRequest { productId: string; batch: string; receivedAt: string; expiry: string; quantity: number; }
