import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Role, User } from '../../../../core/models/domain.models';

/** Edição de nome e perfil de um usuário existente. */
@Component({
  selector: 'app-user-edit-dialog', standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `<h2 mat-dialog-title>Editar usuário</h2><form [formGroup]="form" (ngSubmit)="save()"><mat-dialog-content class="fields">
    <p class="email">{{ user.email }}</p>
    <mat-form-field appearance="outline"><mat-label>Nome completo</mat-label><input matInput formControlName="name" autocomplete="name">@if(form.controls.name.invalid&&form.controls.name.touched){<mat-error>Informe o nome.</mat-error>}</mat-form-field>
    <mat-form-field appearance="outline"><mat-label>Perfil</mat-label><mat-select formControlName="role"><mat-option value="CASHIER">Operador de caixa</mat-option><mat-option value="ADMIN">Administradora</mat-option></mat-select></mat-form-field>
  </mat-dialog-content><mat-dialog-actions align="end"><button mat-button type="button" mat-dialog-close>Cancelar</button><button mat-flat-button class="primary-button" type="submit">Salvar alterações</button></mat-dialog-actions></form>`,
  styles: [`.fields{display:grid;gap:6px;padding-top:8px;min-width:min(420px,80vw)}.email{margin:0 0 8px;color:var(--muted);font-size:.8rem}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserEditDialogComponent {
  readonly user = inject<User>(MAT_DIALOG_DATA);
  private readonly dialog = inject(MatDialogRef<UserEditDialogComponent>);
  readonly form = new FormGroup({
    name: new FormControl(this.user.name, { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    role: new FormControl<Role>(this.user.role, { nonNullable: true, validators: [Validators.required] })
  });

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.dialog.close(this.form.getRawValue());
  }
}
