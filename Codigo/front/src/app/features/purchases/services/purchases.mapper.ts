import { Product, Purchase, PurchaseItem } from '../../../core/models/domain.models';
import { ApiCreatePurchaseRequest, ApiPurchase, ApiPurchaseItem, ApiPurchaseSummary } from '../models/purchase-api.models';
import { PurchaseDraft } from '../models/purchase.models';

function toItem(item: ApiPurchaseItem): PurchaseItem { return { productId: item.productId, productName: item.productName, quantity: item.quantity, unitCost: Number(item.unitCost), total: Number(item.total) }; }
export function toPurchase(api: ApiPurchase | ApiPurchaseSummary): Purchase {
  return { id: api.id, supplierId: api.supplierId, supplierName: api.supplierName, date: api.date, items: 'items' in api ? api.items.map(toItem) : [], total: Number(api.total), status: api.status };
}
export function toCreatePurchaseRequest(draft: PurchaseDraft, products: Product[], performedBy?: string): ApiCreatePurchaseRequest {
  return { supplierId: draft.supplierId, date: draft.date, performedBy, items: draft.items.map(item => ({ productId: item.productId, quantity: Number(item.quantity), unitCost: Number(item.unitCost) })) };
}
