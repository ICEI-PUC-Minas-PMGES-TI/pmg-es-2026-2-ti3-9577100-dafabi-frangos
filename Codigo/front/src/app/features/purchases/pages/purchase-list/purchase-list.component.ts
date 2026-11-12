import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PaymentMethod, Purchase } from '../../../../core/models/domain.models';
import { ConfirmationDialogComponent } from '../../../../shared/components/confirmation-dialog/confirmation-dialog.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../../../shared/components/error-state/error-state.component';
import { LoadingStateComponent } from '../../../../shared/components/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { StatusChipComponent } from '../../../../shared/components/status-chip/status-chip.component';
import { PurchasesService } from '../../services/purchases.service';

@Component({
  selector: 'app-purchase-list', standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink, MatButtonModule, MatIconModule, PageHeaderComponent, StatusChipComponent, EmptyStateComponent, ErrorStateComponent, LoadingStateComponent],
  template: `<app-page-header eyebrow="Estoque" title="Compras" subtitle="A compra atualiza o estoque e o custo médio; o pagamento afeta o caixa quando é em dinheiro."/>
    <div class="toolbar"><span class="grow"></span><a mat-flat-button class="primary-button" routerLink="/app/compras/nova"><mat-icon>add</mat-icon>Nova compra</a></div>
    <section class="surface">@if(service.loading()){<app-loading-state/>}@else if(service.error()){<app-error-state title="Não foi possível carregar as compras" [message]="service.error()!"/>}@else if(!service.purchases().length){<app-empty-state icon="shopping_cart" title="Nenhuma compra registrada" message="Registre uma compra para atualizar o estoque." actionLabel="Nova compra" actionLink="/app/compras/nova"/>}@else{
      <div class="table-wrap"><table class="data-table"><thead><tr><th>Compra</th><th>Fornecedor</th><th>Data</th><th>Itens</th><th>Valor total</th><th>Compra</th><th>Pagamento</th><th>Ações</th></tr></thead><tbody>@for(c of service.purchases();track c.id){<tr>
        <td class="cell-title">#{{c.id.slice(0,8)}}</td><td>{{c.supplierName}}</td><td>{{c.date|date:'dd/MM/yyyy'}}</td><td>{{c.itemCount||'—'}}</td><td class="money">{{c.total|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</td>
        <td><app-status-chip [status]="c.status"/></td><td><app-status-chip [status]="c.paymentStatus??'PENDING'"/>@if(c.paymentMethod){<div class="cell-meta">{{paymentLabel(c.paymentMethod)}}</div>}</td>
        <td class="actions">@if(c.status==='REGISTERED'&&c.paymentStatus!=='PAID'){
          <label class="payment-label">Forma de pagamento<select [value]="paymentMethods()[c.id]??'CASH'" (change)="setPaymentMethod(c.id,$any($event.target).value)" aria-label="Forma de pagamento da compra"><option value="CASH">Dinheiro</option><option value="PIX">Pix</option><option value="DEBIT">Débito</option><option value="CREDIT">Crédito</option><option value="BOLETO_TRANSFER">Boleto/transferência</option></select></label>
          <button mat-button (click)="pay(c)" [disabled]="busyId()===c.id"><mat-icon>payments</mat-icon>Registrar pagamento</button>
          <button mat-button class="cancel" (click)="cancel(c)" [disabled]="busyId()===c.id">Cancelar compra</button>
        }@else{<span class="cell-meta">{{c.status==='CANCELED'?'Cancelada':'Pagamento registrado'}}</span>}</td>
      </tr>}</tbody></table></div>}</section>`,
  styles: [`.grow{flex:1}.actions{min-width:245px}.payment-label{display:grid;gap:4px;color:var(--muted);font-size:.68rem}.payment-label select{max-width:180px;padding:6px 8px;border:1px solid var(--border);border-radius:7px;background:#fff;color:var(--text)}.actions button{padding-inline:5px}.cancel{color:var(--red-dark)}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PurchaseListComponent {
  readonly service = inject(PurchasesService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  readonly busyId = signal<string | null>(null);
  readonly paymentMethods = signal<Partial<Record<string, PaymentMethod>>>({});

  constructor() { void this.service.load(); }

  setPaymentMethod(id: string, method: PaymentMethod): void {
    this.paymentMethods.update(methods => ({ ...methods, [id]: method }));
  }

  paymentLabel(method: PaymentMethod): string {
    return ({ CASH: 'Dinheiro', PIX: 'Pix', DEBIT: 'Débito', CREDIT: 'Crédito', BOLETO_TRANSFER: 'Boleto/transferência' })[method];
  }

  pay(purchase: Purchase): void {
    const method = this.paymentMethods()[purchase.id] ?? 'CASH';
    this.dialog.open(ConfirmationDialogComponent, { data: {
      title: 'Registrar pagamento da compra?',
      message: `A compra de ${purchase.total.toLocaleString('pt-BR',{style:'currency',currency:'BRL'})} será marcada como paga por ${this.paymentLabel(method)}.`,
      confirmLabel: 'Registrar pagamento',
      confirmAction: async () => { this.busyId.set(purchase.id); try { await this.service.pay(purchase.id, method); } finally { this.busyId.set(null); } }
    } }).afterClosed().subscribe(ok => { if (ok) this.snack.open('Pagamento da compra registrado.', 'Fechar', { duration: 3500 }); });
  }

  cancel(purchase: Purchase): void {
    this.dialog.open(ConfirmationDialogComponent, { data: {
      title: 'Cancelar compra?',
      message: 'O sistema só cancela se o estoque recebido ainda estiver disponível e o custo médio não tiver sido alterado por outra compra.',
      confirmLabel: 'Cancelar compra', destructive: true,
      confirmAction: async () => { this.busyId.set(purchase.id); try { await this.service.cancel(purchase.id); } finally { this.busyId.set(null); } }
    } }).afterClosed().subscribe(ok => { if (ok) this.snack.open('Compra cancelada e estoque revertido.', 'Fechar', { duration: 3500 }); });
  }
}
