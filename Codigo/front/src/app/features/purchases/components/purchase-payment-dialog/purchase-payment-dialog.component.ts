import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { ApiError } from '../../../../core/http/api-error';
import { PaymentMethod, Purchase } from '../../../../core/models/domain.models';

export interface PurchasePaymentDialogData { purchase: Purchase; label: string; submit: (method: PaymentMethod) => Promise<unknown>; }

/** Registro do pagamento de uma compra: a forma é escolhida no próprio diálogo, sem pré-seleção. */
@Component({
  selector: 'app-purchase-payment-dialog', standalone: true,
  imports: [CurrencyPipe, MatButtonModule, MatDialogModule, MatIconModule],
  template: `<h2 mat-dialog-title>Registrar pagamento</h2><mat-dialog-content>
    <p class="intro">{{ data.label }} · {{ data.purchase.supplierName }} · <strong>{{ data.purchase.total | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong></p>
    <p class="question" id="method-label">Como a compra foi paga?</p>
    <div class="methods" role="radiogroup" aria-labelledby="method-label">@for (m of methods; track m.value) {
      <button type="button" role="radio" [attr.aria-checked]="method() === m.value" [class.active]="method() === m.value" [disabled]="saving()" (click)="method.set(m.value); error.set('')"><mat-icon aria-hidden="true">{{ m.icon }}</mat-icon>{{ m.label }}</button>
    }</div>
    @if (method() === 'CASH') { <p class="hint"><mat-icon aria-hidden="true">info</mat-icon>Pagamento em dinheiro sai do caixa aberto.</p> }
    @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
  </mat-dialog-content><mat-dialog-actions align="end"><button mat-button mat-dialog-close [disabled]="saving()">Voltar</button><button mat-flat-button class="primary-button" [disabled]="saving()" (click)="save()">{{ saving() ? 'Registrando…' : 'Registrar pagamento' }}</button></mat-dialog-actions>`,
  styles: [`.intro{margin:0 0 14px;font-size:.84rem}.question{margin:0 0 8px;font-size:.78rem;font-weight:700}.methods{display:grid;grid-template-columns:repeat(auto-fit,minmax(130px,1fr));gap:8px;min-width:min(460px,78vw)}.methods button{display:flex;align-items:center;gap:8px;padding:10px 12px;border:1px solid var(--border);border-radius:9px;background:#fff;font:inherit;font-size:.8rem;font-weight:700;cursor:pointer}.methods button.active{border:2px solid var(--red);background:#fff7f5;color:var(--red-dark)}.hint{display:flex;gap:6px;align-items:center;font-size:.76rem;color:var(--muted);margin:12px 0 0}.hint mat-icon{font-size:17px;width:17px;height:17px}.error{margin:12px 0 0;color:var(--red-dark);font-size:.8rem;font-weight:700}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PurchasePaymentDialogComponent {
  readonly data = inject<PurchasePaymentDialogData>(MAT_DIALOG_DATA);
  private readonly ref = inject(MatDialogRef<PurchasePaymentDialogComponent>);
  readonly method = signal<PaymentMethod | null>(null);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly methods: { value: PaymentMethod; label: string; icon: string }[] = [
    { value: 'CASH', label: 'Dinheiro', icon: 'payments' }, { value: 'PIX', label: 'Pix', icon: 'qr_code_2' },
    { value: 'DEBIT', label: 'Débito', icon: 'credit_card' }, { value: 'CREDIT', label: 'Crédito', icon: 'credit_score' },
    { value: 'BOLETO_TRANSFER' as PaymentMethod, label: 'Boleto/transferência', icon: 'account_balance' }
  ];

  async save(): Promise<void> {
    const method = this.method();
    if (!method) { this.error.set('Escolha a forma de pagamento.'); return; }
    this.saving.set(true); this.error.set('');
    try { await this.data.submit(method); this.ref.close(true); }
    catch (error) { this.error.set(error instanceof ApiError ? error.message : 'Não foi possível registrar o pagamento.'); this.saving.set(false); }
  }
}
