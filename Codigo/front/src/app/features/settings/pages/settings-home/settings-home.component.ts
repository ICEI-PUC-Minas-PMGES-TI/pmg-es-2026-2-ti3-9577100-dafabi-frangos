import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { SettingsNavComponent } from '../../components/settings-nav/settings-nav.component';
import { SettingsService } from '../../services/settings.service';

@Component({
  selector: 'app-settings-home',
  standalone: true,
  imports: [
    ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatFormFieldModule, MatIconModule,
    MatInputModule, MatProgressBarModule, MatSelectModule, PageHeaderComponent, SettingsNavComponent
  ],
  template: `
    <app-page-header eyebrow="Administração" title="Configurações" subtitle="Dados básicos da loja e preferências gerais." />
    <app-settings-nav />
    @if (loading()) {
      <mat-progress-bar mode="indeterminate" aria-label="Carregando configurações da loja" />
      <p class="state-message" role="status">Carregando configurações da loja…</p>
    } @else if (loadError()) {
      <section class="surface surface-body state-message" role="alert">
        <mat-icon aria-hidden="true">error_outline</mat-icon>
        <p>Não foi possível carregar as configurações.</p>
        <button mat-stroked-button type="button" (click)="load()">Tentar novamente</button>
      </section>
    } @else {
      <form class="surface surface-body form-card form-grid" [formGroup]="form" (ngSubmit)="save()">
        <h2 class="full">Dados da loja</h2>
        <mat-form-field appearance="outline">
          <mat-label>Nome da loja</mat-label>
          <input matInput formControlName="name" autocomplete="organization" />
          @if (form.controls.name.hasError('required')) { <mat-error>Informe o nome da loja.</mat-error> }
        </mat-form-field>
        <mat-form-field appearance="outline">
          <mat-label>Telefone</mat-label>
          <input matInput formControlName="phone" autocomplete="tel" />
          @if (form.controls.phone.hasError('required')) { <mat-error>Informe o telefone.</mat-error> }
        </mat-form-field>
        <mat-form-field appearance="outline" class="full">
          <mat-label>Endereço</mat-label>
          <input matInput formControlName="address" autocomplete="street-address" />
        </mat-form-field>
        <h2 class="full section-title">Preferências gerais</h2>
        <mat-form-field appearance="outline">
          <mat-label>Pagamento sugerido no PDV</mat-label>
          <mat-select formControlName="defaultPayment">
            <mat-option value="PIX">Pix</mat-option><mat-option value="CASH">Dinheiro</mat-option>
            <mat-option value="DEBIT">Débito</mat-option><mat-option value="CREDIT">Crédito</mat-option>
          </mat-select>
        </mat-form-field>
        <div class="full"><mat-checkbox formControlName="compactMode">Usar modo compacto nas tabelas</mat-checkbox></div>
        <div class="form-actions">
          <button mat-flat-button class="primary-button" type="submit" [disabled]="saving() || form.invalid">
            <mat-icon>save</mat-icon>{{ saving() ? 'Salvando…' : 'Salvar configurações' }}
          </button>
        </div>
      </form>
    }
  `,
  styles: [`
    h2{font-size:1rem;margin:0 0 8px}.section-title{padding-top:12px;border-top:1px solid var(--border)}
    .state-message{display:flex;align-items:center;gap:12px;margin-top:16px;color:var(--text-secondary)}
    .state-message p{margin:0}.state-message[role=alert]{flex-wrap:wrap}
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SettingsHomeComponent implements OnInit {
  private readonly service = inject(SettingsService);
  private readonly snack = inject(MatSnackBar);
  readonly loading = signal(true);
  readonly loadError = signal(false);
  readonly saving = signal(false);
  readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(120)] }),
    phone: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(40)] }),
    address: new FormControl('', { nonNullable: true, validators: [Validators.maxLength(250)] }),
    defaultPayment: new FormControl('PIX', { nonNullable: true, validators: [Validators.required] }),
    compactMode: new FormControl(false, { nonNullable: true })
  });

  ngOnInit(): void { void this.load(); }

  async load(): Promise<void> {
    this.loading.set(true);
    this.loadError.set(false);
    try {
      this.form.patchValue(await this.service.loadStore());
    } catch {
      this.loadError.set(true);
    } finally {
      this.loading.set(false);
    }
  }

  async save(): Promise<void> {
    if (this.form.invalid || this.saving()) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    try {
      this.form.patchValue(await this.service.saveStore(this.form.getRawValue()));
      this.snack.open('Configurações salvas com sucesso.', 'Fechar', { duration: 3500 });
    } catch {
      this.snack.open('Não foi possível salvar. Verifique sua conexão e tente novamente.', 'Fechar', { duration: 5000 });
    } finally {
      this.saving.set(false);
    }
  }
}
