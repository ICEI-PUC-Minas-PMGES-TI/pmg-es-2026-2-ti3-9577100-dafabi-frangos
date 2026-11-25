import { paymentLabel } from '../../../../shared/utils/labels';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';

import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PaymentMethod, Purchase } from '../../../../core/models/domain.models';
import { ConfirmationDialogComponent } from '../../../../shared/components/confirmation-dialog/confirmation-dialog.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../../../shared/components/error-state/error-state.component';
import { LoadingStateComponent } from '../../../../shared/components/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { StatusChipComponent } from '../../../../shared/components/status-chip/status-chip.component';
import { PurchaseDetailsDialogComponent } from '../../components/purchase-details-dialog/purchase-details-dialog.component';
import { PurchasePaymentDialogComponent } from '../../components/purchase-payment-dialog/purchase-payment-dialog.component';
import { PurchasesService } from '../../services/purchases.service';

type PaymentFilter = 'ALL' | 'PENDING' | 'PAID' | 'CANCELED';

@Component({
  selector: 'app-purchase-list', standalone: true,
  imports: [CurrencyPipe, DatePipe, MatButtonModule, MatIconModule, MatMenuModule, PageHeaderComponent, StatusChipComponent, EmptyStateComponent, ErrorStateComponent, LoadingStateComponent],
  template: `<app-page-header eyebrow="Estoque" title="Compras" subtitle="A compra atualiza o estoque e o custo médio; o pagamento afeta o caixa quando é em dinheiro." actionLabel="Nova compra" actionLink="/app/compras/nova"/>
    <div class="toolbar">
      <label class="filter">Fornecedor<select [value]="supplier()" (change)="supplier.set($any($event.target).value)"><option value="">Todos</option>@for(s of suppliers();track s.id){<option [value]="s.id">{{s.name}}</option>}</select></label>
      <div class="chips" role="group" aria-label="Filtrar por situação">@for(f of filters;track f.value){<button type="button" [class.active]="paymentFilter()===f.value" [attr.aria-pressed]="paymentFilter()===f.value" (click)="paymentFilter.set(f.value)">{{f.label}}</button>}</div>
      
    </div>
    <section class="surface">@if(service.loading()){<app-loading-state/>}@else if(service.error()){<app-error-state title="Não foi possível carregar as compras" [message]="service.error()!"/>}@else if(!service.purchases().length){<app-empty-state icon="shopping_cart" title="Nenhuma compra registrada" message="Registre uma compra para atualizar o estoque." actionLabel="Nova compra" actionLink="/app/compras/nova"/>}@else if(!visible().length){<app-empty-state icon="filter_alt_off" title="Nenhuma compra neste filtro" message="Altere o fornecedor ou a situação para ver outras compras."/>}@else{
      <div class="table-wrap"><table class="data-table"><thead><tr><th>Compra</th><th>Fornecedor</th><th>Data</th><th>Itens</th><th>Valor total</th><th>Compra</th><th>Pagamento</th><th><span class="sr-only">Ações</span></th></tr></thead><tbody>@for(c of visible();track c.id){<tr>
        <td class="cell-title"><button type="button" class="link" (click)="details(c)">{{label(c)}}</button></td><td>{{c.supplierName}}</td><td>{{c.date|date:'dd/MM/yyyy'}}</td><td>{{c.itemCount||'—'}}</td><td class="money">{{c.total|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</td>
        <td><app-status-chip [status]="c.status"/></td><td>@if(c.status==='CANCELED'){<span class="cell-meta">—</span>}@else{<app-status-chip [status]="c.paymentStatus??'PENDING'"/>@if(c.paymentMethod){<div class="cell-meta">{{paymentLabel(c.paymentMethod)}}</div>}}</td>
        <td class="actions">@if(c.status==='REGISTERED'&&c.paymentStatus!=='PAID'){<button mat-stroked-button class="pay" (click)="pay(c)" [disabled]="busyId()===c.id"><mat-icon>payments</mat-icon>Pagar</button>}<button mat-icon-button [matMenuTriggerFor]="menu" [attr.aria-label]="'Mais ações da ' + label(c)"><mat-icon>more_vert</mat-icon></button>
          <mat-menu #menu="matMenu"><button mat-menu-item (click)="details(c)"><mat-icon>receipt_long</mat-icon><span>Ver itens</span></button>@if(c.status==='REGISTERED'&&c.paymentStatus!=='PAID'){<button mat-menu-item class="cancel" (click)="cancel(c)" [disabled]="busyId()===c.id"><mat-icon>block</mat-icon><span>Cancelar compra</span></button>}</mat-menu>
        </td>
      </tr>}</tbody></table></div>}</section>`,
  styles: [`.toolbar{display:flex;flex-wrap:wrap;align-items:flex-end;gap:14px}.grow{flex:1}.filter{display:grid;gap:4px;color:var(--muted);font-size:.68rem;font-weight:700}.filter select{min-width:200px;height:36px;padding:0 10px;border:1px solid var(--border);border-radius:8px;background:#fff;color:var(--text);font:inherit;font-size:.8rem}.chips{display:flex;gap:6px;flex-wrap:wrap}.chips button{border:1px solid var(--border);background:#fff;border-radius:999px;padding:7px 13px;font:inherit;font-size:.75rem;font-weight:700;cursor:pointer}.chips button.active{background:var(--red);border-color:var(--red);color:#fff}.actions{white-space:nowrap;text-align:right}.pay{margin-right:4px}.link{border:0;background:none;padding:0;font:inherit;font-weight:800;color:var(--red-dark);cursor:pointer;text-decoration:underline;text-underline-offset:3px}.cancel{color:var(--red-dark)}.sr-only{position:absolute;width:1px;height:1px;overflow:hidden;clip:rect(0 0 0 0)}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PurchaseListComponent {
  readonly service = inject(PurchasesService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  readonly busyId = signal<string | null>(null);
  readonly supplier = signal('');
  readonly paymentFilter = signal<PaymentFilter>('ALL');
  readonly paymentLabel = paymentLabel;
  readonly filters: { value: PaymentFilter; label: string }[] = [
    { value: 'ALL', label: 'Todas' }, { value: 'PENDING', label: 'A pagar' }, { value: 'PAID', label: 'Pagas' }, { value: 'CANCELED', label: 'Canceladas' }
  ];

  /** Número sequencial de exibição (1 = compra mais antiga), mais fácil de citar que o identificador interno. */
  private readonly numbers = computed(() => {
    const ordered = [...this.service.purchases()].sort((a, b) => a.date.localeCompare(b.date) || a.id.localeCompare(b.id));
    return new Map(ordered.map((purchase, index) => [purchase.id, index + 1]));
  });
  readonly suppliers = computed(() => {
    const map = new Map<string, string>();
    for (const purchase of this.service.purchases()) map.set(purchase.supplierId, purchase.supplierName);
    return [...map].map(([id, name]) => ({ id, name })).sort((a, b) => a.name.localeCompare(b.name, 'pt-BR'));
  });
  readonly visible = computed(() => this.service.purchases().filter(purchase => {
    if (this.supplier() && purchase.supplierId !== this.supplier()) return false;
    switch (this.paymentFilter()) {
      case 'PENDING': return purchase.status === 'REGISTERED' && purchase.paymentStatus !== 'PAID';
      case 'PAID': return purchase.status === 'REGISTERED' && purchase.paymentStatus === 'PAID';
      case 'CANCELED': return purchase.status === 'CANCELED';
      default: return true;
    }
  }));

  constructor() { void this.service.load(); }

  label(purchase: Purchase): string {
    return `Compra nº ${String(this.numbers().get(purchase.id) ?? 0).padStart(3, '0')}`;
  }

  details(purchase: Purchase): void {
    this.dialog.open(PurchaseDetailsDialogComponent, { data: { purchase, label: this.label(purchase) } });
  }

  pay(purchase: Purchase): void {
    this.dialog.open(PurchasePaymentDialogComponent, { data: {
      purchase, label: this.label(purchase),
      submit: async (method: PaymentMethod) => { this.busyId.set(purchase.id); try { await this.service.pay(purchase.id, method); } finally { this.busyId.set(null); } }
    } }).afterClosed().subscribe(ok => { if (ok) this.snack.open('Pagamento da compra registrado.', 'Fechar', { duration: 3500 }); });
  }

  cancel(purchase: Purchase): void {
    this.dialog.open(ConfirmationDialogComponent, { data: {
      title: `Cancelar a ${this.label(purchase)}?`,
      message: 'O sistema só cancela se o estoque recebido ainda estiver disponível e o custo médio não tiver sido alterado por outra compra.',
      confirmLabel: 'Cancelar compra', destructive: true,
      confirmAction: async () => { this.busyId.set(purchase.id); try { await this.service.cancel(purchase.id); } finally { this.busyId.set(null); } }
    } }).afterClosed().subscribe(ok => { if (ok) this.snack.open('Compra cancelada e estoque revertido.', 'Fechar', { duration: 3500 }); });
  }
}
