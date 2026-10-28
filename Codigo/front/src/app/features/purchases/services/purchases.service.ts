import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { Product, Purchase } from '../../../core/models/domain.models';
import { ProductsService } from '../../products/services/products.service';
import { SuppliersService } from '../../suppliers/services/suppliers.service';
import { PurchaseDraft } from '../models/purchase.models';
import { PurchasesApiService } from './purchases-api.service';
import { toCreatePurchaseRequest, toPurchase } from './purchases.mapper';

@Injectable({ providedIn: 'root' })
export class PurchasesService {
  private readonly api = inject(PurchasesApiService); private readonly auth = inject(AuthService); private readonly productsService = inject(ProductsService); private readonly suppliersService = inject(SuppliersService);
  readonly purchases = signal<Purchase[]>([]); readonly loading = signal(false); readonly loadingData = signal(false); readonly error = signal<string | null>(null);
  readonly products = this.productsService.products; readonly suppliers = this.suppliersService.suppliers;

  async load(supplierId?: string): Promise<void> {
    this.loading.set(true); this.error.set(null);
    try { this.purchases.set((await firstValueFrom(this.api.list(supplierId))).map(toPurchase)); }
    catch (error) { this.error.set(error instanceof Error ? error.message : 'Não foi possível carregar as compras.'); }
    finally { this.loading.set(false); }
  }

  async loadFormData(): Promise<void> {
    this.loadingData.set(true); this.error.set(null);
    try {
      await Promise.all([
        this.suppliers().length ? Promise.resolve() : this.suppliersService.load(),
        this.products().length ? Promise.resolve() : this.productsService.load({ status: 'ACTIVE', size: 100 })
      ]);
    } catch (error) {
      this.error.set(error instanceof Error ? error.message : 'Não foi possível carregar os dados da compra.');
    } finally { this.loadingData.set(false); }
  }

  async create(draft: PurchaseDraft): Promise<Purchase> {
    const response = await firstValueFrom(this.api.create(toCreatePurchaseRequest(draft, this.products(), this.auth.currentUser()?.name)));
    const purchase = toPurchase(response); this.purchases.update(items => [purchase, ...items]);
    await Promise.all([this.load(), this.productsService.load({ status: 'ACTIVE', size: 100 })]);
    return purchase;
  }
}
