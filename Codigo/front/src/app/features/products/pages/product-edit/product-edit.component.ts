import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiError } from '../../../../core/http/api-error';
import { Product } from '../../../../core/models/domain.models';
import { ErrorStateComponent } from '../../../../shared/components/error-state/error-state.component';
import { LoadingStateComponent } from '../../../../shared/components/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { ProductFormComponent } from '../../components/product-form/product-form.component';
import { ProductFormValue } from '../../models/product.models';
import { ProductsService } from '../../services/products.service';

@Component({
  selector: 'app-product-edit',
  standalone: true,
  imports: [ErrorStateComponent, LoadingStateComponent, PageHeaderComponent, ProductFormComponent],
  template: `<app-page-header eyebrow="Produtos / Editar" title="Editar produto" subtitle="Atualize os dados necessários e salve as alterações."/>@if(loading()){<app-loading-state/>}@else if(error()){<app-error-state title="Não foi possível carregar o produto" [message]="error()!"/>}@else if(product()){<app-product-form [product]="product()!" [categories]="service.categories()" [saving]="saving()" submitLabel="Salvar alterações" (saved)="save($event)" (cancel)="router.navigate(['/app/produtos'])"/>}`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductEditComponent {
  readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly service = inject(ProductsService);
  private readonly snack = inject(MatSnackBar);
  readonly product = signal<Product | null>(null);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly error = signal<string | null>(null);

  constructor() { void this.load(); }

  async load(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) { this.error.set('Identificador do produto inválido.'); this.loading.set(false); return; }
    try {
      const [product] = await Promise.all([
        this.service.findById(id),
        this.service.categories().length ? Promise.resolve() : this.service.loadCategories()
      ]);
      this.product.set(product);
    } catch (error) {
      this.error.set(error instanceof ApiError ? error.message : 'Tente novamente em alguns instantes.');
    } finally {
      this.loading.set(false);
    }
  }

  async save(value: ProductFormValue): Promise<void> {
    const product = this.product();
    if (!product) return;
    this.saving.set(true);
    try {
      const saved = await this.service.save(value, product.id);
      if (saved.status !== value.status) await this.service.updateStatus(product.id, value.status);
      this.snack.open('Produto editado com sucesso.', 'Fechar', { duration: 3500 });
      await this.router.navigate(['/app/produtos', product.id]);
    } catch (error) {
      this.snack.open(error instanceof ApiError ? error.message : 'Não foi possível editar o produto.', 'Fechar', { duration: 4500 });
    } finally {
      this.saving.set(false);
    }
  }
}
