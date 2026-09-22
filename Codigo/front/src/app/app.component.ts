import { ChangeDetectionStrategy, Component, OnDestroy, OnInit, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { SalesService } from './features/sales/services/sales.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: '<router-outlet />',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class AppComponent implements OnInit, OnDestroy {
  private readonly router = inject(Router);
  private readonly sales = inject(SalesService);
  private readonly lifecycle = new AbortController();

  ngOnInit(): void {
    const context = document.modelContext;
    if (!context?.registerTool) return;
    const options = { signal: this.lifecycle.signal };
    void Promise.resolve(context.registerTool({
      name: 'read_current_sale', title: 'Consultar venda atual',
      description: 'Retorna os itens e o total do carrinho atualmente aberto no PDV.',
      inputSchema: { type: 'object', properties: {}, additionalProperties: false },
      annotations: { readOnlyHint: true, untrustedContentHint: false },
      execute: () => ({ items: this.sales.cart(), total: this.sales.total(), itemCount: this.sales.itemCount() })
    }, options)).catch(() => undefined);
    void Promise.resolve(context.registerTool({
      name: 'add_product_to_current_sale', title: 'Adicionar produto à venda',
      description: 'Abre o PDV e adiciona uma unidade de um produto ativo ao carrinho atual.',
      inputSchema: { type: 'object', properties: { productId: { type: 'string' } }, required: ['productId'], additionalProperties: false },
      annotations: { readOnlyHint: false, untrustedContentHint: false },
      execute: async (input: unknown) => {
        const productId = typeof input === 'object' && input !== null ? (input as { productId?: unknown }).productId : undefined;
        if (typeof productId !== 'string' || !productId) throw new Error('productId é obrigatório');
        const product = this.sales.products().find(item => item.id === productId && item.status === 'ACTIVE');
        if (!product) throw new Error('Produto não encontrado');
        this.sales.addProduct(product);
        await this.router.navigate(['/app/vendas/nova']);
        return { productId, productName: product.name, itemCount: this.sales.itemCount(), total: this.sales.total() };
      }
    }, options)).catch(() => undefined);
  }

  ngOnDestroy(): void { this.lifecycle.abort(); }
}
