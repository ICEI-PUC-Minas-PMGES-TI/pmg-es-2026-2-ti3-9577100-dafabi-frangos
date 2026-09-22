import { Injectable, computed, signal } from '@angular/core';
import { Router } from '@angular/router';
import { User } from '../models/domain.models';

const SESSION_KEY = 'dafabi.session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly currentUserState = signal<User | null>(this.restoreSession());
  readonly currentUser = this.currentUserState.asReadonly();
  readonly isAuthenticated = computed(() => !!this.currentUserState());
  readonly isAdmin = computed(() => this.currentUserState()?.role === 'ADMIN');

  constructor(private readonly router: Router) {}

  async login(identifier: string, password: string): Promise<User> {
    await this.delay(650);
    const digest = await this.hash(password);
    const adminHash = await this.hash('admin123');
    const cashierHash = await this.hash('caixa123');
    let user: User | undefined;

    if (['admin', 'admin@dafabi.com.br'].includes(identifier.trim().toLowerCase()) && digest === adminHash) {
      user = { id: 'u1', name: 'Fabiana Nogueira', email: 'admin@dafabi.com.br', role: 'ADMIN', active: true };
    }
    if (['caixa', 'caixa@dafabi.com.br'].includes(identifier.trim().toLowerCase()) && digest === cashierHash) {
      user = { id: 'u2', name: 'Carlos Oliveira', email: 'caixa@dafabi.com.br', role: 'CASHIER', active: true };
    }
    if (!user) throw new Error('INVALID_CREDENTIALS');

    this.currentUserState.set(user);
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(user));
    return user;
  }

  hasRole(roles: User['role'][]): boolean { return !!this.currentUserState() && roles.includes(this.currentUserState()!.role); }

  logout(expired = false): void {
    this.currentUserState.set(null);
    sessionStorage.removeItem(SESSION_KEY);
    void this.router.navigate(['/login'], { queryParams: expired ? { motivo: 'sessao-expirada' } : undefined });
  }

  private restoreSession(): User | null {
    try {
      const stored = JSON.parse(sessionStorage.getItem(SESSION_KEY) ?? 'null') as Partial<User> | null;
      if (!stored?.id || !stored.name || !stored.email) return null;
      const role = stored.role === 'ADMIN' ? 'ADMIN' : stored.role === 'CASHIER' ? 'CASHIER' : null;
      if (!role) {
        sessionStorage.removeItem(SESSION_KEY);
        return null;
      }
      return { id: stored.id, name: stored.name, email: stored.email, role, active: stored.active !== false };
    } catch {
      sessionStorage.removeItem(SESSION_KEY);
      return null;
    }
  }

  private async hash(value: string): Promise<string> {
    const data = new TextEncoder().encode(value);
    const buffer = await crypto.subtle.digest('SHA-256', data);
    return Array.from(new Uint8Array(buffer)).map(byte => byte.toString(16).padStart(2, '0')).join('');
  }

  private delay(ms: number): Promise<void> { return new Promise(resolve => setTimeout(resolve, ms)); }
}
