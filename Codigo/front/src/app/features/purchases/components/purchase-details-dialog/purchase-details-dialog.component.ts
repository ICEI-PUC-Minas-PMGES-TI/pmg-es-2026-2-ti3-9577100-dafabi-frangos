import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { Purchase } from '../../../../core/models/domain.models';
import { QuantityPipe } from '../../../../shared/pipes/quantity.pipe';
import { paymentLabel } from '../../../../shared/utils/labels';
import { PurchasesService } from '../../services/purchases.service';

/** Detalhe de uma compra com os itens recebidos. */
@Component({
  selector: 'app-purchase-details-dialog', standalone: true,
  imports: [CurrencyPipe, DatePipe, MatButtonModule, MatDialogModule, QuantityPipe],
  template: `<h2 mat-dialog-title>{{ data.label }}</h2><mat-dialog-content>
    <dl class="meta"><div><dt>Fornecedor</dt><dd>{{ data.purchase.supplierName }}</dd></div><div><dt>Data</dt><dd>{{ data.purchase.date | date:'dd/MM/yyyy' }}</dd></div><div><dt>Pagamento</dt><dd>{{ paymentText() }}</dd></div></dl>
    @if (loading()) { <p class="muted">Carregando itens…</p> }
    @else if (error()) { <p class="error" role="alert">{{ error() }}</p> }
    @else { <div class="table-wrap"><table class="data-table"><thead><tr><th>Produto</th><th>Qtd.</th><th>Custo un.</th><th>Lote / validade</th><th>Total</th></tr></thead><tbody>@for (item of items(); track $index) {
      <tr><td class="cell-title">{{ item.productName }}</td><td>{{ item.quantity | qty }}</td><td class="money">{{ item.unitCost | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</td><td>{{ item.batch || '—' }}@if (item.expiry) { <div class="cell-meta">vence {{ item.expiry | date:'dd/MM/yyyy' }}</div> }</td><td class="money">{{ item.total | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</td></tr>
    }</tbody><tfoot><tr><td colspan="4">Total da compra</td><td class="money">{{ data.purchase.total | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</td></tr></tfoot></table></div> }
  </mat-dialog-content><mat-dialog-actions align="end"><button mat-flat-button class="primary-button" mat-dialog-close>Fechar</button></mat-dialog-actions>`,
  styles: [`.meta{display:grid;grid-template-columns:repeat(3,1fr);gap:12px;margin:0 0 16px;min-width:min(620px,80vw)}.meta dt{font-size:.7rem;color:var(--muted)}.meta dd{margin:2px 0 0;font-weight:700;font-size:.84rem}tfoot td{font-weight:800;padding-top:10px}.muted{color:var(--muted)}.error{color:var(--red-dark);font-weight:700}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PurchaseDetailsDialogComponent {
  readonly data = inject<{ purchase: Purchase; label: string }>(MAT_DIALOG_DATA);
  private readonly service = inject(PurchasesService);
  readonly items = signal(this.data.purchase.items);
  readonly loading = signal(!this.data.purchase.items.length);
  readonly error = signal('');

  constructor() {
    if (this.loading()) this.service.findById(this.data.purchase.id)
      .then(purchase => this.items.set(purchase.items))
      .catch(() => this.error.set('Não foi possível carregar os itens da compra.'))
      .finally(() => this.loading.set(false));
  }

  paymentText(): string {
    const purchase = this.data.purchase;
    if (purchase.status === 'CANCELED') return 'Compra cancelada';
    if (purchase.paymentStatus !== 'PAID') return 'Pendente';
    return purchase.paymentMethod ? `Pago · ${paymentLabel(purchase.paymentMethod)}` : 'Pago';
  }
}
