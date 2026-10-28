import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { StatusChipComponent } from '../../../../shared/components/status-chip/status-chip.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../../../shared/components/error-state/error-state.component';
import { LoadingStateComponent } from '../../../../shared/components/loading-state/loading-state.component';
import { PurchasesService } from '../../services/purchases.service';

@Component({ selector: 'app-purchase-list', standalone: true, imports: [CurrencyPipe, DatePipe, RouterLink, MatButtonModule, MatIconModule, PageHeaderComponent, StatusChipComponent, EmptyStateComponent, ErrorStateComponent, LoadingStateComponent], template: `<app-page-header eyebrow="Estoque" title="Compras" subtitle="Entradas registradas e atualização automática de estoque."/><div class="toolbar"><span class="grow"></span><a mat-flat-button class="primary-button" routerLink="/app/compras/nova"><mat-icon>add</mat-icon>Nova compra</a></div><section class="surface">@if(service.loading()){<app-loading-state/>}@else if(service.error()){<app-error-state title="Não foi possível carregar as compras" [message]="service.error()!"/>}@else if(!service.purchases().length){<app-empty-state icon="shopping_cart" title="Nenhuma compra registrada" message="Registre uma compra para atualizar o estoque." actionLabel="Nova compra" actionLink="/app/compras/nova"/>}@else{<div class="table-wrap"><table class="data-table"><thead><tr><th>Compra</th><th>Fornecedor</th><th>Data</th><th>Itens</th><th>Valor total</th><th>Status</th></tr></thead><tbody>@for(c of service.purchases();track c.id){<tr><td class="cell-title">#{{c.id.slice(0,8)}}</td><td>{{c.supplierName}}</td><td>{{c.date|date:'dd/MM/yyyy'}}</td><td>{{c.items.length||'—'}}</td><td class="money">{{c.total|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</td><td><app-status-chip [status]="c.status"/></td></tr>}</tbody></table></div>}</section>`, styles: [`.grow{flex:1}`], changeDetection: ChangeDetectionStrategy.OnPush })
export class PurchaseListComponent { readonly service = inject(PurchasesService); constructor() { void this.service.load(); } }
