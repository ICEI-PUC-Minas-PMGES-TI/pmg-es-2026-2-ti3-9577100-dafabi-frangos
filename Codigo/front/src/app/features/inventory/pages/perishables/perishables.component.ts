import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDrawer, MatSidenavModule } from '@angular/material/sidenav';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiError } from '../../../../core/http/api-error';
import { ErrorStateComponent } from '../../../../shared/components/error-state/error-state.component';
import { LoadingStateComponent } from '../../../../shared/components/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { StatusChipComponent } from '../../../../shared/components/status-chip/status-chip.component';
import { InventoryService } from '../../services/inventory.service';

const today = new Date().toISOString().slice(0, 10);

@Component({
  selector: 'app-perishables',
  standalone: true,
  imports: [DatePipe, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule, MatSidenavModule, ErrorStateComponent, LoadingStateComponent, PageHeaderComponent, StatusChipComponent],
  template: `
    <mat-drawer-container class="drawer-shell" [hasBackdrop]="true">
      <mat-drawer #drawer position="end" mode="over" class="form-drawer">
        <div class="drawer-head"><div><p class="eyebrow">Perecíveis</p><h2>Registrar lote</h2></div><button mat-icon-button (click)="drawer.close()" aria-label="Fechar"><mat-icon>close</mat-icon></button></div>
        <form [formGroup]="form" (ngSubmit)="save(drawer)">
          <mat-form-field appearance="outline"><mat-label>Produto</mat-label><mat-select formControlName="productId">@for(p of perishableProducts();track p.id){<mat-option [value]="p.id">{{p.name}}</mat-option>}</mat-select></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Lote</mat-label><input matInput formControlName="batch"></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Data de entrada</mat-label><input matInput type="date" formControlName="receivedAt"></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Validade</mat-label><input matInput type="date" formControlName="expiry"></mat-form-field>
          <mat-form-field appearance="outline"><mat-label>Quantidade recebida</mat-label><input matInput type="number" min="1" formControlName="quantity"></mat-form-field>
          <button mat-flat-button class="primary-button" type="submit" [disabled]="saving()">{{saving()?'Registrando…':'Registrar lote'}}</button>
        </form>
      </mat-drawer>
      <mat-drawer-content>
        <app-page-header eyebrow="Estoque" title="Produtos perecíveis" subtitle="Consulte e registre lotes, quantidades e validades." actionLabel="Registrar lote" (action)="drawer.open()"/>
        <section class="surface">
          @if(loading()) { <app-loading-state/> }
          @else if(error()) { <app-error-state title="Não foi possível carregar os lotes" [message]="error()!"/> }
          @else { <div class="table-wrap"><table class="data-table"><thead><tr><th>Produto</th><th>Lote</th><th>Data de entrada</th><th>Validade</th><th>Quantidade</th><th>Status</th></tr></thead><tbody>@for(item of service.perishables();track item.id){<tr><td class="cell-title">{{item.productName}}</td><td>{{item.batch}}</td><td>{{item.receivedAt|date:'dd/MM/yyyy'}}</td><td>{{item.expiry|date:'dd/MM/yyyy'}}</td><td>{{item.quantity}} un</td><td><app-status-chip [status]="item.status"/></td></tr>}@empty{<tr><td colspan="6" class="empty-row">Nenhum lote registrado.</td></tr>}</tbody></table></div><div class="perishable-note"><mat-icon>info</mat-icon>Registrar um lote também atualiza o saldo de estoque do produto.</div> }
        </section>
      </mat-drawer-content>
    </mat-drawer-container>
  `,
  styles: [`.drawer-shell{background:transparent;min-height:calc(100vh - 130px)}.form-drawer{width:min(430px,92vw);padding:24px}.drawer-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:24px}.drawer-head h2{margin:3px 0 0}.form-drawer form{display:grid;gap:6px}.form-drawer form>button{height:46px}.perishable-note{display:flex;align-items:center;gap:9px;color:var(--muted);font-size:.76rem;padding:14px 18px;border-top:1px solid var(--border)}.empty-row{text-align:center;color:var(--muted);padding:28px}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PerishablesComponent {
  readonly service = inject(InventoryService);
  private readonly snack = inject(MatSnackBar);
  readonly saving = signal(false);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly perishableProducts = computed(() => this.service.products().filter(product => product.perishable));
  readonly form = new FormGroup({
    productId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    batch: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    receivedAt: new FormControl(today, { nonNullable: true, validators: [Validators.required] }),
    expiry: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    quantity: new FormControl(1, { nonNullable: true, validators: [Validators.required, Validators.min(1)] })
  });

  constructor() { void this.load(); }

  async load(): Promise<void> {
    try {
      await Promise.all([this.service.loadPositions(), this.service.loadPerishables()]);
    } catch (error) {
      this.error.set(error instanceof ApiError ? error.message : 'Tente novamente em alguns instantes.');
    } finally {
      this.loading.set(false);
    }
  }

  async save(drawer: MatDrawer): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving.set(true);
    try {
      await this.service.addPerishable(this.form.getRawValue());
      drawer.close();
      this.form.reset({ productId: '', batch: '', receivedAt: today, expiry: '', quantity: 1 });
      this.snack.open('Lote registrado e estoque atualizado.', 'Fechar', { duration: 3500 });
    } catch (error) {
      this.snack.open(error instanceof ApiError ? error.message : 'Não foi possível registrar o lote.', 'Fechar', { duration: 4500 });
    } finally {
      this.saving.set(false);
    }
  }
}
