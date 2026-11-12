import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { User } from '../../../core/models/domain.models';
import { PermissionRow, StoreSettings } from '../models/settings.models';

@Injectable({ providedIn: 'root' })
export class SettingsService {
  private readonly http = inject(HttpClient);
  private readonly usersEndpoint = `${environment.apiBaseUrl}/users`;
  private readonly storeEndpoint = `${environment.apiBaseUrl}/settings/store`;
  readonly store = signal<StoreSettings>({ name: 'DaFabi Frangos', phone: '', address: '', defaultPayment: 'PIX', compactMode: false });
  readonly permissions: PermissionRow[] = [
    { area: 'Dashboard financeiro', admin: true, cashier: false }, { area: 'PDV e vendas', admin: true, cashier: true },
    { area: 'Produtos e estoque', admin: true, cashier: true }, { area: 'Caixa', admin: true, cashier: true },
    { area: 'Compras e fornecedores', admin: true, cashier: false }, { area: 'Despesas', admin: true, cashier: false },
    { area: 'Integrações', admin: true, cashier: false }, { area: 'Estornos', admin: true, cashier: false },
    { area: 'Usuários e configurações', admin: true, cashier: false }
  ];
  async loadStore() {
    const value = await firstValueFrom(this.http.get<StoreSettings>(this.storeEndpoint));
    this.store.set(value);
    return value;
  }
  async saveStore(value: StoreSettings) {
    const saved = await firstValueFrom(this.http.put<StoreSettings>(this.storeEndpoint, value));
    this.store.set(saved);
    return saved;
  }
  listUsers() { return firstValueFrom(this.http.get<User[]>(this.usersEndpoint)); }
  createUser(user: { name: string; email: string; password: string; role: User['role'] }) {
    return firstValueFrom(this.http.post<User>(this.usersEndpoint, user));
  }
  setUserActive(id: string, active: boolean) {
    return firstValueFrom(this.http.patch<User>(`${this.usersEndpoint}/${id}/status`, { active }));
  }
}
