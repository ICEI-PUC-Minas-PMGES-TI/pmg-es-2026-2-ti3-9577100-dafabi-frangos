import { ChangeDetectionStrategy, Component, computed, effect, input, output } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Category, Product } from '../../../../core/models/domain.models';
import { ProductFormValue } from '../../models/product.models';

@Component({
  selector: 'app-product-form',
  standalone: true,
  imports: [ReactiveFormsModule, MatButtonModule, MatCheckboxModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule],
  template: `
    <form class="surface form-card surface-body form-grid" [formGroup]="form" (ngSubmit)="submit()">
      <mat-form-field appearance="outline" class="full"><mat-label>Nome do produto</mat-label><input matInput formControlName="name" placeholder="Ex.: Frango assado tradicional">@if(form.controls.name.touched&&form.controls.name.invalid){<mat-error>Informe um nome com pelo menos 3 caracteres.</mat-error>}</mat-form-field>
      <mat-form-field appearance="outline"><mat-label>Categoria</mat-label><mat-select formControlName="categoryId">@for(category of categories();track category.id){<mat-option [value]="category.id">{{category.name}}</mat-option>}</mat-select>@if(form.controls.categoryId.touched&&form.controls.categoryId.invalid){<mat-error>Selecione uma categoria.</mat-error>}</mat-form-field>
      <mat-form-field appearance="outline"><mat-label>Unidade de venda</mat-label><mat-select formControlName="unit">@for(unit of units;track unit.value){<mat-option [value]="unit.value">{{unit.label}}</mat-option>}</mat-select></mat-form-field>
      <mat-form-field appearance="outline"><mat-label>Preço de venda</mat-label><span matTextPrefix>R$&nbsp;</span><input matInput type="number" min="0.01" step="0.01" formControlName="price"></mat-form-field>
      <mat-form-field appearance="outline"><mat-label>Custo</mat-label><span matTextPrefix>R$&nbsp;</span><input matInput type="number" min="0" step="0.01" formControlName="cost"></mat-form-field>
      @if(!editing()){<mat-form-field appearance="outline"><mat-label>Quantidade inicial em estoque</mat-label><input matInput type="number" min="0" formControlName="stock"><span matTextSuffix>&nbsp;{{form.controls.unit.value}}</span></mat-form-field>}
      <mat-form-field appearance="outline"><mat-label>Código de barras (opcional)</mat-label><input matInput formControlName="barcode"><mat-icon matSuffix>barcode_scanner</mat-icon></mat-form-field>
      <div class="check-group full"><mat-checkbox formControlName="perishable">Produto perecível</mat-checkbox><mat-checkbox formControlName="frequent">Exibir nos produtos frequentes do PDV</mat-checkbox></div>
      <mat-form-field appearance="outline"><mat-label>Status</mat-label><mat-select formControlName="status"><mat-option value="ACTIVE">Ativo</mat-option><mat-option value="INACTIVE">Inativo</mat-option></mat-select></mat-form-field>
      @if(editing()){<div class="form-note"><mat-icon>inventory</mat-icon>O estoque é atualizado pelo módulo de ajustes para preservar o histórico das movimentações.</div>}
      <div class="form-note"><mat-icon>info</mat-icon>Produtos sem código de barras podem ser adicionados à venda pela pesquisa ou pelos botões visuais.</div>
      <div class="form-actions"><button mat-button type="button" (click)="cancel.emit()">Cancelar</button><button mat-flat-button class="primary-button" type="submit" [disabled]="saving()">@if(saving()){<span>Salvando…</span>} @else {<span>{{submitLabel()}}</span>} @if(!saving()){<mat-icon>save</mat-icon>}</button></div>
    </form>`,
  styles: [`.check-group{display:flex;gap:24px;padding:0 4px 18px;flex-wrap:wrap}`],
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
    frequent: new FormControl(false, { nonNullable: true })
  });

  constructor() {
    effect(() => {
      const product = this.product();
      if (!product) return;
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
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const value = this.form.getRawValue();
    this.saved.emit({ ...value, barcode: value.barcode || undefined });
  }
}
