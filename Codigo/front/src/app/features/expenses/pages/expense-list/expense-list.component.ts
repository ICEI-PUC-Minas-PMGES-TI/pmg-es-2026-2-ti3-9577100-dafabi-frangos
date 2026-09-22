import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ConfirmationDialogComponent } from '../../../../shared/components/confirmation-dialog/confirmation-dialog.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { StatusChipComponent } from '../../../../shared/components/status-chip/status-chip.component';
import { ExpensesService } from '../../services/expenses.service';

@Component({
  selector: 'app-expense-list', standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink, MatButtonModule, MatIconModule, EmptyStateComponent, PageHeaderComponent, StatusChipComponent],
  template: `
    <app-page-header eyebrow="Financeiro" title="Despesas" subtitle="Registre saídas e acompanhe o impacto no resultado."/>
    <div class="expense-summary"><div><small>Total no período</small><strong>{{total()|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</strong></div><span><mat-icon>trending_down</mat-icon>Considerado no resultado estimado</span></div>
    <div class="toolbar"><input class="filter-select" type="date" aria-label="Data inicial" [value]="start()" (change)="start.set($any($event.target).value)"><span>até</span><input class="filter-select" type="date" aria-label="Data final" [value]="end()" (change)="end.set($any($event.target).value)"><select class="filter-select" aria-label="Status" (change)="status.set($any($event.target).value)"><option value="ALL">Todos os status</option><option value="PENDING">Pendente</option><option value="CONSOLIDATED">Consolidada</option><option value="CANCELED">Cancelada</option></select><span class="grow"></span><a mat-flat-button class="primary-button" routerLink="/app/despesas/nova"><mat-icon>add</mat-icon>Nova despesa</a></div>
    <section class="surface">@if(!filtered().length){<app-empty-state icon="receipt_long" title="Nenhuma despesa encontrada" message="Ajuste o período ou registre uma nova despesa."/>}@else{<div class="table-wrap"><table class="data-table"><thead><tr><th>Descrição</th><th>Categoria</th><th>Data</th><th>Pagamento</th><th>Valor</th><th>Status</th><th>Ações</th></tr></thead><tbody>@for(e of filtered();track e.id){<tr><td><div class="cell-title">{{e.description}}</div>@if(e.notes){<div class="cell-meta">{{e.notes}}</div>}</td><td>{{e.category}}</td><td>{{e.date|date:'dd/MM/yyyy'}}</td><td>{{paymentLabel(e.paymentMethod)}}</td><td class="money">{{e.amount|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</td><td><app-status-chip [status]="e.status"/></td><td>@if(e.status==='PENDING'){<a mat-button [routerLink]="['/app/despesas',e.id,'editar']">Editar</a><button mat-button class="cancel" (click)="cancelExpense(e.id,e.description)">Cancelar</button>}@else{<span class="cell-meta">Somente consulta</span>}</td></tr>}</tbody></table></div>}</section>`,
  styles: [`.expense-summary{display:flex;align-items:center;justify-content:space-between;background:var(--yellow-light);border:1px solid #f1d66f;border-radius:12px;padding:15px 20px;margin-bottom:18px}.expense-summary small,.expense-summary strong{display:block}.expense-summary small{color:#76520e}.expense-summary strong{font-size:1.35rem;color:var(--red-dark);margin-top:3px}.expense-summary>span{display:flex;align-items:center;gap:7px;font-size:.75rem;color:#76520e}.grow{flex:1}.cancel{color:var(--red-dark)}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExpenseListComponent {
  readonly service = inject(ExpensesService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  readonly start = signal('2026-09-01');
  readonly end = signal('2026-09-30');
  readonly status = signal('ALL');
  readonly filtered = computed(() => this.service.expenses().filter(expense =>
    (!this.start() || expense.date >= this.start()) && (!this.end() || expense.date <= this.end()) &&
    (this.status() === 'ALL' || expense.status === this.status())));
  readonly total = computed(() => this.filtered().filter(expense => expense.status !== 'CANCELED').reduce((sum, expense) => sum + expense.amount, 0));

  paymentLabel(value: string) { return { CASH: 'Dinheiro', PIX: 'Pix', DEBIT: 'Débito', CREDIT: 'Crédito' }[value] ?? value; }
  cancelExpense(id: string, name: string) {
    this.dialog.open(ConfirmationDialogComponent, { data: {
      title: 'Cancelar despesa?', message: `A despesa “${name}” deixará de impactar o resultado e o caixa.`,
      confirmLabel: 'Cancelar despesa', destructive: true
    }}).afterClosed().subscribe(async ok => {
      if (!ok) return;
      try {
        await this.service.cancel(id);
        this.snack.open('Despesa cancelada.', 'Fechar', { duration: 3000 });
      } catch { this.snack.open('Despesas consolidadas não podem ser canceladas.', 'Fechar', { duration: 3500 }); }
    });
  }
}
