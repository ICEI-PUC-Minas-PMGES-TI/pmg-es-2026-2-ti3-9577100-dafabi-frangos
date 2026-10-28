import { CurrencyPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormArray, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiError } from '../../../../core/http/api-error';
import { ErrorStateComponent } from '../../../../shared/components/error-state/error-state.component';
import { LoadingStateComponent } from '../../../../shared/components/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { PurchaseItemsComponent } from '../../components/purchase-items/purchase-items.component';
import { PurchaseDraft } from '../../models/purchase.models';
import { PurchasesService } from '../../services/purchases.service';

@Component({ selector: 'app-purchase-create', standalone: true, imports: [CurrencyPipe, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule, PageHeaderComponent, PurchaseItemsComponent, ErrorStateComponent, LoadingStateComponent], template: `<app-page-header eyebrow="Compras" title="Registrar compra" subtitle="A entrada atualizará o estoque dos produtos informados."/>@if(service.loadingData()){<app-loading-state label="Carregando fornecedores e produtos…"/>}@else if(service.error()){<app-error-state title="Não foi possível carregar os dados da compra" [message]="service.error()!"/>}@else{<form class="surface surface-body purchase-form" [formGroup]="form" (ngSubmit)="save()"><div class="form-grid"><mat-form-field appearance="outline"><mat-label>Fornecedor</mat-label><mat-select formControlName="supplierId">@for(s of service.suppliers();track s.id){<mat-option [value]="s.id">{{s.name}}</mat-option>}</mat-select></mat-form-field><mat-form-field appearance="outline"><mat-label>Data da compra</mat-label><input matInput type="date" formControlName="date"></mat-form-field></div><app-purchase-items [parent]="form" [products]="service.products()"/><div class="purchase-total"><span>Valor total</span><strong>{{total()|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</strong></div><div class="form-actions"><button mat-button type="button" (click)="router.navigate(['/app/compras'])">Cancelar</button><button mat-flat-button class="primary-button" type="submit" [disabled]="saving()"><mat-icon>check</mat-icon>{{saving()?'Registrando…':'Registrar compra'}}</button></div></form>}`, styles: [`.purchase-form{max-width:1000px}.purchase-total{display:flex;justify-content:flex-end;align-items:center;gap:25px;background:var(--yellow-light);padding:14px 18px;border-radius:10px;margin-top:18px}.purchase-total span{font-size:.8rem;font-weight:700}.purchase-total strong{font-size:1.3rem;color:var(--red-dark)}.form-actions{display:flex}`], changeDetection: ChangeDetectionStrategy.OnPush })
export class PurchaseCreateComponent {
  readonly service = inject(PurchasesService); readonly router = inject(Router); private readonly snack = inject(MatSnackBar); readonly saving = signal(false);
  readonly form = new FormGroup({ supplierId: new FormControl('', { nonNullable: true, validators: [Validators.required] }), date: new FormControl(new Date().toISOString().slice(0, 10), { nonNullable: true, validators: [Validators.required] }), items: new FormArray([this.itemGroup()]) });
  constructor() { void this.service.loadFormData(); }
  private itemGroup() { return new FormGroup({ productId: new FormControl('', { nonNullable: true, validators: [Validators.required] }), quantity: new FormControl(1, { nonNullable: true, validators: [Validators.required, Validators.min(1)] }), unitCost: new FormControl(0, { nonNullable: true, validators: [Validators.required, Validators.min(.01)] }) }); }
  total(): number { return this.form.controls.items.controls.reduce((sum, group) => { const value = group.getRawValue(); return sum + Number(value.quantity) * Number(value.unitCost); }, 0); }
  async save(): Promise<void> {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    try { await this.service.create(this.form.getRawValue() as PurchaseDraft); this.snack.open('Compra registrada e estoque atualizado.', 'Fechar', { duration: 4000 }); await this.router.navigate(['/app/compras']); }
    catch (error) { this.snack.open(error instanceof ApiError ? error.message : 'Não foi possível registrar a compra.', 'Fechar', { duration: 4500 }); }
    finally { this.saving.set(false); }
  }
}
