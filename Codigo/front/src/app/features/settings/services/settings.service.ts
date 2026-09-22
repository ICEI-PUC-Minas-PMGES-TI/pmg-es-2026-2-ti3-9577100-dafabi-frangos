import { Injectable, signal } from '@angular/core';
import { User } from '../../../core/models/domain.models';
import { PermissionRow, StoreSettings } from '../models/settings.models';

@Injectable({ providedIn: 'root' })
export class SettingsService {
  readonly store = signal<StoreSettings>({ name: 'DaFabi Frangos', phone: '(31) 99123-4567', address: 'Rua das Palmeiras, 125 — Belo Horizonte/MG', defaultPayment: 'PIX', compactMode: false });
  readonly users = signal<User[]>([
    { id: 'u1', name: 'Fabiana Nogueira', email: 'admin@dafabi.com.br', role: 'ADMIN', active: true },
    { id: 'u2', name: 'Carlos Oliveira', email: 'caixa@dafabi.com.br', role: 'CASHIER', active: true },
    { id: 'u3', name: 'Marina Souza', email: 'marina@dafabi.com.br', role: 'CASHIER', active: false }
  ]);
  readonly permissions: PermissionRow[] = [
    { area: 'Dashboard financeiro', admin: true, cashier: false }, { area: 'PDV e vendas', admin: true, cashier: true },
    { area: 'Produtos e estoque', admin: true, cashier: true }, { area: 'Caixa', admin: true, cashier: true },
    { area: 'Compras e fornecedores', admin: true, cashier: false }, { area: 'Despesas', admin: true, cashier: false },
    { area: 'Integrações', admin: true, cashier: false }, { area: 'Estornos', admin: true, cashier: false },
    { area: 'Usuários e configurações', admin: true, cashier: false }
  ];
  async saveStore(value: StoreSettings) { await new Promise(resolve => setTimeout(resolve, 400)); this.store.set(value); }
  toggleUser(id: string) { this.users.update(items => items.map(user => user.id === id ? { ...user, active: !user.active } : user)); }
  addCashier() { this.users.update(items => [...items, { id: `u${Date.now()}`, name: 'Novo Operador', email: `operador${items.length + 1}@dafabi.com.br`, role: 'CASHIER', active: true }]); }
}
