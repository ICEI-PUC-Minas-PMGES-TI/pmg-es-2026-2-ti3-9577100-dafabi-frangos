import { ChangeDetectionStrategy, Component, computed, effect, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { CurrencyPipe } from '@angular/common';
import { Category, Product } from '../../../../core/models/domain.models';
import { ProductFormValue } from '../../models/product.models';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [CurrencyPipe, ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule],
  template: `
    <form class="surface form-card surface-body form-grid" [formGroup]="form" (ngSubmit)="submit()">
      <mat-form-field appearance="outline" class="full"><mat-label>Nome do produto</mat-label><input matInput formControlName="name" placeholder="Ex.: Frango assado tradicional">@if(form.controls.name.touched&&form.controls.name.invalid){<mat-error>Informe um nome com pelo menos 3 caracteres.</mat-error>}</mat-form-field>
      <mat-form-field appearance="outline"><mat-label>Categoria</mat-label><mat-select formControlName="categoryId">@for(category of categories();track category.id){<mat-option [value]="category.id">{{category.name}}</mat-option>}</mat-select>@if(form.controls.categoryId.touched&&form.controls.categoryId.invalid){<mat-error>Selecione uma categoria.</mat-error>}</mat-form-field>
      <mat-form-field appearance="outline"><mat-label>Unidade de venda</mat-label><mat-select formControlName="unit">@for(unit of units;track unit.value){<mat-option [value]="unit.value">{{unit.label}}</mat-option>}</mat-select></mat-form-field>
      <mat-form-field appearance="outline"><mat-label>Preço de venda</mat-label><span matTextPrefix>R$&nbsp;</span><input matInput type="number" min="0.01" step="0.01" formControlName="price"></mat-form-field>
      <mat-form-field appearance="outline"><mat-label>Custo</mat-label><span matTextPrefix>R$&nbsp;</span><input matInput type="number" min="0" step="0.01" formControlName="cost">@if(costLocked()){<mat-hint>Custo médio calculado pelas compras</mat-hint>}</mat-form-field>@if(negativeMargin()){<div class="form-warning full" role="alert"><mat-icon>warning</mat-icon>Preço de venda abaixo do custo: cada unidade vendida dá prejuízo de {{(form.controls.cost.value - form.controls.price.value)|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}.</div>}
      @if(!editing()){<mat-form-field appearance="outline"><mat-label>Quantidade inicial em estoque</mat-label><input matInput type="number" min="0" step="0.001" formControlName="stock"><span matTextSuffix>&nbsp;{{form.controls.unit.value}}</span></mat-form-field>}
      <mat-form-field appearance="outline"><mat-label>Código de barras (opcional)</mat-label><input matInput formControlName="barcode"><mat-icon matSuffix>barcode_scanner</mat-icon></mat-form-field>
      <div class="check-group full"><mat-checkbox formControlName="perishable">Produto perecível</mat-checkbox><mat-checkbox formControlName="frequent">Exibir nos produtos frequentes do PDV</mat-checkbox></div>
      @if(becomesPerishableWithStock()){<div class="form-warning full" role="alert"><mat-icon>event_busy</mat-icon>As {{product()!.stock}} unidades atuais ficarão sem lote e não poderão ser vendidas até serem vinculadas a um lote em Estoque &gt; Perecíveis (Vincular saldo existente).</div>}
      @if(needsInitialLot()){<div class="initial-lot full"><p><mat-icon>event_available</mat-icon>Produto perecível com estoque inicial: informe o lote e a validade dessas unidades. Sem lote, o saldo não pode ser vendido.</p>
        <mat-form-field appearance="outline"><mat-label>Lote do estoque inicial</mat-label><input matInput formControlName="initialBatch">@if(form.controls.initialBatch.touched&&!form.controls.initialBatch.value.trim()){<mat-error>Informe o lote.</mat-error>}</mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Validade</mat-label><input matInput type="date" [min]="today" formControlName="initialExpiry">@if(form.controls.initialExpiry.touched&&!form.controls.initialExpiry.value){<mat-error>Informe a validade.</mat-error>}</mat-form-field></div>}
      <mat-form-field appearance="outline"><mat-label>Status</mat-label><mat-select formControlName="status"><mat-option value="ACTIVE">Ativo</mat-option><mat-option value="INACTIVE">Inativo</mat-option></mat-select></mat-form-field>
      @if(editing()){<div class="form-note"><mat-icon>inventory</mat-icon>O estoque é atualizado pelo módulo de ajustes para preservar o histórico das movimentações.</div>}
      <div class="form-note"><mat-icon>info</mat-icon>Produtos sem código de barras podem ser adicionados à venda pela pesquisa ou pelos botões visuais.</div>
      <div class="form-actions"><button mat-button type="button" (click)="cancel.emit()">Cancelar</button><button mat-flat-button class="primary-button" type="submit" [disabled]="saving()">@if(saving()){<span>Salvando…</span>} @else {<span>{{submitLabel()}}</span>} @if(!saving()){<mat-icon>save</mat-icon>}</button></div>
    </form>`,
  styles: [`.form-warning{display:flex;gap:8px;align-items:flex-start;padding:10px 12px;margin:-6px 0 14px;border-radius:9px;background:#fff0ee;color:var(--red-dark);font-size:.78rem;font-weight:600;line-height:1.4}.form-warning mat-icon{font-size:18px;width:18px;height:18px;flex:none}.initial-lot{display:grid;grid-template-columns:1fr 1fr;gap:0 16px;padding:12px 14px 0;margin-bottom:12px;border:1px solid #f1d58a;border-radius:10px;background:var(--yellow-light)}.initial-lot p{grid-column:1/-1;display:flex;gap:6px;align-items:flex-start;margin:0 0 12px;font-size:.78rem;line-height:1.4}.initial-lot mat-icon{font-size:18px;width:18px;height:18px}@media(max-width:700px){.initial-lot{grid-template-columns:1fr}}.check-group{display:flex;gap:24px;padding:0 4px 18px;flex-wrap:wrap}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductFormComponent {
  readonly product = input<Product>();
  readonly categories = input<readonly Category[]>([]);
  readonly saving = input(false);
  readonly submitLabel = input('Salvar produto');
  readonly saved = output<ProductFormValue>();
  readonly cancel = output<void>();
  readonly editing = computed(() => !!this.product());
  readonly units = [{ value: 'un', label: 'Unidade' }, { value: 'pct', label: 'Pacote' }, { value: 'pote', label: 'Pote' }];
  readonly form = new FormGroup({
    name: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(3)] }),
    categoryId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    price: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(.01)] }),
    cost: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    stock: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(0)] }),
    unit: new FormControl('un', { nonNullable: true, validators: [Validators.required] }),
    barcode: new FormControl('', { nonNullable: true }),
    perishable: new FormControl(false, { nonNullable: true }),
    status: new FormControl<'ACTIVE' | 'INACTIVE'>('ACTIVE', { nonNullable: true }),
    frequent: new FormControl(false, { nonNullable: true }),
    initialBatch: new FormControl('', { nonNullable: true }),
    initialExpiry: new FormControl('', { nonNullable: true })
  });
  readonly today = new Date().toLocaleDateString('en-CA');
  negativeMargin(): boolean { return Number(this.form.controls.price.value) > 0 && Number(this.form.controls.price.value) < Number(this.form.controls.cost.value); }
  /** Com estoque, o custo é o custo médio das compras e fica somente leitura. */
  costLocked(): boolean { return this.editing() && (this.product()?.stock ?? 0) > 0; }
  becomesPerishableWithStock(): boolean { const p = this.product(); return !!p && !p.perishable && p.stock > 0 && this.form.controls.perishable.value; }
  needsInitialLot(): boolean { return !this.editing() && this.form.controls.perishable.value && Number(this.form.controls.stock.value) > 0; }

  constructor() {
    effect(() => {
      const product = this.product();
      if (!product) return;
      if (product.stock > 0) this.form.controls.cost.disable({ emitEvent: false }); else this.form.controls.cost.enable({ emitEvent: false });
      this.form.patchValue({
        name: product.name,
        categoryId: product.categoryId ?? '',
        price: product.price,
        cost: product.cost,
        stock: product.stock,
        unit: product.unit,
        barcode: product.barcode ?? '',
        perishable: product.perishable,
        status: product.status,
        frequent: product.frequent ?? false
      });
    });
  }

  submit(): void {
    const missingLot = this.needsInitialLot()
      && (!this.form.controls.initialBatch.value.trim() || !this.form.controls.initialExpiry.value);
    if (this.form.invalid || missingLot) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.saved.emit({ ...value, barcode: value.barcode || undefined });
  }
}
