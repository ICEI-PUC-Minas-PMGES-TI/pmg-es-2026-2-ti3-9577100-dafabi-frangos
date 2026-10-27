import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { StatusChipComponent } from '../../../../shared/components/status-chip/status-chip.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../../../shared/components/error-state/error-state.component';
import { LoadingStateComponent } from '../../../../shared/components/loading-state/loading-state.component';
import { SuppliersService } from '../../services/suppliers.service';

@Component({
  selector: 'app-supplier-list', standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule, PageHeaderComponent, StatusChipComponent, EmptyStateComponent, ErrorStateComponent, LoadingStateComponent],
  template: `<app-page-header eyebrow="Cadastros" title="Fornecedores" subtitle="Contatos dos parceiros que abastecem a loja."/><div class="toolbar"><span class="toolbar-spacer"></span><a mat-flat-button class="primary-button" routerLink="/app/fornecedores/novo"><mat-icon>add</mat-icon>Novo fornecedor</a></div><section class="supplier-grid">@if(service.loading()){<app-loading-state/>}@else if(service.error()){<app-error-state title="Não foi possível carregar os fornecedores" [message]="service.error()!"/>}@else if(!service.suppliers().length){<app-empty-state icon="local_shipping" title="Nenhum fornecedor cadastrado" message="Cadastre o primeiro fornecedor para registrar compras." actionLabel="Novo fornecedor" actionLink="/app/fornecedores/novo"/>}@else{@for(s of service.suppliers();track s.id){<article class="surface surface-body"><div class="supplier-head"><span>{{s.name.slice(0,2).toUpperCase()}}</span><app-status-chip [status]="s.active?'ACTIVE':'INACTIVE'"/></div><h2>{{s.name}}</h2><p><mat-icon>phone</mat-icon>{{s.phone}}</p><p><mat-icon>mail</mat-icon>{{s.email}}</p>@if(s.notes){<small>{{s.notes}}</small>}<a mat-stroked-button [routerLink]="['/app/fornecedores',s.id,'editar']">Editar cadastro</a></article>}}</section>`,
  styles: [`.toolbar-spacer{flex:1}.supplier-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:16px}.supplier-head{display:flex;justify-content:space-between}.supplier-head>span{width:42px;height:42px;border-radius:10px;background:var(--yellow-light);color:var(--red-dark);display:grid;place-items:center;font-weight:800}.supplier-grid h2{font-size:1rem;margin:14px 0}.supplier-grid p{display:flex;gap:8px;align-items:center;color:var(--muted);font-size:.77rem}.supplier-grid p mat-icon{font-size:17px;width:17px;height:17px}.supplier-grid small{display:block;background:#fafaf9;padding:10px;border-radius:8px;color:var(--muted);font-size:.7rem;min-height:48px}.supplier-grid a{margin-top:18px;width:100%}@media(max-width:1000px){.supplier-grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:650px){.supplier-grid{grid-template-columns:1fr}}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SupplierListComponent {
  readonly service = inject(SuppliersService);
  constructor() { void this.service.load(); }
}
