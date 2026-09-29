import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../../core/auth/auth.service';
import { ApiError } from '../../../core/http/api-error';
import { CashMovement, CashRegister } from '../../../core/models/domain.models';
import { ApiCashSummary, ApiCloseCashRequest, ApiOpenCashRequest } from '../models/cash-api.models';
import { toCashMovement, toCashRegister } from './cash.mapper';
import { CashApiService } from './cash-api.service';

export interface OpenCashRequest { initialBalance: number; }
export interface CloseCashRequest { countedBalance: number; justification?: string; }

const STORE_ID = '00000000-0000-0000-0000-000000000001';

@Injectable({ providedIn: 'root' })
export class CashService {
  private readonly api = inject(CashApiService);
  private readonly auth = inject(AuthService);
  readonly register = signal<CashRegister | null>(null);
  readonly movements = signal<CashMovement[]>([]);
  readonly summary = signal<ApiCashSummary | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly cashSales = computed(() => Number(this.summary()?.cashSales ?? 0));
  readonly cashExpenses = computed(() => Number(this.summary()?.cashExpenses ?? 0));
  readonly cashRefunds = computed(() => Number(this.summary()?.cashRefunds ?? 0));
  readonly expected = computed(() => Number(this.summary()?.expectedBalance ?? this.register()?.expectedBalance ?? 0));
  readonly cashOpen = computed(() => this.register()?.status === 'OPEN');

  constructor() { void this.refresh(); }

  async refresh(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const current = await this.api.current(STORE_ID);
      if (!current) {
        this.register.set(null);
        this.summary.set(null);
        this.movements.set([]);
        return;
      }
      this.register.set(toCashRegister(current));
      const [summary, movements] = await Promise.all([
        firstValueFrom(this.api.summary(current.id)),
        firstValueFrom(this.api.movements(current.id))
      ]);
      this.summary.set(summary);
      this.movements.set(movements.map(toCashMovement));
      this.register.set(toCashRegister({ ...current, expectedBalance: summary.expectedBalance }));
    } catch (error) {
      this.error.set(error instanceof ApiError ? error.message : 'Não foi possível consultar o caixa.');
    } finally {
      this.loading.set(false);
    }
  }

  async open(request: OpenCashRequest): Promise<CashRegister> {
    const user = this.auth.currentUser();
    const payload: ApiOpenCashRequest = {
      initialBalance: request.initialBalance,
      storeId: STORE_ID,
      operatorId: user?.id ?? '00000000-0000-0000-0000-000000000102',
      operator: user?.name ?? 'Operador'
    };
    const register = toCashRegister(await firstValueFrom(this.api.open(payload)));
    this.register.set(register);
    await this.refresh();
    return register;
  }

  async close(request: CloseCashRequest): Promise<CashRegister> {
    const register = this.register();
    if (!register) throw new ApiError(404, 'CASH_CLOSED', 'Não existe caixa aberto para fechamento.');
    const response = toCashRegister(await firstValueFrom(this.api.close(register.id, request as ApiCloseCashRequest)));
    this.register.set(response);
    await this.refresh();
    return response;
  }
}
