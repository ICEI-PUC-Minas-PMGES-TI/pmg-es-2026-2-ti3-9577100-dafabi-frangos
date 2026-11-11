import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { StatusChipComponent } from '../../../../shared/components/status-chip/status-chip.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../../../shared/components/error-state/error-state.component';
import { LoadingStateComponent } from '../../../../shared/components/loading-state/loading-state.component';
import { Refund } from '../../../../core/models/domain.models';
import { RefundsService } from '../../services/refunds.service';

@Component({
  selector: 'app-refund-list', standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink, MatButtonModule, MatIconModule, PageHeaderComponent,
    StatusChipComponent, EmptyStateComponent, ErrorStateComponent, LoadingStateComponent],
  template: `<app-page-header eyebrow="Financeiro" title="Estornos" subtitle="Histórico auditável das vendas revertidas."/>
  <div class="toolbar"><span class="grow"></span><a mat-flat-button class="primary-button" routerLink="/app/estornos/novo"><mat-icon>undo</mat-icon>Novo estorno</a></div>
  @if(loading()){<section class="surface"><app-loading-state label="Carregando histórico de estornos…"/></section>}
  @else if(error()){<app-error-state title="Histórico indisponível" message="Não foi possível carregar os estornos."/><div class="retry"><button mat-stroked-button (click)="load()">Tentar novamente</button></div>}
  @else if(!refunds().length){<section class="surface"><app-empty-state icon="undo" title="Nenhum estorno registrado" message="Os estornos concluídos aparecerão aqui."/></section>}
  @else{<section class="surface"><div class="table-wrap"><table class="data-table"><thead><tr><th>Venda</th><th>Data</th><th>Usuário</th><th>Motivo</th><th>Valor</th><th>Status</th><th></th></tr></thead><tbody>@for(r of refunds();track r.id){<tr><td class="cell-title">{{r.saleNumber}}</td><td>{{r.date|date:'dd/MM/yyyy HH:mm'}}</td><td>{{r.user}}</td><td>{{r.reason}}</td><td class="money">{{r.amount|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</td><td><app-status-chip [status]="r.status"/></td><td><a mat-icon-button [routerLink]="['/app/estornos',r.id]" aria-label="Ver detalhes"><mat-icon>chevron_right</mat-icon></a></td></tr>}</tbody></table></div></section>}`,
  styles: [`.grow{flex:1}.retry{text-align:center;margin:12px}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RefundListComponent {
  private readonly service = inject(RefundsService);
  readonly refunds = signal<Refund[]>([]);
  readonly loading = signal(true);
  readonly error = signal(false);

  constructor() { void this.load(); }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(false);
    try { this.refunds.set(await this.service.list()); }
    catch { this.error.set(true); }
    finally { this.loading.set(false); }
  }
}
