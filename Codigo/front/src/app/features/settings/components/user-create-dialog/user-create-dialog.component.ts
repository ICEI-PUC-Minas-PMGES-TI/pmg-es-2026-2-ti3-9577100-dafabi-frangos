import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Role } from '../../../../core/models/domain.models';

@Component({
  selector: 'app-user-create-dialog', standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  template: `<h2 mat-dialog-title>Novo usuário</h2><form [formGroup]="form" (ngSubmit)="save()"><mat-dialog-content class="fields">
    <mat-form-field appearance="outline"><mat-label>Nome completo</mat-label><input matInput formControlName="name" autocomplete="name"></mat-form-field>
    <mat-form-field appearance="outline"><mat-label>E-mail</mat-label><input matInput type="email" formControlName="email" autocomplete="email"></mat-form-field>
    <mat-form-field appearance="outline"><mat-label>Senha inicial</mat-label><input matInput type="password" formControlName="password" autocomplete="new-password"><mat-hint>Mínimo de 8 caracteres</mat-hint></mat-form-field>
    <mat-form-field appearance="outline"><mat-label>Perfil</mat-label><mat-select formControlName="role"><mat-option value="CASHIER">Operador de caixa</mat-option><mat-option value="ADMIN">Administradora</mat-option></mat-select></mat-form-field>
  </mat-dialog-content><mat-dialog-actions align="end"><button mat-button type="button" mat-dialog-close>Cancelar</button><button mat-flat-button class="primary-button" type="submit" [disabled]="form.invalid">Cadastrar usuário</button></mat-dialog-actions></form>`,
  styles: [`.fields{display:grid;grid-template-columns:1fr 1fr;gap:6px 12px;padding-top:8px;min-width:min(520px,80vw)}@media(max-width:600px){.fields{grid-template-columns:1fr;min-width:0}}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UserCreateDialogComponent {
  private readonly dialog = inject(MatDialogRef<UserCreateDialogComponent>);
  readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(100)] }),
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email, Validators.maxLength(254)] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(8), Validators.maxLength(100)] }),
    role: new FormControl<Role>('CASHIER', { nonNullable: true, validators: [Validators.required] })
  });

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.dialog.close(this.form.getRawValue());
  }
}
