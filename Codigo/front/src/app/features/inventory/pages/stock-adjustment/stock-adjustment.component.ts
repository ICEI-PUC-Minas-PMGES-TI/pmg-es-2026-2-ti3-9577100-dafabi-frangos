import { QuantityPipe } from '../../../../shared/pipes/quantity.pipe';
import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal, DestroyRef } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiError } from '../../../../core/http/api-error';
import { AuthService } from '../../../../core/auth/auth.service';
import { ErrorStateComponent } from '../../../../shared/components/error-state/error-state.component';
import { LoadingStateComponent } from '../../../../shared/components/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { InventoryService } from '../../services/inventory.service';

@Component({
  selector: 'app-stock-adjustment',
  standalone: true,
  imports:[QuantityPipe,DatePipe, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule, ErrorStateComponent, LoadingStateComponent, PageHeaderComponent],
  template: `
    <app-page-header eyebrow="Estoque" title="Ajustar estoque" subtitle="Informe a quantidade real e registre o motivo."/>
    @if(loading()) { <app-loading-state/> }
    @else if(error()) { <app-error-state title="Não foi possível carregar o estoque" [message]="error()!"/> }
    @else {
      <section class="surface surface-body form-card">
        <form [formGroup]="form" class="form-grid" (ngSubmit)="save()">
          <mat-form-field appearance="outline" class="full"><mat-label>Produto</mat-label><mat-select formControlName="productId">@for(p of activeProducts();track p.id){<mat-option [value]="p.id">{{p.name}}{{p.perishable?' · perecível':''}}</mat-option>}</mat-select></mat-form-field>
          <div class="quantity-current"><small>Quantidade atual</small><strong>{{(current()?.stock ?? 0)|qty}} {{current()?.unit}}</strong></div>
          <mat-form-field appearance="outline"><mat-label>Nova quantidade</mat-label><input matInput type="number" min="0" step="0.001" formControlName="newQuantity"><span matTextSuffix>&nbsp;{{current()?.unit}}</span></mat-form-field>
          <div class="difference full" [class.negative]="difference()<0" [class.neutral]="!current()||difference()===0"><mat-icon>{{!current()||difference()===0?'drag_handle':difference()>0?'add_circle':'remove_circle'}}</mat-icon><div><small>Diferença calculada</small><strong>@if(current()){{{difference()>0?'+':''}}{{difference()|qty}} {{current()?.unit}}@if(difference()===0){ · sem alteração}}@else{Escolha um produto}</strong></div></div>
          @if(needsLot()){<div class="lot-box full"><p><mat-icon>event_available</mat-icon>Produto perecível: as unidades adicionadas precisam de lote. Sem lote, o saldo não pode ser vendido.</p>
            <mat-form-field appearance="outline"><mat-label>Lote das unidades adicionadas</mat-label><mat-select formControlName="lotId">@for(lot of validLots();track lot.id){<mat-option [value]="lot.id">{{lot.batch}} · validade {{lot.expiry|date:'dd/MM/yyyy'}}</mat-option>}<mat-option value="NEW">Novo lote</mat-option></mat-select></mat-form-field>
            @if(form.controls.lotId.value==='NEW'){<mat-form-field appearance="outline"><mat-label>Número do lote</mat-label><input matInput formControlName="batch"></mat-form-field><mat-form-field appearance="outline"><mat-label>Validade</mat-label><input matInput type="date" [min]="today" formControlName="expiry"></mat-form-field>}
            @if(lotError()){<p class="lot-error">{{lotError()}}</p>}</div>}
          <mat-form-field appearance="outline" class="full"><mat-label>Motivo do ajuste</mat-label><textarea matInput rows="3" formControlName="reason" placeholder="Ex.: contagem física do estoque"></textarea><mat-hint>Mínimo de 5 caracteres</mat-hint></mat-form-field>
          <div class="audit full"><span><small>Usuário</small><strong>{{auth.currentUser()?.name}}</strong></span><span><small>Data</small><strong>{{now()|date:'dd/MM/yyyy HH:mm'}}</strong></span></div>
          <div class="form-actions"><button mat-button type="button" (click)="router.navigate(['/app/estoque'])">Cancelar</button><button mat-flat-button class="primary-button" type="submit" [disabled]="saving()"><mat-icon>check</mat-icon>{{saving()?'Salvando…':'Confirmar ajuste'}}</button></div>
        </form>
      </section>
    }
  `,
  styles: [`.difference.neutral{background:#f5f5f4!important;color:var(--muted)!important;border-color:var(--border)!important}.lot-box{display:grid;grid-template-columns:1fr 1fr;gap:0 16px;padding:12px 14px 0;border:1px solid #f1d58a;border-radius:10px;background:var(--yellow-light)}.lot-box p{grid-column:1/-1;display:flex;gap:6px;margin:0 0 12px;font-size:.78rem;line-height:1.4}.lot-box mat-icon{font-size:18px;width:18px;height:18px}.lot-box .lot-error{color:var(--red-dark);font-weight:700}@media(max-width:700px){.lot-box{grid-template-columns:1fr}}.quantity-current{height:56px;border:1px solid var(--border);border-radius:8px;padding:9px 14px}.quantity-current small,.quantity-current strong{display:block}.quantity-current small{font-size:.68rem;color:var(--muted)}.quantity-current strong{margin-top:3px}.difference{display:flex;gap:10px;padding:13px;border-radius:9px;background:#eaf5eb;color:var(--success)}.difference.negative{background:#fff0ee;color:var(--red-dark)}.difference small,.difference strong{display:block}.difference small{font-size:.68rem}.audit{display:flex;gap:40px;padding:14px;background:#fafaf9;border-radius:9px}.audit small,.audit strong{display:block}.audit small{color:var(--muted);font-size:.68rem}.audit strong{font-size:.8rem;margin-top:3px}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StockAdjustmentComponent {
  readonly service = inject(InventoryService);
  readonly auth = inject(AuthService);
  readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);
  private readonly route = inject(ActivatedRoute);
  readonly saving = signal(false);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  /** Horário exibido no registro; atualizado a cada 30 s enquanto a tela está aberta. */
  readonly now = signal(new Date());
  private readonly clock = setInterval(() => this.now.set(new Date()), 30_000);
  private readonly stopClock = inject(DestroyRef).onDestroy(() => clearInterval(this.clock));
  readonly form = new FormGroup({
    productId: new FormControl(this.route.snapshot.queryParamMap.get('produto') ?? '', { nonNullable: true, validators: [Validators.required] }),
    newQuantity: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    reason: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(5)] }),
    lotId: new FormControl('', { nonNullable: true }),
    batch: new FormControl('', { nonNullable: true }),
    expiry: new FormControl('', { nonNullable: true })
  });
  readonly today = new Date().toLocaleDateString('en-CA');
  readonly lotError = signal('');

  constructor() {
    this.form.controls.productId.valueChanges.subscribe(() => this.syncSelectedProduct());
    void this.load();
  }

  current() { return this.service.products().find(product => product.id === this.form.controls.productId.value); }
  activeProducts() { return this.service.products().filter(product => product.status === 'ACTIVE' || product.id === this.form.controls.productId.value); }
  needsLot() { return !!this.current()?.perishable && this.difference() > 0; }
  validLots() { return this.service.perishables().filter(lot => lot.productId === this.form.controls.productId.value && lot.status === 'VALID'); }
  difference() { return Math.round((this.form.controls.newQuantity.value - (this.current()?.stock ?? 0)) * 1000) / 1000; }

  async load(): Promise<void> {
    try {
      await Promise.all([this.service.loadPositions(), this.service.loadPerishables()]);
      this.syncSelectedProduct();
    } catch (error) {
      this.error.set(error instanceof ApiError ? error.message : 'Tente novamente em alguns instantes.');
    } finally {
      this.loading.set(false);
    }
  }

  async save(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.lotError.set('');
    if (this.needsLot()) {
      if (!value.lotId) { this.lotError.set('Escolha o lote das unidades adicionadas.'); return; }
      if (value.lotId === 'NEW' && (!value.batch.trim() || !value.expiry)) { this.lotError.set('Informe o número e a validade do novo lote.'); return; }
    }
    this.saving.set(true);
    try {
      await this.service.adjust({
        productId: value.productId,
        newQuantity: value.newQuantity,
        reason: value.reason,
        ...(this.needsLot() ? (value.lotId === 'NEW' ? { batch: value.batch.trim(), expiry: value.expiry } : { lotId: value.lotId }) : {})
      });
      this.snack.open('Estoque ajustado com sucesso.', 'Fechar', { duration: 3500 });
      await this.router.navigate(['/app/estoque']);
    } catch (error) {
      this.snack.open(error instanceof ApiError ? error.message : 'Não foi possível ajustar o estoque.', 'Fechar', { duration: 4500 });
    } finally {
      this.saving.set(false);
    }
  }

  private syncSelectedProduct(): void {
    const product = this.current();
    if (product) this.form.controls.newQuantity.setValue(product.stock, { emitEvent: false });
  }
}
