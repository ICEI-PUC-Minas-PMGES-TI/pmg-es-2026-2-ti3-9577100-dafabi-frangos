import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { PerishableProduct, Product, StockMovement } from '../../../core/models/domain.models';
import { PerishableRequest, StockAdjustmentRequest } from '../models/inventory.models';
import { toInventoryProduct, toPerishableProduct, toStockMovement } from './inventory.mapper';
import { InventoryApiService } from './inventory-api.service';

@Injectable({ providedIn: 'root' })
export class InventoryService {
  readonly products = signal<Product[]>([]);
  readonly movements = signal<StockMovement[]>([]);
  readonly perishables = signal<PerishableProduct[]>([]);

  private readonly api = inject(InventoryApiService);
  private readonly auth = inject(AuthService);

  async loadPositions(query = ''): Promise<void> {
    const positions = await firstValueFrom(this.api.positions(query));
    this.products.set(positions.map(toInventoryProduct));
    this.movements.set(positions
      .map(position => position.lastMovement)
      .filter((movement): movement is NonNullable<typeof movement> => movement !== null)
      .map(toStockMovement));
  }

  async loadMovements(productId?: string): Promise<void> {
    const movements = await firstValueFrom(this.api.movements(productId));
    this.movements.set(movements.map(toStockMovement));
  }

  async adjust(request: StockAdjustmentRequest): Promise<StockMovement> {
    const response = await firstValueFrom(this.api.adjust({
      ...request,
      performedBy: this.auth.currentUser()?.name
    }));
    await this.loadPositions();
    return toStockMovement(response);
  }

  async loadPerishables(): Promise<void> {
    const lots = await firstValueFrom(this.api.lots());
    this.perishables.set(lots.map(toPerishableProduct));
  }

  async addPerishable(request: PerishableRequest): Promise<PerishableProduct> {
    const response = await firstValueFrom(this.api.createLot({
      ...request,
      performedBy: this.auth.currentUser()?.name
    }));
    const lot = toPerishableProduct(response);
    this.perishables.update(items => [lot, ...items]);
    await this.loadPositions();
    return lot;
  }
}
