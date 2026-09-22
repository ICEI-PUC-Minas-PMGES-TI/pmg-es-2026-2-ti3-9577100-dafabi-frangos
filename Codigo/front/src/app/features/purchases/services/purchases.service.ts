import { Injectable, signal } from '@angular/core';
import { AuthService } from '../../../core/auth/auth.service';
import { Purchase, StockMovement } from '../../../core/models/domain.models';
import { MockDatabaseService } from '../../../core/services/mock-database.service';
import { PurchaseDraft } from '../models/purchase.models';

@Injectable({ providedIn: 'root' })
export class PurchasesService {
  readonly purchases = signal<Purchase[]>([
    { id: 'c1', supplierId: 'f1', supplierName: 'Granja Santa Clara', date: '2026-09-14',
      items: [{ productId: 'p1', productName: 'Frango assado tradicional', quantity: 40, unitCost: 24.5, total: 980 },
        { productId: 'p3', productName: 'Meio frango assado', quantity: 20, unitCost: 13, total: 260 }],
      total: 1240, status: 'REGISTERED' },
    { id: 'c2', supplierId: 'f2', supplierName: 'Distribuidora Bom Sabor', date: '2026-09-12',
      items: [{ productId: 'p4', productName: 'Farofa da casa 300 g', quantity: 40, unitCost: 4.1, total: 164 },
        { productId: 'p5', productName: 'Refrigerante 2 L', quantity: 52, unitCost: 8.2, total: 426.4 }],
      total: 590.4, status: 'REGISTERED' }
  ]);
  readonly products = this.db.products.asReadonly();
  readonly suppliers = this.db.suppliers.asReadonly();

  constructor(private readonly db: MockDatabaseService, private readonly auth: AuthService) {}

  async create(draft: PurchaseDraft) {
    await new Promise(resolve => setTimeout(resolve, 600));
    const items = draft.items.map(item => {
      const product = this.db.products().find(candidate => candidate.id === item.productId)!;
      return { productId: product.id, productName: product.name, quantity: item.quantity,
        unitCost: item.unitCost, total: item.quantity * item.unitCost };
    });
    const supplier = this.db.suppliers().find(item => item.id === draft.supplierId)!;
    const purchase: Purchase = {
      id: `c${Date.now()}`, supplierId: supplier.id, supplierName: supplier.name,
      date: draft.date, items, total: items.reduce((sum, item) => sum + item.total, 0), status: 'REGISTERED'
    };
    const movements: StockMovement[] = [];
    this.db.products.update(products => products.map(product => {
      const line = items.find(item => item.productId === product.id);
      if (!line) return product;
      movements.push({
        id: `mov-${purchase.id}-${product.id}`, productId: product.id, productName: product.name,
        date: new Date().toISOString(), previousQuantity: product.stock,
        newQuantity: product.stock + line.quantity, difference: line.quantity,
        origin: 'Compra', user: this.auth.currentUser()?.name ?? 'Administradora',
        reason: `Compra registrada de ${supplier.name}`
      });
      return { ...product, stock: product.stock + line.quantity, cost: line.unitCost };
    }));
    this.db.movements.update(current => [...movements, ...current]);
    this.purchases.update(list => [purchase, ...list]);
    return purchase;
  }
}
