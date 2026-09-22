import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { ExpenseFormComponent } from '../../components/expense-form/expense-form.component';
import { ExpenseFormValue } from '../../models/expense.models';
import { ExpensesService } from '../../services/expenses.service';

@Component({
  selector: 'app-expense-edit', standalone: true,
  imports: [EmptyStateComponent, PageHeaderComponent, ExpenseFormComponent],
  template: `<app-page-header eyebrow="Despesas" title="Editar despesa" subtitle="Atualize os dados antes da consolidação."/>@if(expense){<app-expense-form [expense]="expense" [saving]="saving()" (saved)="save($event)" (cancel)="router.navigate(['/app/despesas'])"/>}@else{<app-empty-state icon="search_off" title="Despesa não encontrada" message="O registro solicitado não existe."/>}`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExpenseEditComponent {
  readonly router = inject(Router);
  private readonly service = inject(ExpensesService);
  private readonly snack = inject(MatSnackBar);
  private readonly id = inject(ActivatedRoute).snapshot.paramMap.get('id') ?? '';
  readonly expense = this.service.find(this.id);
  readonly saving = signal(false);

  async save(value: ExpenseFormValue) {
    if (!this.expense || this.expense.status !== 'PENDING') return;
    this.saving.set(true);
    try {
      await this.service.save(value, this.id);
      this.snack.open('Despesa atualizada com sucesso.', 'Fechar', { duration: 3500 });
      await this.router.navigate(['/app/despesas']);
    } finally { this.saving.set(false); }
  }
}
