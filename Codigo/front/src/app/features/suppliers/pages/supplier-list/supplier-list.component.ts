import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { StatusChipComponent } from '../../../../shared/components/status-chip/status-chip.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../../../shared/components/error-state/error-state.component';
import { LoadingStateComponent } from '../../../../shared/components/loading-state/loading-state.component';
import { SearchInputComponent } from '../../../../shared/components/search-input/search-input.component';
import { formatPhone } from '../../../../shared/utils/phone.utils';
import { SuppliersService } from '../../services/suppliers.service';

type StatusFilter = 'ACTIVE' | 'INACTIVE' | 'ALL';

@Component({
  selector: 'app-supplier-list', standalone: true,
  imports: [RouterLink, MatButtonModule, MatIconModule, PageHeaderComponent, StatusChipComponent, EmptyStateComponent, ErrorStateComponent, LoadingStateComponent, SearchInputComponent],
  template: `<app-page-header eyebrow="Fornecedores" title="Fornecedores" subtitle="Contatos dos parceiros que abastecem a loja." actionLabel="Novo fornecedor" actionLink="/app/fornecedores/novo"/>
    <div class="toolbar"><app-search-input label="Buscar fornecedor" placeholder="Nome, telefone ou e-mail" [value]="query()" (changed)="query.set($event)"/>
      <div class="chips" role="group" aria-label="Filtrar por situação">@for(f of filters;track f.value){<button type="button" [class.active]="status()===f.value" [attr.aria-pressed]="status()===f.value" (click)="status.set(f.value)">{{f.label}} ({{count(f.value)}})</button>}</div></div>
    <section class="surface">@if(service.loading()){<app-loading-state/>}@else if(service.error()){<app-error-state title="Não foi possível carregar os fornecedores" [message]="service.error()!"/>}@else if(!service.suppliers().length){<app-empty-state icon="local_shipping" title="Nenhum fornecedor cadastrado" message="Cadastre o primeiro fornecedor para registrar compras." actionLabel="Novo fornecedor" actionLink="/app/fornecedores/novo"/>}@else if(!visible().length){<app-empty-state icon="search_off" title="Nenhum fornecedor encontrado" message="Altere a busca ou o filtro de situação."/>}@else{
      <div class="table-wrap"><table class="data-table"><thead><tr><th>Fornecedor</th><th>Telefone</th><th>E-mail</th><th>Situação</th><th><span class="sr-only">Ações</span></th></tr></thead><tbody>@for(s of visible();track s.id){<tr>
        <td><div class="cell-title">{{s.name}}</div>@if(s.notes){<div class="cell-meta notes">{{s.notes}}</div>}</td>
        <td>@if(s.phone){<a class="contact" [href]="'tel:+55' + digits(s.phone)"><mat-icon aria-hidden="true">call</mat-icon>{{phone(s.phone)}}</a>}@else{<span class="cell-meta">—</span>}</td>
        <td>@if(s.email){<a class="contact" [href]="'mailto:' + s.email"><mat-icon aria-hidden="true">mail</mat-icon>{{s.email}}</a>}@else{<span class="cell-meta">—</span>}</td>
        <td><app-status-chip [status]="s.active?'ACTIVE':'INACTIVE'"/></td>
        <td class="actions"><a mat-button [routerLink]="['/app/fornecedores',s.id,'editar']" [attr.aria-label]="'Editar ' + s.name"><mat-icon>edit</mat-icon>Editar</a></td>
      </tr>}</tbody></table></div>}</section>`,
  styles: [`.toolbar{display:flex;flex-wrap:wrap;gap:12px;align-items:center}.chips{display:flex;gap:6px;flex-wrap:wrap}.chips button{border:1px solid var(--border);background:#fff;border-radius:999px;padding:7px 13px;font:inherit;font-size:.75rem;font-weight:700;cursor:pointer}.chips button.active{background:var(--red);border-color:var(--red);color:#fff}.contact{display:inline-flex;align-items:center;gap:6px;color:var(--text);text-decoration:none;font-size:.84rem}.contact:hover{color:var(--red-dark);text-decoration:underline}.contact mat-icon{font-size:17px;width:17px;height:17px;color:var(--muted)}.notes{max-width:360px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.actions{text-align:right;white-space:nowrap}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SupplierListComponent {
  readonly service = inject(SuppliersService);
  readonly query = signal('');
  readonly status = signal<StatusFilter>('ACTIVE');
  readonly filters: { value: StatusFilter; label: string }[] = [{ value: 'ACTIVE', label: 'Ativos' }, { value: 'INACTIVE', label: 'Inativos' }, { value: 'ALL', label: 'Todos' }];
  readonly phone = formatPhone;

  private readonly matching = computed(() => {
    const term = this.query().trim().toLocaleLowerCase('pt-BR');
    const termDigits = term.replace(/\D/g, '');
    return this.service.suppliers()
      .filter(s => !term || s.name.toLocaleLowerCase('pt-BR').includes(term) || (s.email ?? '').toLocaleLowerCase('pt-BR').includes(term) || (!!termDigits && this.digits(s.phone).includes(termDigits)))
      .sort((a, b) => a.name.localeCompare(b.name, 'pt-BR'));
  });
  readonly visible = computed(() => this.matching().filter(s => this.status() === 'ALL' || (this.status() === 'ACTIVE') === s.active));

  constructor() { void this.service.load(); }

  count(filter: StatusFilter): number {
    return filter === 'ALL' ? this.matching().length : this.matching().filter(s => (filter === 'ACTIVE') === s.active).length;
  }

  digits(value: string | null | undefined): string { return (value ?? '').replace(/\D/g, ''); }
}
