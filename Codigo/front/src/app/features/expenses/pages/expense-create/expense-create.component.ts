import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiError } from '../../../../core/http/api-error';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { ExpenseFormComponent } from '../../components/expense-form/expense-form.component';
import { ExpenseFormValue } from '../../models/expense.models';
import { ExpensesService } from '../../services/expenses.service';

@Component({ selector: 'app-expense-create', standalone: true, imports: [PageHeaderComponent, ExpenseFormComponent], template: `<app-page-header eyebrow="Despesas" title="Registrar despesa" subtitle="Informe os dados da saída financeira."/><app-expense-form [saving]="saving()" (saved)="save($event)" (cancel)="router.navigate(['/app/despesas'])"/>`, changeDetection: ChangeDetectionStrategy.OnPush })
export class ExpenseCreateComponent {
  readonly router = inject(Router); private readonly service = inject(ExpensesService); private readonly snack = inject(MatSnackBar); readonly saving = signal(false);
  async save(value: ExpenseFormValue): Promise<void> { this.saving.set(true); try { await this.service.save(value); this.snack.open('Despesa registrada com sucesso.', 'Fechar', { duration: 3500 }); await this.router.navigate(['/app/despesas']); } catch (error) { this.snack.open(error instanceof ApiError ? error.message : 'Não foi possível registrar a despesa.', 'Fechar', { duration: 4500 }); } finally { this.saving.set(false); } }
}
