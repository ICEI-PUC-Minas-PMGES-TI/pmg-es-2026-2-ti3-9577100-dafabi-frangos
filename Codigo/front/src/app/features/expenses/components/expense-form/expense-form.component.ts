import { ChangeDetectionStrategy, Component, effect, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Expense } from '../../../../core/models/domain.models';
import { ExpenseFormValue } from '../../models/expense.models';

@Component({
  selector: 'app-expense-form', standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule],
  template: `
    <form class="surface surface-body form-card form-grid" [formGroup]="form" (ngSubmit)="submit()">
      <mat-form-field appearance="outline" class="full"><mat-label>Descrição</mat-label><input matInput formControlName="description" placeholder="Ex.: Compra de embalagens">@if(form.controls.description.touched&&form.controls.description.invalid){<mat-error>Informe uma descrição com pelo menos 3 caracteres.</mat-error>}</mat-form-field>
      <mat-form-field appearance="outline"><mat-label>Categoria</mat-label><mat-select formControlName="category"><mat-option value="Operacional">Operacional</mat-option><mat-option value="Materiais">Materiais</mat-option><mat-option value="Serviços">Serviços</mat-option><mat-option value="Outros">Outros</mat-option></mat-select></mat-form-field>
      <mat-form-field appearance="outline"><mat-label>Valor</mat-label><span matTextPrefix>R$&nbsp;</span><input matInput type="number" min="0.01" step="0.01" formControlName="amount">@if(form.controls.amount.touched&&form.controls.amount.invalid){<mat-error>Informe um valor maior que zero.</mat-error>}</mat-form-field>
      <mat-form-field appearance="outline"><mat-label>Data</mat-label><input matInput type="date" formControlName="date"></mat-form-field>
      <mat-form-field appearance="outline"><mat-label>Forma de pagamento</mat-label><mat-select formControlName="paymentMethod"><mat-option value="CASH">Dinheiro</mat-option><mat-option value="PIX">Pix</mat-option><mat-option value="DEBIT">Débito</mat-option><mat-option value="CREDIT">Crédito</mat-option></mat-select></mat-form-field>
      <mat-form-field appearance="outline" class="full"><mat-label>Observação</mat-label><textarea matInput rows="3" formControlName="notes"></textarea></mat-form-field>
      <div class="form-note"><mat-icon>query_stats</mat-icon>Esta despesa será considerada no resultado estimado. Pagamentos em dinheiro também atualizam o caixa.</div>
      <div class="form-actions"><button mat-button type="button" (click)="cancel.emit()">Cancelar</button><button mat-flat-button class="primary-button" type="submit" [disabled]="saving()"><mat-icon>save</mat-icon>{{saving()?'Salvando…':expense()?'Salvar alterações':'Registrar despesa'}}</button></div>
    </form>`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ExpenseFormComponent {
  readonly expense = input<Expense | null>(null);
  readonly saving = input(false);
  readonly saved = output<ExpenseFormValue>();
  readonly cancel = output<void>();
  readonly form = new FormGroup({
    description: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(3)] }),
    category: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    amount: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(.01)] }),
    date: new FormControl('2026-09-15', { nonNullable: true, validators: [Validators.required] }),
    paymentMethod: new FormControl<'CASH' | 'PIX' | 'DEBIT' | 'CREDIT'>('PIX', { nonNullable: true }),
    notes: new FormControl('', { nonNullable: true }),
    status: new FormControl<'PENDING' | 'CONSOLIDATED' | 'CANCELED'>('PENDING', { nonNullable: true })
  });

  constructor() {
    effect(() => {
      const expense = this.expense();
      if (expense) this.form.patchValue({ ...expense, notes: expense.notes ?? '' }, { emitEvent: false });
    });
  }

  submit() {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const value = this.form.getRawValue();
    this.saved.emit({ ...value, notes: value.notes || undefined });
  }
}
