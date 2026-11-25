import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { ApiError } from '../../../../core/http/api-error';

export interface PasswordDialogData {
  /** "reset": a Administradora define uma senha temporária; "own": o usuário troca a própria senha. */
  mode: 'reset' | 'own';
  userName?: string;
  submit: (value: { currentPassword: string; newPassword: string }) => Promise<unknown>;
}

@Component({
  selector: 'app-password-dialog', standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatDialogModule, MatFormFieldModule, MatInputModule],
  template: `<h2 mat-dialog-title>{{ data.mode === 'own' ? 'Trocar minha senha' : 'Redefinir senha' }}</h2>
    <form [formGroup]="form" (ngSubmit)="save()"><mat-dialog-content class="fields">
      @if (data.mode === 'reset') { <p class="note">Defina uma senha temporária para {{ data.userName }} e informe a pessoa. Ela poderá trocá-la em “Trocar minha senha”.</p> }
      @if (data.mode === 'own') { <mat-form-field appearance="outline"><mat-label>Senha atual</mat-label><input matInput type="password" formControlName="currentPassword" autocomplete="current-password"></mat-form-field> }
      <mat-form-field appearance="outline"><mat-label>Nova senha</mat-label><input matInput type="password" formControlName="newPassword" autocomplete="new-password"><mat-hint>Mínimo de 8 caracteres</mat-hint></mat-form-field>
      <mat-form-field appearance="outline"><mat-label>Confirme a nova senha</mat-label><input matInput type="password" formControlName="confirmation" autocomplete="new-password"></mat-form-field>
      @if (error()) { <p class="error" role="alert">{{ error() }}</p> }
    </mat-dialog-content><mat-dialog-actions align="end"><button mat-button type="button" mat-dialog-close [disabled]="saving()">Cancelar</button><button mat-flat-button class="primary-button" type="submit" [disabled]="saving()">{{ saving() ? 'Salvando…' : 'Salvar senha' }}</button></mat-dialog-actions></form>`,
  styles: [`.fields{display:grid;gap:6px;padding-top:8px;min-width:min(400px,80vw)}.note{margin:0 0 8px;font-size:.8rem;color:var(--muted);line-height:1.45}.error{margin:0;color:var(--red-dark);font-size:.8rem;font-weight:700}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PasswordDialogComponent {
  readonly data = inject<PasswordDialogData>(MAT_DIALOG_DATA);
  private readonly dialog = inject(MatDialogRef<PasswordDialogComponent>);
  readonly saving = signal(false);
  readonly error = signal('');
  readonly form = new FormGroup({
    currentPassword: new FormControl('', { nonNullable: true, validators: this.data.mode === 'own' ? [Validators.required] : [] }),
    newPassword: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(8), Validators.maxLength(100)] }),
    confirmation: new FormControl('', { nonNullable: true, validators: [Validators.required] })
  });

  async save(): Promise<void> {
    this.error.set('');
    if (this.form.invalid) { this.form.markAllAsTouched(); this.error.set('Preencha os campos; a nova senha precisa de pelo menos 8 caracteres.'); return; }
    const value = this.form.getRawValue();
    if (value.newPassword !== value.confirmation) { this.error.set('A confirmação não confere com a nova senha.'); return; }
    this.saving.set(true);
    try {
      await this.data.submit({ currentPassword: value.currentPassword, newPassword: value.newPassword });
      this.dialog.close(true);
    } catch (error) {
      this.error.set(error instanceof ApiError ? error.message : 'Não foi possível salvar a senha.');
    } finally {
      this.saving.set(false);
    }
  }
}
