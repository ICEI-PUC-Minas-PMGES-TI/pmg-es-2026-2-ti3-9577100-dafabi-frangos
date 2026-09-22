import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { StatusChipComponent } from '../../../../shared/components/status-chip/status-chip.component';
import { CashBalanceCardComponent } from '../../components/cash-balance-card/cash-balance-card.component';
import { CashService } from '../../services/cash.service';

@Component({
  selector: 'app-cash-summary', standalone: true,
  imports: [CurrencyPipe, DatePipe, RouterLink, MatButtonModule, MatIconModule, EmptyStateComponent, PageHeaderComponent, StatusChipComponent, CashBalanceCardComponent],
  template: `<app-page-header eyebrow="Caixa" title="Resumo do caixa" subtitle="Valores e movimentações do atendimento atual."/>
    @if(cash.register();as register){
      <div class="cash-status surface"><div><app-status-chip [status]="register.status"/><strong>{{register.operator}}</strong><span>{{register.status==='OPEN'?'Aberto':'Fechado'}} em {{(register.closedAt||register.openedAt)|date:'dd/MM/yyyy, HH:mm'}}</span></div>@if(register.status==='OPEN'){<a mat-flat-button class="primary-button" routerLink="/app/caixa/fechar"><mat-icon>lock</mat-icon>Fechar caixa</a>}@else{<a mat-flat-button class="primary-button" routerLink="/app/caixa/abrir"><mat-icon>lock_open</mat-icon>Abrir novo caixa</a>}</div>
      <section class="balance-grid"><app-cash-balance-card label="Saldo inicial" [value]="register.initialBalance" icon="account_balance_wallet"/><app-cash-balance-card label="Vendas em dinheiro" [value]="cash.cashSales()" icon="payments"/><app-cash-balance-card label="Despesas em dinheiro" [value]="cash.cashExpenses()" icon="receipt_long"/><app-cash-balance-card label="Estornos em dinheiro" [value]="cash.cashRefunds()" icon="undo"/><app-cash-balance-card label="Saldo esperado" [value]="cash.expected()" icon="calculate" [highlight]="true"/></section>
      <section class="surface movements"><div class="surface-header"><div><h2>Movimentações recentes</h2><span class="cell-meta">Entradas e saídas em dinheiro registradas neste caixa</span></div></div>@if(!cash.movements().length){<app-empty-state icon="swap_vert" title="Nenhuma movimentação" message="As movimentações em dinheiro aparecerão aqui."/>}@else{<div class="table-wrap"><table class="data-table"><thead><tr><th>Horário</th><th>Descrição</th><th>Origem</th><th>Tipo</th><th>Valor</th></tr></thead><tbody>@for(m of cash.movements();track m.id){<tr><td>{{m.date|date:'HH:mm'}}</td><td class="cell-title">{{m.description}}</td><td>{{m.origin}}</td><td><span class="move-type" [class.out]="m.type==='OUT'"><mat-icon>{{m.type==='IN'?'south_west':'north_east'}}</mat-icon>{{m.type==='IN'?'Entrada':'Saída'}}</span></td><td class="money">{{m.amount|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</td></tr>}</tbody></table></div>}</section>
    }@else{<app-empty-state icon="point_of_sale" title="Nenhum caixa iniciado" message="Abra o caixa para começar o atendimento." actionLabel="Abrir caixa" actionLink="/app/caixa/abrir"/>}`,
  styles: [`.cash-status{display:flex;justify-content:space-between;align-items:center;padding:17px 20px;margin-bottom:18px}.cash-status>div{display:flex;align-items:center;gap:12px}.cash-status span{color:var(--muted);font-size:.76rem}.balance-grid{display:grid;grid-template-columns:repeat(5,1fr);gap:12px;margin-bottom:18px}.move-type{display:flex;align-items:center;gap:5px;color:var(--success);font-size:.75rem;font-weight:700}.move-type.out{color:var(--red)}.move-type mat-icon{font-size:17px;width:17px;height:17px}@media(max-width:1150px){.balance-grid{grid-template-columns:repeat(3,1fr)}}@media(max-width:700px){.cash-status,.cash-status>div{align-items:flex-start;flex-direction:column}.balance-grid{grid-template-columns:1fr 1fr}}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class CashSummaryComponent { readonly cash = inject(CashService); }
