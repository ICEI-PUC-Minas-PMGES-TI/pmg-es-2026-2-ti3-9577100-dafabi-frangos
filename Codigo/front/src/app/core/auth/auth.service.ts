import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Role, User } from '../models/domain.models';

export const SESSION_KEY = 'dafabi.session';

export interface AuthSession {
  accessToken: string;
  refreshToken?: string;
  expiresAt?: string;
  user: User;
}

interface ApiLoginResponse {
  accessToken: string;
  refreshToken?: string;
  tokenType: string;
  expiresIn: number;
  expiresAt?: string;
  user: {
    id: string;
    name: string;
    email: string;
    role: Role;
    active: boolean;
    permissions?: string[];
  };
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly sessionState = signal<AuthSession | null>(this.restoreSession());

  readonly currentUser = computed<User | null>(() => this.sessionState()?.user ?? null);
  readonly accessToken = computed<string | null>(() => this.sessionState()?.accessToken ?? null);
  readonly isAuthenticated = computed(() => !!this.sessionState()?.accessToken && !!this.sessionState()?.user);
  readonly isAdmin = computed(() => this.currentUser()?.role === 'ADMIN');

  async login(identifier: string, password: string): Promise<User> {
    const response = await firstValueFrom(
      this.http.post<ApiLoginResponse>(`${environment.apiBaseUrl}/auth/login`, {
        identifier: identifier.trim(),
        password
      })
    );

    const user: User = {
      id: response.user.id,
      name: response.user.name,
      email: response.user.email,
      role: response.user.role,
      active: response.user.active
    };

    const session: AuthSession = {
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
      expiresAt: response.expiresAt,
      user
    };

    this.sessionState.set(session);
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
    return user;
  }

  hasRole(roles: User['role'][]): boolean {
    const current = this.currentUser();
    return !!current && roles.includes(current.role);
  }

  logout(expired = false): void {
    const currentSession = this.sessionState();
    this.sessionState.set(null);
    sessionStorage.removeItem(SESSION_KEY);

    if (!expired && currentSession?.accessToken) {
      firstValueFrom(
        this.http.post(`${environment.apiBaseUrl}/auth/logout`, {
          refreshToken: currentSession.refreshToken ?? null
        })
      ).catch(() => undefined);
    }

    void this.router.navigate(['/login'], {
      queryParams: expired ? { motivo: 'sessao-expirada' } : undefined
    });
  }

  private restoreSession(): AuthSession | null {
    try {
      const raw = sessionStorage.getItem(SESSION_KEY);
      if (!raw) return null;

      const parsed = JSON.parse(raw) as Partial<AuthSession> | null;
      const user = parsed?.user;
      if (!parsed?.accessToken || !user?.id || !user.name || !user.email) {
        sessionStorage.removeItem(SESSION_KEY);
        return null;
      }

      const role = user.role === 'ADMIN' ? 'ADMIN' : user.role === 'CASHIER' ? 'CASHIER' : null;
      if (!role) {
        sessionStorage.removeItem(SESSION_KEY);
        return null;
      }

      if (parsed.expiresAt && new Date(parsed.expiresAt).getTime() <= Date.now()) {
        sessionStorage.removeItem(SESSION_KEY);
        return null;
      }

      return {
        accessToken: parsed.accessToken,
        refreshToken: parsed.refreshToken,
        expiresAt: parsed.expiresAt,
        user: {
          id: user.id,
          name: user.name,
          email: user.email,
          role,
          active: user.active !== false
        }
      };
    } catch {
      sessionStorage.removeItem(SESSION_KEY);
      return null;
    }
  }
}
