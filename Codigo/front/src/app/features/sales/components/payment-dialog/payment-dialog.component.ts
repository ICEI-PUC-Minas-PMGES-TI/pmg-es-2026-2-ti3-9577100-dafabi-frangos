import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { PaymentMethod, SaleItem } from '../../../../core/models/domain.models';
import { PaymentRequest } from '../../models/sales.models';

export interface PaymentDialogData { total: number; items: SaleItem[]; }

@Component({
  selector: 'app-payment-dialog', standalone: true,
  imports: [CurrencyPipe, ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule],
  template: `
    <div class="payment-dialog">
      <header><div><p class="eyebrow">Finalizar venda</p><h2>Pagamento</h2></div><button mat-icon-button (click)="ref.close()" aria-label="Fechar"><mat-icon>close</mat-icon></button></header>
      <div class="payment-total"><span>Total a receber</span><strong>{{ data.total | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong><small>{{ data.items.length }} produto(s) no carrinho</small></div>
      <section>
        <h3>Forma de pagamento</h3>
        <div class="payment-methods">@for (method of methods; track method.value) { <button type="button" [class.active]="selected() === method.value" (click)="selected.set(method.value)"><mat-icon>{{ method.icon }}</mat-icon><span>{{ method.label }}</span></button> }</div>
        @if (selected() === 'CASH') {
          <div class="cash-fields">
            <mat-form-field appearance="outline"><mat-label>Valor recebido</mat-label><span matTextPrefix>R$&nbsp;</span><input matInput type="number" min="0" step="0.01" [formControl]="received"></mat-form-field>
            <div class="change" [class.invalid]="change() < 0"><span>Troco</span><strong>{{ change() > 0 ? (change() | currency:'BRL':'symbol':'1.2-2':'pt-BR') : 'R$ 0,00' }}</strong></div>
            @if (received.touched && change() < 0) { <p class="field-error"><mat-icon>error</mat-icon>O valor recebido é insuficiente.</p> }
          </div>
        }
        <div class="summary-line"><span>Resumo</span><strong>{{ data.items.length }} produto(s) · {{ itemUnits }} unidade(s)</strong></div>
      </section>
      <footer><button mat-button (click)="ref.close()">Voltar</button><button mat-flat-button class="primary-button" [disabled]="processing()" (click)="confirm()">@if (processing()) { <span>Processando…</span> } @else { <span>Confirmar pagamento</span><mat-icon>check_circle</mat-icon> }</button></footer>
    </div>`,
  styles: [`
    .payment-dialog{width:min(560px,90vw)}header{display:flex;justify-content:space-between;align-items:center;padding:22px 24px 12px}header h2{margin:3px 0 0;font-size:1.6rem}.payment-total{margin:0 24px;padding:17px 20px;border-radius:12px;background:var(--yellow-light);display:grid;grid-template-columns:1fr auto;align-items:center}.payment-total span{font-size:.78rem;font-weight:700}.payment-total strong{font-size:1.65rem;color:var(--red-dark);grid-row:span 2}.payment-total small{color:#76520e;margin-top:3px}section{padding:20px 24px}section h3{font-size:.8rem;margin:0 0 10px}.payment-methods{display:grid;grid-template-columns:repeat(4,1fr);gap:8px}.payment-methods button{height:80px;border:1px solid var(--border);background:white;border-radius:10px;display:grid;place-items:center;align-content:center;gap:6px;cursor:pointer}.payment-methods button.active{border:2px solid var(--red);background:#fff7f5;color:var(--red-dark)}.payment-methods span{font-size:.72rem;font-weight:700}.cash-fields{display:grid;grid-template-columns:1fr 1fr;gap:12px;margin-top:16px}.change{height:56px;border:1px solid var(--border);border-radius:8px;padding:8px 14px}.change span,.change strong{display:block}.change span{color:var(--muted);font-size:.7rem}.change strong{margin-top:3px}.change.invalid strong{color:var(--red)}.field-error{grid-column:1/-1;color:var(--red-dark);font-size:.75rem;display:flex;align-items:center;margin:0}.field-error mat-icon{font-size:17px;width:17px;height:17px}.summary-line{margin-top:18px;padding-top:14px;border-top:1px solid var(--border);display:flex;justify-content:space-between;font-size:.76rem}.summary-line span{color:var(--muted)}footer{padding:14px 24px 22px;display:flex;justify-content:flex-end;gap:8px}@media(max-width:520px){.payment-methods{grid-template-columns:repeat(2,1fr)}.cash-fields{grid-template-columns:1fr}}
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PaymentDialogComponent {
  readonly data = inject<PaymentDialogData>(MAT_DIALOG_DATA);
  readonly ref = inject(MatDialogRef<PaymentDialogComponent>);
  readonly selected = signal<PaymentMethod>('PIX'); readonly processing = signal(false);
  readonly received = new FormControl<number | null>(null, [Validators.min(0)]);
  readonly methods = [{ value: 'CASH' as const, label: 'Dinheiro', icon: 'payments' }, { value: 'PIX' as const, label: 'Pix', icon: 'qr_code_2' }, { value: 'DEBIT' as const, label: 'Débito', icon: 'credit_card' }, { value: 'CREDIT' as const, label: 'Crédito', icon: 'credit_score' }];
  change(): number { return (this.received.value ?? 0) - this.data.total; }
  get itemUnits(): number { return this.data.items.reduce((sum, item) => sum + item.quantity, 0); }
  confirm(): void { if (this.selected() === 'CASH' && (this.received.value ?? 0) < this.data.total) { this.received.markAsTouched(); return; } this.processing.set(true); setTimeout(() => this.ref.close({ method: this.selected(), received: this.received.value ?? undefined } satisfies PaymentRequest), 300); }
}
