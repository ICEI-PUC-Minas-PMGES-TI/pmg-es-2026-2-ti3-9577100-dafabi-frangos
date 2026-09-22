import { Injectable } from '@angular/core';
import { AuthService } from '../../../core/auth/auth.service';
import { Refund, StockMovement } from '../../../core/models/domain.models';
import { MockDatabaseService } from '../../../core/services/mock-database.service';
import { RefundRequest } from '../models/refund.models';

@Injectable({ providedIn: 'root' })
export class RefundsService {
  readonly refunds = this.db.refunds.asReadonly();
  readonly sales = this.db.sales.asReadonly();

  constructor(private readonly db: MockDatabaseService, private readonly auth: AuthService) {}

  find(id: string) { return this.db.refunds().find(refund => refund.id === id); }
  findSale(query: string) {
    return this.db.sales().find(sale => (sale.number === query || sale.id === query) && sale.status === 'COMPLETED');
  }

  async create(request: RefundRequest) {
    await new Promise(resolve => setTimeout(resolve, 650));
    const sale = this.db.sales().find(item => item.id === request.saleId);
    if (!sale || sale.status !== 'COMPLETED') throw new Error('INVALID_SALE');
    const now = new Date().toISOString();
    const user = this.auth.currentUser()?.name ?? 'Administradora';
    const refund: Refund = {
      id: `e${Date.now()}`, saleId: sale.id, saleNumber: sale.number, date: now, user,
      reason: request.reason, amount: sale.net, paymentMethod: sale.payment.method, status: 'COMPLETED'
    };
    const movements: StockMovement[] = [];
    this.db.products.update(products => products.map(product => {
      const line = sale.items.find(item => item.productId === product.id);
      if (!line) return product;
      movements.push({
        id: `mov-${refund.id}-${product.id}`, productId: product.id, productName: product.name,
        date: now, previousQuantity: product.stock, newQuantity: product.stock + line.quantity,
        difference: line.quantity, origin: 'Estorno', user,
        reason: `Estorno da venda ${sale.number}: ${request.reason}`
      });
      return { ...product, stock: product.stock + line.quantity };
    }));
    this.db.movements.update(items => [...movements, ...items]);
    this.db.refunds.update(items => [refund, ...items]);
    this.db.sales.update(items => items.map(item => item.id === sale.id
      ? { ...item, status: 'REFUNDED', history: [...item.history, `Estorno realizado: ${request.reason}`, 'Caixa e estoque revertidos'] }
      : item));
    if (sale.payment.method === 'CASH') this.db.cashMovements.update(items => [{
      id: `m${Date.now()}`, referenceId: refund.id, date: now, description: `Estorno ${sale.number}`,
      type: 'OUT', amount: sale.net, origin: 'Estorno em dinheiro'
    }, ...items]);
    return refund;
  }
}
