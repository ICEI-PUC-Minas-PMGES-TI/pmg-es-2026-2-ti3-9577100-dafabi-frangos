import { Sale, SaleItem } from '../../../core/models/domain.models';
import { ApiSale, ApiSaleItem } from '../models/sales-api.models';

function operatorLabel(operatorId: string, origin: ApiSale['origin']): string {
  if (origin === 'IFOOD') return 'Integração iFood';
  if (origin === 'FOOD_99') return 'Integração 99Food';
  // Nunca exibe o identificador técnico: vendas antigas podem apontar para um usuário que não existe mais.
  return 'Usuário não encontrado';
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
    operator: api.operatorName || operatorLabel(api.operatorId, api.origin),
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
    // Venda estornada ou cancelada não gera receita: o líquido fica zerado.
    net: api.status === 'REFUNDED' || api.status === 'CANCELLED' ? 0 : total,
    status: api.status === 'CANCELLED' ? 'CANCELED' : api.status,
    history: ['Venda registrada', 'Estoque baixado']
  };
}
