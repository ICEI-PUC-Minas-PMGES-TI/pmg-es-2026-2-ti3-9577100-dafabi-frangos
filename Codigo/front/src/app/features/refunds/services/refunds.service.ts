import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { Refund } from '../../../core/models/domain.models';
import { ApiSale } from '../../sales/models/sales-api.models';
import { SalesApiService } from '../../sales/services/sales-api.service';
import { ApiRefund, ApiRefundEligibility, RefundRequest, RefundSaleSummary } from '../models/refund.models';

@Injectable({ providedIn: 'root' })
export class RefundsService {
  private readonly http = inject(HttpClient);
  private readonly sales = inject(SalesApiService);
  private readonly baseUrl = `${environment.apiBaseUrl}/refunds`;

  async list(page = 0, size = 20): Promise<Refund[]> {
    const response = await firstValueFrom(this.http.get<{ content: ApiRefund[] }>(this.baseUrl, {
      params: { page, size, sort: 'refundedAt,desc' }
    }));
    return response.content.map(refund => this.toDomain(refund));
  }

  async find(id: string): Promise<Refund> {
    return this.toDomain(await firstValueFrom(this.http.get<ApiRefund>(`${this.baseUrl}/${id}`)));
  }

  async findSale(query: string): Promise<RefundSaleSummary | null> {
    const normalized = query.trim().replace(/^#/, '');
    if (!normalized) return null;

    const page = await firstValueFrom(this.sales.list({ query: normalized, page: 0, size: 50 }));
    const sale: ApiSale | undefined = page.content.find(item =>
      String(item.saleNumber) === normalized || item.id.toLowerCase() === normalized.toLowerCase()
    );
    if (!sale) return null;

    const eligibility = await firstValueFrom(this.http.get<ApiRefundEligibility>(
      `${environment.apiBaseUrl}/sales/${sale.id}/refund-eligibility`
    ));
    return {
      id: eligibility.saleId,
      number: `#${eligibility.saleNumber}`,
      createdAt: eligibility.dateTime,
      origin: eligibility.origin === 'FOOD_99' ? '99FOOD' : eligibility.origin,
      paymentMethod: eligibility.paymentMethod,
      total: Number(eligibility.total),
      cashAmountToReverse: Number(eligibility.cashAmountToReverse),
      eligible: eligibility.eligible,
      ineligibilityReason: eligibility.ineligibilityReason,
      items: eligibility.items.map(item => ({
        productId: item.productId,
        productName: item.productName,
        quantity: Number(item.quantity),
        unitPrice: Number(item.unitPrice),
        subtotal: Number(item.subtotal)
      }))
    };
  }

  async create(request: RefundRequest): Promise<Refund> {
    const key = globalThis.crypto?.randomUUID?.() ?? `${Date.now()}-${Math.random().toString(16).slice(2)}`;
    const response = await firstValueFrom(this.http.post<ApiRefund>(
      `${environment.apiBaseUrl}/sales/${request.saleId}/refund`,
      { reason: request.reason.trim() },
      { headers: new HttpHeaders({ 'Idempotency-Key': key }) }
    ));
    return this.toDomain(response);
  }

  private toDomain(refund: ApiRefund): Refund {
    return {
      id: refund.id,
      saleId: refund.saleId,
      saleNumber: `#${refund.saleNumber}`,
      date: refund.refundedAt,
      user: refund.refundedBy,
      reason: refund.reason,
      amount: Number(refund.amount),
      paymentMethod: refund.paymentMethod,
      status: 'COMPLETED'
    };
  }
}
