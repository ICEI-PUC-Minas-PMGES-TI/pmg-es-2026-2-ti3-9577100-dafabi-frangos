import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiError } from '../../../../core/http/api-error';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../../../shared/components/error-state/error-state.component';
import { LoadingStateComponent } from '../../../../shared/components/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { ExpenseFormComponent } from '../../components/expense-form/expense-form.component';
import { ExpenseFormValue } from '../../models/expense.models';
import { ExpensesService } from '../../services/expenses.service';
import { Expense } from '../../../../core/models/domain.models';

@Component({ selector: 'app-expense-edit', standalone: true, imports: [EmptyStateComponent, ErrorStateComponent, LoadingStateComponent, PageHeaderComponent, ExpenseFormComponent], template: `<app-page-header eyebrow="Despesas" title="Editar despesa" subtitle="Atualize os dados antes da consolidação."/>@if(loading()){<app-loading-state/>}@else if(error()){<app-error-state title="Não foi possível carregar a despesa" [message]="error()!"/>}@else if(expense()){<app-expense-form [expense]="expense()!" [saving]="saving()" (saved)="save($event)" (cancel)="router.navigate(['/app/despesas'])"/>}@else{<app-empty-state icon="search_off" title="Despesa não encontrada" message="O registro solicitado não existe."/>}`, changeDetection: ChangeDetectionStrategy.OnPush })
export class ExpenseEditComponent {
  readonly router = inject(Router); private readonly service = inject(ExpensesService); private readonly snack = inject(MatSnackBar); private readonly id = inject(ActivatedRoute).snapshot.paramMap.get('id') ?? '';
  readonly expense = signal<Expense | null>(null); readonly loading = signal(true); readonly saving = signal(false); readonly error = signal<string | null>(null);
  constructor() { void this.load(); }
  async load(): Promise<void> { try { this.expense.set(await this.service.find(this.id)); } catch (error) { this.error.set(error instanceof ApiError ? error.message : 'Tente novamente em alguns instantes.'); } finally { this.loading.set(false); } }
  async save(value: ExpenseFormValue): Promise<void> { const expense = this.expense(); if (!expense || expense.status !== 'PENDING') return; this.saving.set(true); try { await this.service.save(value, this.id); this.snack.open('Despesa atualizada com sucesso.', 'Fechar', { duration: 3500 }); await this.router.navigate(['/app/despesas']); } catch (error) { this.snack.open(error instanceof ApiError ? error.message : 'Não foi possível editar a despesa.', 'Fechar', { duration: 4500 }); } finally { this.saving.set(false); } }
}
