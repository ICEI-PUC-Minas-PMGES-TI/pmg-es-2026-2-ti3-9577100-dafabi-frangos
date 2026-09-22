import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiError } from '../../../../core/http/api-error';
import { ErrorStateComponent } from '../../../../shared/components/error-state/error-state.component';
import { LoadingStateComponent } from '../../../../shared/components/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { ProductFormComponent } from '../../components/product-form/product-form.component';
import { ProductFormValue } from '../../models/product.models';
import { ProductsService } from '../../services/products.service';

@Component({
  selector: 'app-product-create',
  standalone: true,
  imports: [ErrorStateComponent, LoadingStateComponent, PageHeaderComponent, ProductFormComponent],
  template: `<app-page-header eyebrow="Produtos / Novo" title="Cadastrar produto" subtitle="Preencha os dados comerciais e de estoque."/>@if(loading()){<app-loading-state/>}@else if(error()){<app-error-state title="Não foi possível carregar as categorias" [message]="error()!"/>}@else{<app-product-form [categories]="service.categories()" [saving]="saving()" (saved)="save($event)" (cancel)="router.navigate(['/app/produtos'])"/>}`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ProductCreateComponent {
  readonly router = inject(Router);
  readonly service = inject(ProductsService);
  private readonly snack = inject(MatSnackBar);
  readonly saving = signal(false);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  constructor() { void this.loadCategories(); }

  async loadCategories(): Promise<void> {
    try {
      if (!this.service.categories().length) await this.service.loadCategories();
    } catch (error) {
      this.error.set(error instanceof ApiError ? error.message : 'Tente novamente em alguns instantes.');
    } finally {
      this.loading.set(false);
    }
  }

  async save(value: ProductFormValue): Promise<void> {
    this.saving.set(true);
    try {
      await this.service.save(value);
      this.snack.open('Produto salvo com sucesso.', 'Fechar', { duration: 3500 });
      await this.router.navigate(['/app/produtos']);
    } catch (error) {
      this.snack.open(error instanceof ApiError ? error.message : 'Não foi possível salvar o produto.', 'Fechar', { duration: 4500 });
    } finally {
      this.saving.set(false);
    }
  }
}
