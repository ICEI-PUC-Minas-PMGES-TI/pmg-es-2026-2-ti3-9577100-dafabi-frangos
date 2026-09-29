import { Sale, SaleItem } from '../../../core/models/domain.models';
import { ApiSale, ApiSaleItem } from '../models/sales-api.models';

const DEMO_OPERATOR_NAMES: Record<string, string> = {
  '00000000-0000-0000-0000-000000000101': 'Fabiana Nogueira',
  '00000000-0000-0000-0000-000000000102': 'Carlos Oliveira'
};

function operatorLabel(operatorId: string, origin: ApiSale['origin']): string {
  if (origin === 'IFOOD') return 'Integração iFood';
  if (origin === 'FOOD_99') return 'Integração 99Food';
  return DEMO_OPERATOR_NAMES[operatorId] ?? 'Operador de caixa';
}

function toItem(api: ApiSaleItem): SaleItem {
  return {
    productId: api.productId,
    productName: api.productName,
    unitPrice: Number(api.unitPrice),
    quantity: Number(api.quantity),
    subtotal: Number(api.subtotal)
  };
}

export function toSale(api: ApiSale): Sale {
  const total = Number(api.total);
  const received = api.amountReceived == null ? undefined : Number(api.amountReceived);
  return {
    id: api.id,
    number: `#${api.saleNumber}`,
    createdAt: api.dateTime,
    operator: operatorLabel(api.operatorId, api.origin),
    origin: api.origin === 'FOOD_99' ? '99FOOD' : api.origin,
    payment: {
      method: api.paymentMethod,
      amount: total,
      received,
      change: api.change == null && received != null ? received - total : api.change == null ? undefined : Number(api.change)
    },
    items: api.items.map(toItem),
    gross: Number(api.subtotal),
    discount: 0,
    fees: 0,
    net: total,
    status: api.status === 'CANCELLED' ? 'CANCELED' : api.status,
    history: ['Venda registrada pela API', 'Estoque atualizado']
  };
}
