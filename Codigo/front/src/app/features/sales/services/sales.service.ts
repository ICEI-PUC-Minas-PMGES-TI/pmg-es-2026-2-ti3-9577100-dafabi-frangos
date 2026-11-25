import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { ApiError } from '../../../core/http/api-error';
import { Product, Sale, SaleItem } from '../../../core/models/domain.models';
import { ProductsApiService } from '../../products/services/products-api.service';
import { toProduct } from '../../products/services/products.mapper';
import { PaymentRequest } from '../models/sales.models';
import { SaleQuery } from '../models/sales-api.models';
import { SalesApiService } from './sales-api.service';
import { toSale } from './sales.mapper';
import { CashService } from '../../cash/services/cash.service';

export type CartError = 'PRODUCT_NOT_FOUND' | 'INSUFFICIENT_STOCK' | 'INVALID_QUANTITY' | 'EMPTY_CART' | 'INSUFFICIENT_AMOUNT' | 'CASH_CLOSED';

const STORE_ID = '00000000-0000-0000-0000-000000000001';

/** Perecíveis só podem ser vendidos a partir de lotes dentro da validade. */
function sellableOf(product: Product): number {
  return product.sellable ?? product.stock;
}

@Injectable({ providedIn: 'root' })
export class SalesService {
  private readonly api = inject(SalesApiService);
  private readonly productsApi = inject(ProductsApiService);
  private readonly auth = inject(AuthService);
  private readonly cash = inject(CashService);
  private readonly cartState = signal<SaleItem[]>([]);
  readonly cart = this.cartState.asReadonly();
  readonly products = signal<Product[]>([]);
  readonly sales = signal<Sale[]>([]);
  readonly total = computed(() => this.cartState().reduce((sum, item) => sum + item.subtotal, 0));
  readonly itemCount = computed(() => this.cartState().reduce((sum, item) => sum + item.quantity, 0));
  readonly cashOpen = computed(() => this.cash.cashOpen());
  readonly loadingProducts = signal(false);
  readonly productsError = signal<string | null>(null);
  readonly loadingSales = signal(false);
  readonly salesError = signal<string | null>(null);
  readonly pageState = signal({ page: 0, size: 20, totalElements: 0, totalPages: 0 });
  readonly completedCounterTotal = signal(0);
  private productsRequest = 0;
  private historyRequest = 0;

  async loadProducts(query = ''): Promise<void> {
    const request = ++this.productsRequest;
    this.loadingProducts.set(true);
    this.productsError.set(null);
    try {
      const response = await firstValueFrom(this.productsApi.list({ query, status: 'ACTIVE', page: 0, size: 100 }));
      if (request === this.productsRequest) this.products.set(response.content.map(toProduct));
    } catch (error) {
      if (request === this.productsRequest) this.productsError.set(error instanceof ApiError ? error.message : 'Não foi possível carregar os produtos.');
    } finally {
      if (request === this.productsRequest) this.loadingProducts.set(false);
    }
  }

  findProducts(query: string): Product[] {
    const normalized = query.trim().toLowerCase();
    return this.products().filter(product => product.status === 'ACTIVE' &&
      (!normalized || `${product.name} ${product.category} ${product.barcode ?? ''}`.toLowerCase().includes(normalized)));
  }

  async addByBarcode(barcode: string): Promise<void> {
    try {
      const product = toProduct(await firstValueFrom(this.productsApi.getByBarcode(barcode)));
      this.upsertProduct(product);
      this.addProduct(product);
    } catch (error) {
      if (error instanceof ApiError && error.status === 404) throw new Error('PRODUCT_NOT_FOUND');
      throw error;
    }
  }

  addProduct(product: Product): void {
    const available = sellableOf(product);
    if (available < 1) throw new Error(product.stock > 0 ? 'NO_SELLABLE_STOCK' : 'INSUFFICIENT_STOCK');
    const existing = this.cartState().find(item => item.productId === product.id);
    const quantity = (existing?.quantity ?? 0) + 1;
    if (quantity > available) throw new Error('INSUFFICIENT_STOCK');
    const updated: SaleItem = {
      productId: product.id, productName: product.name, unitPrice: product.price,
      quantity, subtotal: product.price * quantity
    };
    this.cartState.update(items => existing
      ? items.map(item => item.productId === product.id ? updated : item)
      : [...items, updated]);
  }

  changeQuantity(productId: string, delta: number): void {
    const product = this.products().find(item => item.id === productId);
    const cartItem = this.cartState().find(item => item.productId === productId);
    if (!product || !cartItem) return;
    const quantity = cartItem.quantity + delta;
    if (quantity < 1) throw new Error('INVALID_QUANTITY');
    if (quantity > sellableOf(product)) throw new Error('INSUFFICIENT_STOCK');
    this.cartState.update(items => items.map(item => item.productId === productId
      ? { ...item, quantity, subtotal: item.unitPrice * quantity } : item));
  }

  remove(productId: string): void { this.cartState.update(items => items.filter(item => item.productId !== productId)); }
  cancel(): void { this.cartState.set([]); }

  async complete(payment: PaymentRequest): Promise<Sale> {
    if (!this.cashOpen()) throw new Error('CASH_CLOSED');
    if (!this.cartState().length) throw new Error('EMPTY_CART');
    if (payment.method === 'CASH' && (payment.received ?? 0) < this.total()) throw new Error('INSUFFICIENT_AMOUNT');
    const user = this.auth.currentUser();
    if (!user) throw new Error('CASH_CLOSED');
    const response = await firstValueFrom(this.api.create({
      items: this.cartState().map(item => ({ productId: item.productId, quantity: item.quantity })),
      paymentMethod: payment.method,
      amountReceived: payment.received,
      storeId: STORE_ID,
      operatorId: user.id
    }));
    const sale = toSale(response);
    this.sales.update(items => [sale, ...items.filter(item => item.id !== sale.id)]);
    this.cartState.set([]);
    await this.loadProducts();
    await this.cash.refresh();
    return sale;
  }

  async loadHistory(query: SaleQuery = {}): Promise<void> {
    const request = ++this.historyRequest;
    this.loadingSales.set(true);
    this.salesError.set(null);
    try {
      const [response, summary] = await Promise.all([
        firstValueFrom(this.api.list(query)), firstValueFrom(this.api.summary(query))
      ]);
      if (request !== this.historyRequest) return;
      this.sales.set(response.content.map(toSale));
      this.completedCounterTotal.set(summary.completedCounterTotal);
      this.pageState.set({ page: response.number, size: response.size, totalElements: response.totalElements, totalPages: response.totalPages });
    } catch (error) {
      if (request === this.historyRequest) this.salesError.set(error instanceof ApiError ? error.message : 'Não foi possível carregar o histórico de vendas.');
    } finally {
      if (request === this.historyRequest) this.loadingSales.set(false);
    }
  }

  async findById(id: string): Promise<Sale> {
    const sale = toSale(await firstValueFrom(this.api.getById(id)));
    this.sales.update(items => [sale, ...items.filter(item => item.id !== sale.id)]);
    return sale;
  }

  private upsertProduct(product: Product): void {
    this.products.update(items => [product, ...items.filter(item => item.id !== product.id)]);
  }
}
