import { PaymentMethod, PurchaseStatus } from '../../../core/models/domain.models';

export interface ApiPurchaseItem { id: string; productId: string; productName: string; quantity: number; unitCost: number; total: number; batch?: string | null; expiry?: string | null; }
export interface ApiPurchaseSummary { id: string; supplierId: string; supplierName: string; date: string; itemCount: number; total: number; status: PurchaseStatus; paymentStatus?: 'PENDING' | 'PAID'; paymentMethod?: PaymentMethod | null; paidAt?: string | null; createdAt: string; }
export interface ApiPurchase extends ApiPurchaseSummary { createdBy?: string; items: ApiPurchaseItem[]; }
export interface ApiPurchaseItemRequest { productId: string; quantity: number; unitCost: number; batch?: string; expiry?: string; }
export interface ApiCreatePurchaseRequest { supplierId: string; date: string; items: ApiPurchaseItemRequest[]; performedBy?: string; }
export interface ApiPayPurchaseRequest { paymentMethod: PaymentMethod; }
