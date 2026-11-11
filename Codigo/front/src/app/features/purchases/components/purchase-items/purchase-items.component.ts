import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { Product } from '../../../../core/models/domain.models';

@Component({
  selector: 'app-purchase-items', standalone: true,
  imports: [CurrencyPipe, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule],
  template: `<div class="items-head"><div><h3>Itens da compra</h3><p>Para produtos perecíveis, informe o lote e a validade recebidos.</p></div><button mat-stroked-button type="button" (click)="add()"><mat-icon>add</mat-icon>Adicionar item</button></div>
    <div [formGroup]="parent()"><div formArrayName="items" class="items-list">@for(group of items.controls;track $index;let i=$index){<div class="item-card" [formGroupName]="i">
      <div class="item-row"><mat-form-field appearance="outline"><mat-label>Produto</mat-label><mat-select formControlName="productId">@for(p of products();track p.id){<mat-option [value]="p.id">{{p.name}}{{p.perishable?' · perecível':''}}</mat-option>}</mat-select></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Quantidade</mat-label><input matInput type="number" min="1" formControlName="quantity"></mat-form-field>
        <mat-form-field appearance="outline"><mat-label>Custo unitário</mat-label><span matTextPrefix>R$&nbsp;</span><input matInput type="number" min="0.01" step="0.01" formControlName="unitCost"></mat-form-field>
        <strong>{{lineTotal(i)|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</strong><button mat-icon-button type="button" (click)="remove(i)" [disabled]="items.length===1" aria-label="Remover item"><mat-icon>delete</mat-icon></button></div>
      @if(isPerishable(i)){<div class="lot-row"><mat-form-field appearance="outline"><mat-label>Lote recebido</mat-label><input matInput formControlName="batch" maxlength="100" aria-label="Lote recebido para produto perecível"></mat-form-field><mat-form-field appearance="outline"><mat-label>Validade</mat-label><input matInput type="date" formControlName="expiry" aria-label="Validade do lote"></mat-form-field></div>}
    </div>}</div></div>`,
  styles: [`.items-head{display:flex;justify-content:space-between;align-items:center;margin:20px 0 12px;gap:12px}.items-head h3{margin:0;font-size:.9rem}.items-head p{margin:4px 0 0;color:var(--muted);font-size:.74rem}.items-list{display:grid;gap:8px}.item-card{padding:12px;background:#fafaf9;border:1px solid var(--border);border-radius:10px}.item-row{display:grid;grid-template-columns:2fr .8fr 1fr .75fr auto;gap:10px;align-items:center}.item-row mat-form-field,.lot-row mat-form-field{margin-bottom:-20px}.item-row>strong{text-align:right;font-size:.82rem}.lot-row{display:grid;grid-template-columns:2fr 1fr;gap:10px;margin:8px 0 0}@media(max-width:800px){.item-row{grid-template-columns:1fr 1fr}.item-row mat-form-field:first-child{grid-column:1/-1}.lot-row{grid-template-columns:1fr 1fr}}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PurchaseItemsComponent {
  readonly parent = input.required<FormGroup>();
  readonly products = input.required<Product[]>();
  get items(): FormArray<FormGroup> { return this.parent().get('items') as FormArray<FormGroup>; }

  add(): void {
    this.items.push(new FormGroup({
      productId: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
      quantity: new FormControl(1, { nonNullable: true, validators: [Validators.required, Validators.min(1)] }),
      unitCost: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(.01)] }),
      batch: new FormControl('', { nonNullable: true }),
      expiry: new FormControl('', { nonNullable: true })
    }));
  }

  remove(index: number): void { this.items.removeAt(index); }

  isPerishable(index: number): boolean {
    const id = this.items.at(index).get('productId')?.value;
    return this.products().some(product => product.id === id && product.perishable);
  }

  lineTotal(index: number): number {
    const value = this.items.at(index).getRawValue();
    return Number(value.quantity) * Number(value.unitCost);
  }
}
