import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../../../core/auth/auth.service';
import { ConfirmationDialogComponent } from '../../../../shared/components/confirmation-dialog/confirmation-dialog.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { CashService } from '../../services/cash.service';

@Component({ selector:'app-open-cash',standalone:true,
  imports:[DatePipe,ReactiveFormsModule,MatButtonModule,MatFormFieldModule,MatIconModule,MatInputModule,PageHeaderComponent],
  template:`<app-page-header eyebrow="Caixa" title="Abrir caixa" subtitle="Confirme o saldo inicial antes de começar as vendas."/><section class="surface surface-body cash-form"><div class="operator-block"><span class="avatar">{{initials}}</span><div><small>Operador responsável</small><strong>{{auth.currentUser()?.name}}</strong><span>{{now|date:'dd/MM/yyyy, HH:mm'}}</span></div></div>@if(cash.register()?.status==='OPEN'){<div class="notice warning"><mat-icon>info</mat-icon>Já existe um caixa aberto para este atendimento.</div>}<form [formGroup]="form" (ngSubmit)="open()"><mat-form-field appearance="outline"><mat-label>Saldo inicial</mat-label><span matTextPrefix>R$&nbsp;</span><input matInput type="number" step="0.01" min="0" formControlName="initialBalance">@if(form.controls.initialBalance.touched&&form.controls.initialBalance.invalid){<mat-error>Informe um saldo válido.</mat-error>}</mat-form-field><p>Conte o valor disponível em dinheiro e registre o total acima.</p><div class="form-actions"><button mat-button type="button" (click)="router.navigate(['/app/caixa/resumo'])">Voltar</button><button mat-flat-button class="primary-button" type="submit" [disabled]="saving()||cash.register()?.status==='OPEN'"><mat-icon>lock_open</mat-icon>{{saving()?'Abrindo…':'Abrir caixa'}}</button></div></form></section>`,
  styles:[`.cash-form{max-width:650px}.operator-block{display:flex;gap:12px;align-items:center;padding-bottom:20px;margin-bottom:22px;border-bottom:1px solid var(--border)}.operator-block small,.operator-block strong,.operator-block div>span{display:block}.operator-block small,.operator-block div>span{color:var(--muted);font-size:.72rem}.operator-block strong{margin:3px 0}.cash-form form p{color:var(--muted);font-size:.78rem;margin:-10px 0 20px}.form-actions{display:flex;justify-content:flex-end;gap:8px}`],changeDetection:ChangeDetectionStrategy.OnPush })
export class OpenCashComponent {
  readonly auth=inject(AuthService); readonly cash=inject(CashService); readonly router=inject(Router);
  private readonly snack=inject(MatSnackBar); private readonly dialog=inject(MatDialog);
  readonly now=new Date(); readonly saving=signal(false);
  readonly initials=this.auth.currentUser()?.name.split(' ').map(word=>word[0]).slice(0,2).join('')??'DF';
  readonly form=new FormGroup({initialBalance:new FormControl(150,{nonNullable:true,validators:[Validators.required,Validators.min(0)]})});
  open(){
    if(this.form.invalid){this.form.markAllAsTouched();return;}
    const balance=this.form.controls.initialBalance.value;
    this.dialog.open(ConfirmationDialogComponent,{data:{title:'Confirmar abertura do caixa?',message:`O caixa será aberto com saldo inicial de R$ ${balance.toFixed(2).replace('.',',')}.`,confirmLabel:'Abrir caixa'}}).afterClosed().subscribe(async ok=>{
      if(!ok)return;this.saving.set(true);try{await this.cash.open(this.form.getRawValue());this.snack.open('Caixa aberto com sucesso.','Fechar',{duration:3500});await this.router.navigate(['/app/caixa/resumo']);}finally{this.saving.set(false);}
    });
  }
}
