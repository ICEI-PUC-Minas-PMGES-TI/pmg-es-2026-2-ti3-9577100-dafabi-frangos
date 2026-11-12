import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiError } from '../../../../core/http/api-error';
import { ConfirmationDialogComponent } from '../../../../shared/components/confirmation-dialog/confirmation-dialog.component';
import { ErrorStateComponent } from '../../../../shared/components/error-state/error-state.component';
import { LoadingStateComponent } from '../../../../shared/components/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { RefundSaleSummary } from '../../models/refund.models';
import { RefundSummaryComponent } from '../../components/refund-summary/refund-summary.component';
import { RefundsService } from '../../services/refunds.service';

@Component({
  selector: 'app-refund-create', standalone: true,
  imports: [CurrencyPipe, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule,
    PageHeaderComponent, RefundSummaryComponent, ErrorStateComponent, LoadingStateComponent],
  template: `<app-page-header eyebrow="Estornos" title="Registrar estorno" subtitle="Localize a venda, confira os dados e informe o motivo obrigatório."/>
  <section class="surface surface-body refund-form">
    <div class="sale-search"><mat-form-field appearance="outline"><mat-label>Número da venda</mat-label><mat-icon matPrefix>search</mat-icon><input matInput [formControl]="search" placeholder="#1048" (keyup.enter)="findSale()"></mat-form-field><button mat-flat-button class="primary-button" type="button" (click)="findSale()" [disabled]="loading()">{{loading()?'Buscando…':'Buscar venda'}}</button></div>
    @if(loading()){<app-loading-state label="Consultando venda e elegibilidade para estorno…"/>}
    @if(error()){<app-error-state title="Não foi possível consultar a venda" message="Verifique a conexão e tente novamente."/>}
    @if(notFound()){<div class="notice error"><mat-icon>error</mat-icon>Venda não encontrada.</div>}
    @if(sale()){
      <app-refund-summary [sale]="sale()!"/>
      @if(!sale()!.eligible){<div class="notice error"><mat-icon>block</mat-icon>{{sale()!.ineligibilityReason||'Esta venda não está disponível para estorno.'}}</div>}
      @if(sale()!.eligible){<form [formGroup]="form" (ngSubmit)="confirm()"><mat-form-field appearance="outline"><mat-label>Motivo do estorno</mat-label><textarea matInput rows="4" formControlName="reason" placeholder="Descreva o motivo com clareza"></textarea>@if(form.controls.reason.touched&&form.controls.reason.invalid){<mat-error>Informe um motivo com pelo menos 5 caracteres.</mat-error>}</mat-form-field><div class="notice warning"><mat-icon>warning</mat-icon>Será revertido {{sale()!.cashAmountToReverse|currency:'BRL':'symbol':'1.2-2':'pt-BR'}} no caixa e os itens retornarão ao estoque.</div><div class="form-actions"><button mat-button type="button" (click)="router.navigate(['/app/estornos'])">Cancelar</button><button mat-flat-button class="danger-button" type="submit" [disabled]="saving()"><mat-icon>undo</mat-icon>{{saving()?'Estornando…':'Confirmar estorno'}}</button></div></form>}
    }
  </section>`,
  styles: [`.refund-form{max-width:800px}.sale-search{display:grid;grid-template-columns:1fr auto;gap:10px}.sale-search button{height:56px}.refund-form form{display:grid;gap:10px;margin-top:20px}.refund-form .form-actions{display:flex}.notice{display:flex;align-items:center;gap:8px;padding:10px 12px;border-radius:8px;font-size:.8rem;margin:14px 0}.notice.error{background:#fff0ee;color:var(--red-dark)}.notice.warning{background:var(--yellow-light);color:#76520e}@media(max-width:600px){.sale-search{grid-template-columns:1fr}.sale-search button{margin-top:-16px}}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RefundCreateComponent {
  private readonly service = inject(RefundsService);
  readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  readonly search = new FormControl('', { nonNullable: true });
  readonly sale = signal<RefundSaleSummary | null>(null);
  readonly notFound = signal(false);
  readonly loading = signal(false);
  readonly error = signal(false);
  readonly saving = signal(false);
  readonly form = new FormGroup({ reason: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.minLength(5), Validators.maxLength(500)] }) });

  async findSale(): Promise<void> {
    this.sale.set(null);
    this.notFound.set(false);
    this.error.set(false);
    this.loading.set(true);
    try {
      const found = await this.service.findSale(this.search.value);
      this.sale.set(found);
      this.notFound.set(!found);
    } catch {
      this.error.set(true);
    } finally {
      this.loading.set(false);
    }
  }

  confirm(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    const sale = this.sale();
    if (!sale?.eligible) return;

    this.dialog.open(ConfirmationDialogComponent, { data: {
      title: `Estornar a venda ${sale.number}?`,
      message: 'Esta ação reverterá o valor em dinheiro e devolverá os itens ao estoque. O histórico será mantido.',
      confirmLabel: 'Confirmar estorno', destructive: true,
      confirmAction: async () => {
        this.saving.set(true);
        try {
          const created = await this.service.create({ saleId: sale.id, reason: this.form.controls.reason.value });
          await this.router.navigate(['/app/estornos', created.id]);
          this.snack.open('Estorno concluído com sucesso.', 'Fechar', { duration: 4000 });
        } catch (error) {
          if (error instanceof ApiError) throw error;
          throw new Error('Não foi possível concluir o estorno. Tente novamente.');
        } finally {
          this.saving.set(false);
        }
      }
    }});
  }
}
