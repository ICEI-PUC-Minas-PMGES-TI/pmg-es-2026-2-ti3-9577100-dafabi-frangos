import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CurrencyPipe } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ApiError } from '../../../../core/http/api-error';
import { AuthService } from '../../../../core/auth/auth.service';
import { Product } from '../../../../core/models/domain.models';
import { ErrorStateComponent } from '../../../../shared/components/error-state/error-state.component';
import { LoadingStateComponent } from '../../../../shared/components/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { StatusChipComponent } from '../../../../shared/components/status-chip/status-chip.component';
import { ProductsService } from '../../services/products.service';

@Component({
  selector: 'app-product-details',
  standalone: true,
  imports: [RouterLink, CurrencyPipe, MatButtonModule, MatIconModule, ErrorStateComponent, LoadingStateComponent, PageHeaderComponent, StatusChipComponent],
  template: `<app-page-header eyebrow="Produtos / Detalhes" [title]="product()?.name??'Produto'" subtitle="Informações comerciais e de estoque do item."/>@if(loading()){<app-loading-state/>}@else if(error()){<app-error-state title="Produto não encontrado" [message]="error()!"/>}@else if(product()){<div class="detail-grid"><section class="surface surface-body detail-card"><div class="detail-head"><span class="product-hero">{{product()!.name.slice(0,2).toUpperCase()}}</span><div><h2>{{product()!.name}}</h2><app-status-chip [status]="product()!.status"/></div></div><dl><div><dt>Categoria</dt><dd>{{product()!.category}}</dd></div><div><dt>Unidade</dt><dd>{{product()!.unit}}</dd></div><div><dt>Preço de venda</dt><dd>{{product()!.price|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</dd></div><div><dt>Custo</dt><dd>{{product()!.cost|currency:'BRL':'symbol':'1.2-2':'pt-BR'}}</dd></div><div><dt>Estoque atual</dt><dd>{{product()!.stock}} {{product()!.unit}}</dd></div><div><dt>Código de barras</dt><dd>{{product()!.barcode??'Não informado'}}</dd></div><div><dt>Perecível</dt><dd>{{product()!.perishable?'Sim':'Não'}}</dd></div></dl><div class="actions"><a mat-stroked-button routerLink="/app/produtos">Voltar</a>@if(auth.isAdmin()){<a mat-flat-button class="primary-button" [routerLink]="['/app/produtos',product()!.id,'editar']"><mat-icon>edit</mat-icon>Editar produto</a>}</div></section><aside class="surface surface-body stock-panel"><mat-icon>inventory</mat-icon><span>Disponível para venda</span><strong>{{product()!.stock}}</strong><small>{{product()!.unit}} em estoque</small></aside></div>}`,
  styles: [`.detail-grid{display:grid;grid-template-columns:minmax(0,2fr) minmax(240px,.7fr);gap:20px;max-width:1000px}.detail-head{display:flex;gap:16px;align-items:center;margin-bottom:28px}.detail-head h2{margin:0 0 7px}.product-hero{width:64px;height:64px;border-radius:16px;background:var(--yellow-light);color:var(--red-dark);display:grid;place-items:center;font-weight:800;font-size:1.25rem}dl{display:grid;grid-template-columns:repeat(2,1fr);gap:18px;margin:0}dl div{padding-bottom:14px;border-bottom:1px solid var(--border)}dt{color:var(--muted);font-size:.76rem;margin-bottom:5px}dd{margin:0;font-weight:700}.actions{display:flex;justify-content:flex-end;gap:10px;margin-top:28px}.stock-panel{text-align:center;align-self:start;background:var(--yellow-light)}.stock-panel>mat-icon{color:var(--red);font-size:32px;width:32px;height:32px}.stock-panel span,.stock-panel small{display:block;color:#6e4c0d}.stock-panel strong{display:block;font-size:3.3rem;color:var(--red-dark);margin:10px 0 0}@media(max-width:760px){.detail-grid,dl{grid-template-columns:1fr}}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductDetailsComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(ProductsService);
  readonly auth = inject(AuthService);
  readonly product = signal<Product | null>(null);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor() { void this.load(); }

  async load(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) { this.error.set('Identificador do produto inválido.'); this.loading.set(false); return; }
    try {
      this.product.set(await this.service.findById(id));
    } catch (error) {
      this.error.set(error instanceof ApiError ? error.message : 'Tente novamente em alguns instantes.');
    } finally {
      this.loading.set(false);
    }
  }
}
