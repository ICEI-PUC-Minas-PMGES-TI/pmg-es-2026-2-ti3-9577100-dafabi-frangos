import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Supplier } from '../../../core/models/domain.models';
import { SupplierFormValue } from '../models/supplier.models';
import { SuppliersApiService } from './suppliers-api.service';
import { toSupplier, toSupplierRequest } from './suppliers.mapper';

@Injectable({ providedIn: 'root' })
export class SuppliersService {
  private readonly api = inject(SuppliersApiService);
  readonly suppliers = signal<Supplier[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  async load(query = ''): Promise<void> {
    this.loading.set(true); this.error.set(null);
    try { this.suppliers.set((await firstValueFrom(this.api.list(query))).map(toSupplier)); }
    catch (error) { this.error.set(error instanceof Error ? error.message : 'Não foi possível carregar os fornecedores.'); }
    finally { this.loading.set(false); }
  }

  async find(id: string): Promise<Supplier> {
    const cached = this.suppliers().find(item => item.id === id);
    if (cached) return cached;
    const supplier = toSupplier(await firstValueFrom(this.api.getById(id)));
    this.upsert(supplier); return supplier;
  }

  async save(value: SupplierFormValue, id?: string): Promise<Supplier> {
    const response = id
      ? await firstValueFrom(this.api.update(id, toSupplierRequest(value)))
      : await firstValueFrom(this.api.create(toSupplierRequest(value)));
    const supplier = toSupplier(response); this.upsert(supplier); return supplier;
  }

  async updateStatus(id: string, active: boolean): Promise<Supplier> {
    const supplier = toSupplier(await firstValueFrom(this.api.updateStatus(id, active)));
    this.upsert(supplier); return supplier;
  }

  private upsert(supplier: Supplier): void {
    this.suppliers.update(items => {
      const index = items.findIndex(item => item.id === supplier.id);
      return index < 0 ? [supplier, ...items] : items.map(item => item.id === supplier.id ? supplier : item);
    });
  }
}
