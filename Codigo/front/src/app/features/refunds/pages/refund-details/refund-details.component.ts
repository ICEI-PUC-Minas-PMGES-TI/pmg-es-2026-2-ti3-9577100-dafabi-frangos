import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Refund } from '../../../../core/models/domain.models';
import { ErrorStateComponent } from '../../../../shared/components/error-state/error-state.component';
import { LoadingStateComponent } from '../../../../shared/components/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { StatusChipComponent } from '../../../../shared/components/status-chip/status-chip.component';
import { RefundsService } from '../../services/refunds.service';

@Component({
  selector: 'app-refund-details', standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink, MatButtonModule, MatIconModule, PageHeaderComponent,
    StatusChipComponent, ErrorStateComponent, LoadingStateComponent],
  template: `<app-page-header eyebrow="Estornos / Detalhes" [title]="refund()?'Estorno da venda '+refund()!.saleNumber:'Detalhes do estorno'" subtitle="Registro permanente da reversão."/>
    @if(loading()){<section class="surface"><app-loading-state label="Carregando detalhes do estorno…"/></section>}
    @else if(error()){<app-error-state title="Estorno não encontrado" message="Não foi possível carregar este registro."/><div class="retry"><a mat-stroked-button routerLink="/app/estornos">Voltar ao histórico</a></div>}
    @else if(refund()){<section class="surface surface-body refund-detail"><div class="refund-ok"><mat-icon>check_circle</mat-icon><div><strong>Estorno concluído</strong><span>Caixa e estoque revertidos conforme a forma de pagamento</span></div><app-status-chip [status]="refund()!.status"/></div><dl><div><dt>Venda</dt><dd>{{refund()!.saleNumber}}</dd></div><div><dt>Data e hora</dt><dd>{{refund()!.date|date:'dd/MM/yyyy HH:mm'}}</dd></div><div><dt>Usuário responsável</dt><dd>{{refund()!.user}}</dd></div><div><dt>Valor estornado</dt><dd>{{refund()!.amount|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</dd></div><div class="full"><dt>Motivo</dt><dd>{{refund()!.reason}}</dd></div></dl><a mat-stroked-button routerLink="/app/estornos">Voltar ao histórico</a></section>}`,
  styles: [`.refund-detail{max-width:760px}.refund-ok{display:flex;align-items:center;gap:12px;background:#eaf5eb;padding:14px;border-radius:10px;margin-bottom:20px}.refund-ok>mat-icon{color:var(--success)}.refund-ok>div{flex:1}.refund-ok strong,.refund-ok span{display:block}.refund-ok span{font-size:.72rem;color:#4c7950;margin-top:3px}dl{display:grid;grid-template-columns:1fr 1fr;gap:14px;margin:0 0 22px}dl div{border-bottom:1px solid var(--border);padding:10px}.full{grid-column:1/-1}dt{color:var(--muted);font-size:.7rem}dd{margin:4px 0 0;font-weight:700;font-size:.84rem}.retry{text-align:center;margin-top:12px}@media(max-width:600px){dl{grid-template-columns:1fr}.full{grid-column:auto}}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RefundDetailsComponent {
  private readonly service = inject(RefundsService);
  private readonly route = inject(ActivatedRoute);
  readonly refund = signal<Refund | null>(null);
  readonly loading = signal(true);
  readonly error = signal(false);

  constructor() {
    void this.service.find(this.route.snapshot.paramMap.get('id')!).then(value => this.refund.set(value))
      .catch(() => this.error.set(true)).finally(() => this.loading.set(false));
  }
}
