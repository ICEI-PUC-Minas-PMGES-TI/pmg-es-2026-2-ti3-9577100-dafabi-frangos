import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../../../core/auth/auth.service';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { InventoryService } from '../../services/inventory.service';

@Component({
  selector: 'app-stock-adjustment', standalone: true,
  imports: [DatePipe, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule, MatSelectModule, PageHeaderComponent],
  template: `<app-page-header eyebrow="Estoque / Ajustes" title="Ajuste manual" subtitle="Corrija a quantidade de um produto e registre o motivo."/><section class="surface surface-body form-card"><form [formGroup]="form" class="form-grid" (ngSubmit)="save()"><mat-form-field appearance="outline" class="full"><mat-label>Produto</mat-label><mat-select formControlName="productId">@for(p of service.products();track p.id){<mat-option [value]="p.id">{{p.name}}</mat-option>}</mat-select></mat-form-field><div class="quantity-current"><small>Quantidade atual</small><strong>{{current()?.stock??0}} {{current()?.unit}}</strong></div><mat-form-field appearance="outline"><mat-label>Nova quantidade</mat-label><input matInput type="number" min="0" formControlName="newQuantity"><span matTextSuffix>&nbsp;{{current()?.unit}}</span></mat-form-field><div class="difference full" [class.negative]="difference()<0"><mat-icon>{{difference()>=0?'add_circle':'remove_circle'}}</mat-icon><div><small>Diferença calculada</small><strong>{{difference()>0?'+':''}}{{difference()}} {{current()?.unit}}</strong></div></div><mat-form-field appearance="outline" class="full"><mat-label>Motivo do ajuste</mat-label><textarea matInput rows="3" formControlName="reason" placeholder="Ex.: contagem física do estoque"></textarea><mat-hint>Mínimo de 5 caracteres</mat-hint></mat-form-field><div class="audit full"><span><small>Usuário</small><strong>{{auth.currentUser()?.name}}</strong></span><span><small>Data</small><strong>{{now|date:'dd/MM/yyyy HH:mm'}}</strong></span></div><div class="form-actions"><button mat-button type="button" (click)="router.navigate(['/app/estoque'])">Cancelar</button><button mat-flat-button class="primary-button" type="submit" [disabled]="saving()"><mat-icon>check</mat-icon>{{saving()?'Salvando…':'Confirmar ajuste'}}</button></div></form></section>`,
  styles:[`.quantity-current{height:56px;border:1px solid var(--border);border-radius:8px;padding:9px 14px}.quantity-current small,.quantity-current strong{display:block}.quantity-current small{font-size:.68rem;color:var(--muted)}.quantity-current strong{margin-top:3px}.difference{display:flex;gap:10px;padding:13px;border-radius:9px;background:#eaf5eb;color:var(--success)}.difference.negative{background:#fff0ee;color:var(--red-dark)}.difference small,.difference strong{display:block}.difference small{font-size:.68rem}.audit{display:flex;gap:40px;padding:14px;background:#fafaf9;border-radius:9px}.audit small,.audit strong{display:block}.audit small{color:var(--muted);font-size:.68rem}.audit strong{font-size:.8rem;margin-top:3px}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StockAdjustmentComponent {
  readonly service=inject(InventoryService); readonly auth=inject(AuthService); readonly router=inject(Router); private readonly snack=inject(MatSnackBar); private readonly route=inject(ActivatedRoute);
  readonly saving=signal(false); readonly now=new Date();
  readonly form=new FormGroup({productId:new FormControl(this.route.snapshot.queryParamMap.get('produto')??'',{nonNullable:true,validators:[Validators.required]}),newQuantity:new FormControl(0,{nonNullable:true,validators:[Validators.required,Validators.min(0)]}),reason:new FormControl('',{nonNullable:true,validators:[Validators.required,Validators.minLength(5)]})});
  constructor(){this.form.controls.productId.valueChanges.subscribe(id=>this.form.controls.newQuantity.setValue(this.service.products().find(p=>p.id===id)?.stock??0));const id=this.form.controls.productId.value;if(id)this.form.controls.newQuantity.setValue(this.service.products().find(p=>p.id===id)?.stock??0)}
  current(){return this.service.products().find(p=>p.id===this.form.controls.productId.value)} difference(){return this.form.controls.newQuantity.value-(this.current()?.stock??0)}
  async save(){if(this.form.invalid){this.form.markAllAsTouched();return}this.saving.set(true);await this.service.adjust(this.form.getRawValue());this.snack.open('Estoque ajustado com sucesso.','Fechar',{duration:3500});await this.router.navigate(['/app/estoque'])}
}
