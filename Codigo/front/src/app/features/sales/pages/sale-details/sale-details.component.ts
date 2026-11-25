import { paymentLabel } from '../../../../shared/utils/labels';
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../../core/auth/auth.service';
import { ApiError } from '../../../../core/http/api-error';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../../../shared/components/error-state/error-state.component';
import { LoadingStateComponent } from '../../../../shared/components/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { StatusChipComponent } from '../../../../shared/components/status-chip/status-chip.component';
import { Refund, Sale } from '../../../../core/models/domain.models';
import { RefundsService } from '../../../refunds/services/refunds.service';
import { SalesService } from '../../services/sales.service';

@Component({
  selector: 'app-sale-details', standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink, MatButtonModule, MatIconModule, EmptyStateComponent, ErrorStateComponent, LoadingStateComponent, PageHeaderComponent, StatusChipComponent],
  template: `<app-page-header eyebrow="Vendas" [title]="sale()?.number??'Detalhes da venda'" [subtitle]="sale()?'Realizada em '+(sale()!.createdAt|date:'dd/MM/yyyy, HH:mm'):''"/>
    @if(loading()){<section class="surface"><app-loading-state label="Carregando venda…"/></section>}
    @else if(error()){<app-error-state title="Venda não encontrada" [message]="error()!"/>}
    @else if(sale();as current){<div class="sale-grid"><section class="surface"><div class="surface-header"><h2>Itens da venda</h2><app-status-chip [status]="current.status"/></div><div class="table-wrap"><table class="data-table"><thead><tr><th>Produto</th><th>Preço unitário</th><th>Quantidade</th><th>Subtotal</th></tr></thead><tbody>@for(item of current.items;track item.productId){<tr><td class="cell-title">{{item.productName}}</td><td>{{item.unitPrice|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</td><td>{{item.quantity}}</td><td class="money">{{item.subtotal|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</td></tr>}</tbody></table></div><div class="totals"><div><span>Valor bruto</span><b>{{current.gross|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</b></div><div><span>Descontos</span><b>- {{current.discount|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</b></div><div><span>Taxas</span><b>- {{current.fees|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</b></div><div class="total"><span>Valor líquido</span><strong>{{current.net|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</strong></div></div></section><aside class="stack"><section class="surface surface-body"><h2>Informações</h2><dl><div><dt>Origem</dt><dd>{{originLabel(current.origin)}}</dd></div><div><dt>Pagamento</dt><dd>{{paymentLabel(current.payment.method)}}</dd></div><div><dt>Operador</dt><dd>{{current.operator}}</dd></div>@if(current.payment.received!==undefined){<div><dt>Valor recebido</dt><dd>{{current.payment.received|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</dd></div>}@if(current.payment.change!==undefined){<div><dt>Troco</dt><dd>{{current.payment.change|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</dd></div>}</dl></section><section class="surface surface-body"><h2>Histórico</h2><ol class="timeline">@for(entry of current.history;track entry;let first=$first){<li><span [class.first]="first"></span><div><strong>{{entry}}</strong><small>{{current.createdAt|date:'dd/MM/yyyy HH:mm'}} · {{current.operator}}</small></div></li>}@if(refund();as r){<li><span class="refunded"></span><div><strong>Venda estornada</strong><small>{{r.date|date:'dd/MM/yyyy HH:mm'}} · {{r.user}}</small><small>Motivo: {{r.reason}}</small><a class="refund-detail" [routerLink]="['/app/estornos', r.id]">Ver estorno</a></div></li>}</ol></section>@if(auth.isAdmin()&&current.status==='COMPLETED'&&current.origin==='COUNTER'){<a mat-stroked-button class="refund-link" routerLink="/app/estornos/novo" [queryParams]="{venda: current.number.replace('#','')}"><mat-icon>undo</mat-icon>Estornar venda</a>}<a mat-button routerLink="/app/vendas/historico"><mat-icon>arrow_back</mat-icon>Voltar ao histórico</a></aside></div>}
    @else{<app-empty-state icon="receipt_long" title="Venda não encontrada" message="A venda solicitada não está disponível."/>}`,
  styles: [`.sale-grid{display:grid;grid-template-columns:minmax(0,1.5fr) minmax(280px,.65fr);gap:20px}.totals{margin-left:auto;padding:18px 22px;width:min(100%,360px)}.totals div{display:flex;justify-content:space-between;padding:5px 0;color:var(--muted);font-size:.82rem}.totals b{color:var(--text)}.totals .total{border-top:1px solid var(--border);margin-top:8px;padding-top:12px;color:var(--text)}.totals strong{font-size:1.2rem;color:var(--red-dark)}.surface h2{font-size:1rem;margin:0 0 16px}dl{margin:0}dl div{padding:9px 0;border-bottom:1px solid var(--border)}dt{color:var(--muted);font-size:.7rem}dd{margin:3px 0 0;font-weight:700;font-size:.84rem}.timeline{list-style:none;padding:0;margin:0}.timeline li{display:flex;gap:11px;position:relative;padding-bottom:17px}.timeline li>span{width:9px;height:9px;border-radius:50%;background:#d6d3d1;margin-top:4px;z-index:1}.timeline li>span.first{background:var(--success)}.timeline li>span.refunded{background:var(--red)}.refund-detail{display:inline-block;margin-top:4px;font-size:.72rem;font-weight:700;color:var(--red-dark)}.timeline li:not(:last-child)::after{content:'';position:absolute;left:4px;top:12px;width:1px;height:calc(100% - 5px);background:var(--border)}.timeline strong,.timeline small{display:block}.timeline strong{font-size:.78rem}.timeline small{font-size:.68rem;color:var(--muted);margin-top:3px}.refund-link{color:var(--red-dark);height:46px}.refund-link mat-icon{margin-right:6px}@media(max-width:900px){.sale-grid{grid-template-columns:1fr}}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SaleDetailsComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(SalesService);
  readonly auth = inject(AuthService);
  private readonly refunds = inject(RefundsService);
  readonly sale = signal<Sale | null>(null);
  readonly refund = signal<Refund | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor() { void this.load(); }

  async load(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) { this.error.set('Identificador da venda inválido.'); this.loading.set(false); return; }
    try {
      const sale = await this.service.findById(id);
      this.sale.set(sale);
      if (sale.status === 'REFUNDED' && this.auth.isAdmin()) this.refund.set(await this.refunds.findBySale(sale.id));
    }
    catch (error) { this.error.set(error instanceof ApiError ? error.message : 'Não foi possível carregar a venda.'); }
    finally { this.loading.set(false); }
  }

  originLabel(value: string) { return { COUNTER: 'Balcão', IFOOD: 'iFood', '99FOOD': '99Food' }[value] ?? value; }
  readonly paymentLabel = paymentLabel;
}
