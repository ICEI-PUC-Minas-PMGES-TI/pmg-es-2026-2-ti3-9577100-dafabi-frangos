import { paymentLabel } from '../../../../shared/utils/labels';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Expense } from '../../../../core/models/domain.models';
import { ConfirmationDialogComponent } from '../../../../shared/components/confirmation-dialog/confirmation-dialog.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../../../shared/components/error-state/error-state.component';
import { LoadingStateComponent } from '../../../../shared/components/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { StatusChipComponent } from '../../../../shared/components/status-chip/status-chip.component';
import { ExpensesService } from '../../services/expenses.service';

@Component({
  selector: 'app-expense-list', standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink, MatButtonModule, MatIconModule, EmptyStateComponent, ErrorStateComponent, LoadingStateComponent, PageHeaderComponent, StatusChipComponent],
  template: `<app-page-header eyebrow="Despesas" title="Despesas" subtitle="Uma despesa só afeta o resultado e o caixa quando for marcada como paga." actionLabel="Nova despesa" actionLink="/app/despesas/nova"/>
    <div class="expense-summary"><div><small>Despesas pagas no período</small><strong>{{total()|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</strong></div><span><mat-icon>trending_down</mat-icon>Consideradas no resultado estimado</span></div>
    @if(pendingCount()){<div class="notice warning pending-note" role="status"><mat-icon>schedule</mat-icon><span><strong>{{pendingCount()}} {{pendingCount()===1?'despesa pendente':'despesas pendentes'}}</strong> no período. Elas ainda não entram no resultado nem saem do caixa; use “Marcar como paga” quando o pagamento acontecer.</span></div>}<div class="toolbar"><input class="filter-select" type="date" aria-label="Data inicial" [value]="start()" (change)="start.set($any($event.target).value);reload()"><span>até</span><input class="filter-select" type="date" aria-label="Data final" [value]="end()" (change)="end.set($any($event.target).value);reload()"><select class="filter-select" aria-label="Status" [value]="status()" (change)="status.set($any($event.target).value);reload()"><option value="ALL">Todos os status</option><option value="PENDING">Pendente</option><option value="CONSOLIDATED">Paga</option><option value="CANCELLED">Cancelada</option></select><span class="grow"></span></div>
    <section class="surface">@if(service.loading()){<app-loading-state label="Carregando despesas…"/>}@else if(service.error()){<app-error-state title="Não foi possível carregar as despesas" [message]="service.error()!"/>}@else if(!filtered().length){<app-empty-state icon="receipt_long" title="Nenhuma despesa encontrada" message="Ajuste o período ou registre uma nova despesa."/>}@else{<div class="table-wrap"><table class="data-table"><thead><tr><th>Descrição</th><th>Categoria</th><th>Data</th><th>Pagamento</th><th>Valor</th><th>Status</th><th>Ações</th></tr></thead><tbody>@for(e of filtered();track e.id){<tr><td><div class="cell-title">{{e.description}}</div>@if(e.notes){<div class="cell-meta">{{e.notes}}</div>}</td><td>{{e.category}}</td><td>{{e.date|date:'dd/MM/yyyy'}}</td><td>{{paymentLabel(e.paymentMethod)}}</td><td class="money">{{e.amount|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</td><td><app-status-chip [status]="e.status"/>@if(e.paidAt){<div class="cell-meta">Paga em {{e.paidAt|date:'dd/MM/yyyy HH:mm'}}</div>}@if(e.reversedAt){<div class="cell-meta">Estornada em {{e.reversedAt|date:'dd/MM/yyyy HH:mm'}} · {{e.reversalReason}}</div>}</td><td>@if(e.status==='PENDING'){<button mat-button (click)="consolidateExpense(e)" [disabled]="busyId()===e.id"><mat-icon>payments</mat-icon>Marcar como paga</button><a mat-button [routerLink]="['/app/despesas',e.id,'editar']">Editar</a><button mat-button class="cancel" (click)="cancelExpense(e.id,e.description)">Cancelar</button>}@else if(e.status==='CONSOLIDATED'){<button mat-button class="cancel" (click)="reverseExpense(e)" [disabled]="busyId()===e.id"><mat-icon>undo</mat-icon>Estornar pagamento</button>}@else{<span class="cell-meta">Somente consulta</span>}</td></tr>}</tbody></table></div>}</section>`,
  styles: [`.expense-summary{display:flex;align-items:center;justify-content:space-between;background:var(--yellow-light);border:1px solid #f1d66f;border-radius:12px;padding:15px 20px;margin-bottom:18px}.expense-summary small,.expense-summary strong{display:block}.expense-summary small{color:#76520e}.expense-summary strong{font-size:1.35rem;color:var(--red-dark);margin-top:3px}.expense-summary>span{display:flex;align-items:center;gap:7px;font-size:.75rem;color:#76520e}.grow{flex:1}.cancel{color:var(--red-dark)}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExpenseListComponent {
  readonly service = inject(ExpensesService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  readonly start = signal(new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().slice(0, 10));
  readonly end = signal(new Date().toISOString().slice(0, 10));
  readonly status = signal('ALL');
  readonly busyId = signal<string | null>(null);
  readonly filtered = computed(() => this.service.expenses().filter(expense =>
    (!this.start() || expense.date >= this.start()) && (!this.end() || expense.date <= this.end())));
  readonly pendingCount = computed(() => this.filtered().filter(expense => expense.status === 'PENDING').length);
  readonly total = computed(() => this.filtered().filter(expense => expense.status === 'CONSOLIDATED')
    .reduce((sum, expense) => sum + expense.amount, 0));

  constructor() { void this.reload(); }

  async reload(): Promise<void> {
    await this.service.load({ startDate: this.start(), endDate: this.end(), status: this.status() as 'ALL' | 'PENDING' | 'CONSOLIDATED' | 'CANCELLED' });
  }

  readonly paymentLabel = paymentLabel;

  consolidateExpense(expense: Expense): void {
    this.dialog.open(ConfirmationDialogComponent, { data: {
      title: 'Marcar despesa como paga?',
      message: `A despesa será reconhecida no resultado. Se paga em dinheiro, também sairá do caixa aberto.`,
      confirmLabel: 'Confirmar pagamento',
      confirmAction: async () => { this.busyId.set(expense.id); try { await this.service.consolidate(expense.id); } finally { this.busyId.set(null); } }
    } }).afterClosed().subscribe(ok => { if (ok) this.snack.open('Despesa marcada como paga.', 'Fechar', { duration: 3500 }); });
  }

  reverseExpense(expense: Expense): void {
    this.dialog.open(ConfirmationDialogComponent, { data: {
      title: 'Estornar pagamento?',
      message: `O pagamento de “${expense.description}” deixa de contar no resultado${expense.paymentMethod === 'CASH' ? ' e o valor volta para o caixa aberto' : ''}. O registro original é mantido.`,
      confirmLabel: 'Estornar pagamento', destructive: true, reasonLabel: 'Motivo do estorno',
      confirmAction: async (reason?: string) => { this.busyId.set(expense.id); try { await this.service.reverse(expense.id, reason ?? ''); } finally { this.busyId.set(null); } }
    } }).afterClosed().subscribe(ok => { if (ok) this.snack.open('Pagamento estornado.', 'Fechar', { duration: 3500 }); });
  }

  cancelExpense(id: string, name: string): void {
    this.dialog.open(ConfirmationDialogComponent, { data: {
      title: 'Cancelar despesa?', message: `A despesa “${name}” pendente será cancelada sem movimentar o caixa.`,
      confirmLabel: 'Cancelar despesa', destructive: true,
      confirmAction: () => this.service.cancel(id)
    } }).afterClosed().subscribe(ok => { if (ok) this.snack.open('Despesa cancelada.', 'Fechar', { duration: 3000 }); });
  }
}
