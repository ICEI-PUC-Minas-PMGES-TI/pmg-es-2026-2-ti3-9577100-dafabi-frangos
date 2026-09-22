import { Injectable, computed, signal } from '@angular/core';
import { AuthService } from '../../../core/auth/auth.service';
import { Product, Sale, SaleItem, StockMovement } from '../../../core/models/domain.models';
import { MockDatabaseService } from '../../../core/services/mock-database.service';
import { PaymentRequest } from '../models/sales.models';

export type CartError = 'PRODUCT_NOT_FOUND' | 'INSUFFICIENT_STOCK' | 'INVALID_QUANTITY' | 'EMPTY_CART' | 'INSUFFICIENT_AMOUNT' | 'CASH_CLOSED';

@Injectable({ providedIn: 'root' })
export class SalesService {
  private readonly cartState = signal<SaleItem[]>([]);
  readonly cart = this.cartState.asReadonly();
  readonly total = computed(() => this.cartState().reduce((sum, item) => sum + item.subtotal, 0));
  readonly itemCount = computed(() => this.cartState().reduce((sum, item) => sum + item.quantity, 0));
  readonly products = this.db.products.asReadonly();
  readonly sales = this.db.sales.asReadonly();
  readonly cashOpen = computed(() => this.db.cashRegister()?.status === 'OPEN');

  constructor(private readonly db: MockDatabaseService, private readonly auth: AuthService) {}

  findProducts(query: string): Product[] {
    const normalized = query.trim().toLowerCase();
    return this.db.products().filter(product => product.status === 'ACTIVE' &&
      (!normalized || `${product.name} ${product.category} ${product.barcode ?? ''}`.toLowerCase().includes(normalized)));
  }

  addByBarcode(barcode: string): void {
    const product = this.db.products().find(item => item.barcode === barcode && item.status === 'ACTIVE');
    if (!product) throw new Error('PRODUCT_NOT_FOUND');
    this.addProduct(product);
  }

  addProduct(product: Product): void {
    if (product.stock < 1) throw new Error('INSUFFICIENT_STOCK');
    const existing = this.cartState().find(item => item.productId === product.id);
    const quantity = (existing?.quantity ?? 0) + 1;
    if (quantity > product.stock) throw new Error('INSUFFICIENT_STOCK');
    const updated: SaleItem = {
      productId: product.id, productName: product.name, unitPrice: product.price,
      quantity, subtotal: product.price * quantity
    };
    this.cartState.update(items => existing
      ? items.map(item => item.productId === product.id ? updated : item)
      : [...items, updated]);
  }

  changeQuantity(productId: string, delta: number): void {
    const product = this.db.products().find(item => item.id === productId);
    const cartItem = this.cartState().find(item => item.productId === productId);
    if (!product || !cartItem) return;
    const quantity = cartItem.quantity + delta;
    if (quantity < 1) throw new Error('INVALID_QUANTITY');
    if (quantity > product.stock) throw new Error('INSUFFICIENT_STOCK');
    this.cartState.update(items => items.map(item => item.productId === productId
      ? { ...item, quantity, subtotal: item.unitPrice * quantity } : item));
  }

  remove(productId: string): void { this.cartState.update(items => items.filter(item => item.productId !== productId)); }
  cancel(): void { this.cartState.set([]); }

  async complete(payment: PaymentRequest): Promise<Sale> {
    if (!this.cashOpen()) throw new Error('CASH_CLOSED');
    if (!this.cartState().length) throw new Error('EMPTY_CART');
    if (payment.method === 'CASH' && (payment.received ?? 0) < this.total()) throw new Error('INSUFFICIENT_AMOUNT');
    await new Promise(resolve => setTimeout(resolve, 700));

    const total = this.total();
    const now = new Date().toISOString();
    const sale: Sale = {
      id: `v${Date.now()}`, number: `#${1045 + this.db.sales().length}`, createdAt: now,
      operator: this.auth.currentUser()?.name ?? 'Operador', origin: 'COUNTER',
      payment: { method: payment.method, amount: total, received: payment.received,
        change: payment.method === 'CASH' ? (payment.received ?? 0) - total : undefined },
      items: [...this.cartState()], gross: total, discount: 0, fees: 0, net: total,
      status: 'COMPLETED', history: ['Venda criada no PDV', 'Pagamento confirmado', 'Estoque atualizado']
    };

    const movements: StockMovement[] = [];
    this.db.products.update(products => products.map(product => {
      const sold = sale.items.find(item => item.productId === product.id);
      if (!sold) return product;
      movements.push({
        id: `mov-${sale.id}-${product.id}`, productId: product.id, productName: product.name,
        date: now, previousQuantity: product.stock, newQuantity: product.stock - sold.quantity,
        difference: -sold.quantity, origin: 'Venda balcão', user: sale.operator,
        reason: `Venda ${sale.number} concluída`
      });
      return { ...product, stock: product.stock - sold.quantity };
    }));
    this.db.movements.update(items => [...movements, ...items]);
    this.db.sales.update(sales => [sale, ...sales]);
    if (payment.method === 'CASH') this.db.cashMovements.update(items => [{
      id: `m${Date.now()}`, referenceId: sale.id, date: sale.createdAt,
      description: `Venda ${sale.number}`, type: 'IN', amount: total, origin: 'Dinheiro'
    }, ...items]);
    this.cartState.set([]);
    return sale;
  }
}
