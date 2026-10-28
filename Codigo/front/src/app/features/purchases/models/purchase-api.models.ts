import { PurchaseStatus } from '../../../core/models/domain.models';

export interface ApiPurchaseItem { id: string; productId: string; productName: string; quantity: number; unitCost: number; total: number; }
export interface ApiPurchaseSummary { id: string; supplierId: string; supplierName: string; date: string; itemCount: number; total: number; status: PurchaseStatus; createdAt: string; }
export interface ApiPurchase extends ApiPurchaseSummary { createdBy?: string; items: ApiPurchaseItem[]; }
export interface ApiPurchaseItemRequest { productId: string; quantity: number; unitCost: number; }
export interface ApiCreatePurchaseRequest { supplierId: string; date: string; items: ApiPurchaseItemRequest[]; performedBy?: string; }
