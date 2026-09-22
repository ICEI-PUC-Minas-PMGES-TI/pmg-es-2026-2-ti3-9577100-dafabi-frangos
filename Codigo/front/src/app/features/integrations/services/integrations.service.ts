import { Injectable } from '@angular/core';
import { Sale, StockMovement } from '../../../core/models/domain.models';
import { MockDatabaseService } from '../../../core/services/mock-database.service';
import { SyncSummary } from '../models/integration.models';

@Injectable({ providedIn: 'root' })
export class IntegrationsService {
  readonly integrations = this.db.integrations.asReadonly();
  private readonly importedExternalIds = new Set(
    this.db.sales().map(sale => sale.externalId).filter((id): id is string => Boolean(id))
  );

  constructor(private readonly db: MockDatabaseService) {}

  find(id: 'IFOOD' | '99FOOD') { return this.db.integrations().find(item => item.id === id)!; }

  async sync(id: 'IFOOD' | '99FOOD'): Promise<SyncSummary> {
    await new Promise(resolve => setTimeout(resolve, 900));
    if (this.find(id).status === 'UNAVAILABLE') throw new Error('UNAVAILABLE');
    const summary = id === 'IFOOD'
      ? { orders: 8, gross: 486.7, discounts: 28.4, fees: 92.53, cancellations: 1, net: 365.77, duplicates: 2 }
      : { orders: 5, gross: 291.8, discounts: 12, fees: 49.61, cancellations: 1, net: 230.19, duplicates: 1 };

    const now = new Date().toISOString();
    const prefix = id === 'IFOOD' ? 'IF' : '99';
    const completedId = `${prefix}-2051`;
    const canceledId = `${prefix}-2052`;
    const alreadyImported = this.importedExternalIds.has(completedId) && this.importedExternalIds.has(canceledId);
    if (!this.importedExternalIds.has(completedId)) this.importCompleted(id, completedId, now);
    if (!this.importedExternalIds.has(canceledId)) this.importCanceled(id, canceledId, now);

    this.db.integrations.update(list => list.map(item => item.id === id ? {
      ...item, lastSync: now,
      importedOrders: item.importedOrders + (alreadyImported ? 0 : summary.orders)
    } : item));
    return summary;
  }

  validateFile(file: File) {
    const valid = /\.(csv|xlsx)$/i.test(file.name) && file.size < 5_000_000;
    return {
      fileName: file.name, rows: valid ? 12 : 0, valid,
      summary: { orders: 12, gross: 719.8, discounts: 34.5, fees: 122.37, cancellations: 1, net: 562.93, duplicates: 2 }
    };
  }

  async importFile() { return this.sync('99FOOD'); }

  private importCompleted(origin: 'IFOOD' | '99FOOD', externalId: string, now: string) {
    const product = this.db.products().find(item => item.id === 'p2')!;
    if (product.stock < 1) throw new Error('INSUFFICIENT_STOCK');
    const discount = origin === 'IFOOD' ? 2 : 0;
    const fees = origin === 'IFOOD' ? 5.48 : 4.23;
    const sale: Sale = {
      id: `${origin.toLowerCase()}-${Date.now()}`, externalId, number: `#${externalId}`, createdAt: now,
      operator: `Integração ${origin === 'IFOOD' ? 'iFood' : '99Food'}`, origin,
      payment: { method: 'CREDIT', amount: product.price },
      items: [{ productId: product.id, productName: product.name, unitPrice: product.price, quantity: 1, subtotal: product.price }],
      gross: product.price, discount, fees, net: product.price - discount - fees,
      status: 'COMPLETED', history: [`Venda importada da ${origin === 'IFOOD' ? 'iFood' : '99Food'}`, 'Duplicidade validada', 'Estoque atualizado']
    };
    const movement: StockMovement = {
      id: `mov-${externalId}`, productId: product.id, productName: product.name, date: now,
      previousQuantity: product.stock, newQuantity: product.stock - 1, difference: -1,
      origin: `Venda ${origin === 'IFOOD' ? 'iFood' : '99Food'}`, user: sale.operator,
      reason: `Pedido integrado ${externalId}`
    };
    this.db.products.update(items => items.map(item => item.id === product.id ? { ...item, stock: item.stock - 1 } : item));
    this.db.movements.update(items => [movement, ...items]);
    this.db.sales.update(items => [sale, ...items]);
    this.importedExternalIds.add(externalId);
  }

  private importCanceled(origin: 'IFOOD' | '99FOOD', externalId: string, now: string) {
    const product = this.db.products().find(item => item.id === 'p2')!;
    const operator = `Integração ${origin === 'IFOOD' ? 'iFood' : '99Food'}`;
    const sale: Sale = {
      id: `${origin.toLowerCase()}-cancel-${Date.now()}`, externalId, number: `#${externalId}`, createdAt: now,
      operator, origin, payment: { method: 'CREDIT', amount: product.price },
      items: [{ productId: product.id, productName: product.name, unitPrice: product.price, quantity: 1, subtotal: product.price }],
      gross: product.price, discount: 0, fees: 0, net: 0, status: 'CANCELED',
      history: [`Venda importada da ${origin === 'IFOOD' ? 'iFood' : '99Food'}`, 'Cancelamento identificado', 'Estoque recomposto']
    };
    const deducted = Math.max(0, product.stock - 1);
    const movements: StockMovement[] = [
      { id: `mov-${externalId}-entrada`, productId: product.id, productName: product.name, date: now,
        previousQuantity: product.stock, newQuantity: deducted, difference: -1,
        origin: `Venda ${origin === 'IFOOD' ? 'iFood' : '99Food'}`, user: operator, reason: `Pedido integrado ${externalId}` },
      { id: `mov-${externalId}-cancel`, productId: product.id, productName: product.name, date: now,
        previousQuantity: deducted, newQuantity: product.stock, difference: 1,
        origin: `Cancelamento ${origin === 'IFOOD' ? 'iFood' : '99Food'}`, user: operator,
        reason: `Cancelamento integrado ${externalId}; estoque recomposto` }
    ];
    this.db.movements.update(items => [...movements, ...items]);
    this.db.sales.update(items => [sale, ...items]);
    this.importedExternalIds.add(externalId);
  }
}
