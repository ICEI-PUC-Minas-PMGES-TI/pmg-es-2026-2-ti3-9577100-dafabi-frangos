import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiError } from '../../../../core/http/api-error';
import { AuthService } from '../../../../core/auth/auth.service';
import { ConfirmationDialogComponent } from '../../../../shared/components/confirmation-dialog/confirmation-dialog.component';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../../../shared/components/error-state/error-state.component';
import { LoadingStateComponent } from '../../../../shared/components/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { SearchInputComponent } from '../../../../shared/components/search-input/search-input.component';
import { StatusChipComponent } from '../../../../shared/components/status-chip/status-chip.component';
import { ProductsService } from '../../services/products.service';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CurrencyPipe, RouterLink, MatButtonModule, MatIconModule, MatMenuModule, EmptyStateComponent, ErrorStateComponent, LoadingStateComponent, PageHeaderComponent, SearchInputComponent, StatusChipComponent],
  template: `
    <app-page-header title="Produtos" subtitle="Cadastre preços, custos e informações de estoque em um só lugar." [actionLabel]="auth.isAdmin()?'Novo produto':''" (action)="router.navigate(['/app/produtos/novo'])" />
    <div class="toolbar"><app-search-input placeholder="Buscar por nome ou código de barras" [value]="query()" (changed)="search($event)"/><select class="filter-select" aria-label="Filtrar por categoria" [value]="categoryId()" (change)="changeCategory($any($event.target).value)"><option value="ALL">Todas as categorias</option>@for(category of service.categories();track category.id){<option [value]="category.id">{{category.name}}</option>}</select><select class="filter-select" aria-label="Filtrar por status" [value]="status()" (change)="changeStatus($any($event.target).value)"><option value="ALL">Todos os status</option><option value="ACTIVE">Ativos</option><option value="INACTIVE">Inativos</option></select></div>
    <section class="surface" aria-label="Lista de produtos"><div class="surface-header"><div><h2>Catálogo</h2><span class="count">{{service.pageState().totalElements}} produto(s) encontrado(s)</span></div></div>
      @if(loading()){<app-loading-state/>}@else if(error()){<app-error-state title="Não foi possível carregar os produtos" [message]="error()!"/>}@else if(!service.products().length){<app-empty-state title="Nenhum produto encontrado" message="Ajuste os filtros ou cadastre um novo produto." [actionLabel]="auth.isAdmin()?'Cadastrar produto':''" (action)="router.navigate(['/app/produtos/novo'])"/>}@else{<div class="table-wrap"><table class="data-table"><thead><tr><th>Produto</th><th>Preço / custo</th><th>Estoque</th><th>Código de barras</th><th>Status</th><th><span class="sr-only">Ações</span></th></tr></thead><tbody>@for(product of service.products();track product.id){<tr><td><div class="product-cell"><span class="product-badge">{{product.name.slice(0,2).toUpperCase()}}</span><div><div class="cell-title">{{product.name}}</div><div class="cell-meta">{{product.category}} · {{product.unit}}</div></div></div></td><td><div class="money">{{product.price|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</div><div class="cell-meta">Custo {{product.cost|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</div></td><td><strong [class.out]="product.stock===0">{{product.stock}} {{product.unit}}</strong>@if(product.stock===0){<div class="cell-meta out">Sem estoque</div>}</td><td><span class="barcode">{{product.barcode??'Sem código'}}</span></td><td><app-status-chip [status]="product.status"/></td><td><button mat-icon-button [matMenuTriggerFor]="actions" [attr.aria-label]="'Ações para '+product.name"><mat-icon>more_vert</mat-icon></button><mat-menu #actions="matMenu"><a mat-menu-item [routerLink]="['/app/produtos',product.id]"><mat-icon>visibility</mat-icon>Visualizar</a>@if(auth.isAdmin()){<a mat-menu-item [routerLink]="['/app/produtos',product.id,'editar']"><mat-icon>edit</mat-icon>Editar</a>@if(product.status==='ACTIVE'){<button mat-menu-item class="danger-text" (click)="inactivate(product.id,product.name)"><mat-icon>block</mat-icon>Inativar</button>}}</mat-menu></td></tr>}</tbody></table></div>}</section>`,
  styles: [`.count{display:block;color:var(--muted);font-size:.78rem;margin-top:4px}.product-cell{display:flex;gap:11px;align-items:center}.product-badge{width:38px;height:38px;border-radius:9px;background:var(--yellow-light);color:var(--red-dark);display:grid;place-items:center;font-size:.72rem;font-weight:800}.barcode{font-family:monospace;color:#57534e}.out,.danger-text{color:var(--red-dark)!important}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductListComponent {
  readonly service = inject(ProductsService);
  readonly auth = inject(AuthService);
  readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  readonly query = signal('');
  readonly categoryId = signal('ALL');
  readonly status = signal<'ACTIVE' | 'INACTIVE' | 'ALL'>('ALL');
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor() { void this.load(); }

  search(query: string): void { this.query.set(query); void this.load(); }
  changeCategory(categoryId: string): void { this.categoryId.set(categoryId); void this.load(); }
  changeStatus(status: 'ACTIVE' | 'INACTIVE' | 'ALL'): void { this.status.set(status); void this.load(); }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      await Promise.all([
        this.service.categories().length ? Promise.resolve() : this.service.loadCategories(),
        this.service.load({ query: this.query(), categoryId: this.categoryId() === 'ALL' ? undefined : this.categoryId(), status: this.status() })
      ]);
    } catch (error) {
      this.error.set(error instanceof ApiError ? error.message : 'Tente novamente em alguns instantes.');
    } finally {
      this.loading.set(false);
    }
  }

  inactivate(id: string, name: string): void {
    this.dialog.open(ConfirmationDialogComponent, { data: { title: 'Inativar produto?', message: `${name} deixará de aparecer para novas vendas. O histórico será mantido.`, confirmLabel: 'Inativar', destructive: true }, width: '420px' }).afterClosed().subscribe(async ok => {
      if (!ok) return;
      try {
        await this.service.inactivate(id);
        this.snack.open('Produto inativado com sucesso.', 'Fechar', { duration: 3500 });
        await this.load();
      } catch (error) {
        this.snack.open(error instanceof ApiError ? error.message : 'Não foi possível inativar o produto.', 'Fechar', { duration: 4000 });
      }
    });
  }
}
