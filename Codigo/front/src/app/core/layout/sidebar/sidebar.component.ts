import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../auth/auth.service';
import { Role } from '../../models/domain.models';

interface NavItem { label: string; icon: string; route: string; roles: Role[]; exact?: boolean; }
@Component({
  selector: 'app-sidebar', standalone: true, imports: [RouterLink, RouterLinkActive, MatIconModule],
  template: `
    <aside class="sidebar" [class.collapsed]="collapsed()">
      <div class="brand" aria-label="DaFabi Frangos"><div class="brand-mark"><span>DF</span></div>@if (!collapsed()) { <div><strong>DaFabi</strong><small>Frangos</small></div> }</div>
      <nav aria-label="Menu principal">
        @for (item of visibleItems(); track item.route) {
          <a [routerLink]="item.route" routerLinkActive="active" [routerLinkActiveOptions]="{ exact: item.exact ?? false }" [attr.aria-label]="collapsed() ? item.label : null" (click)="navigate.emit()">
            <mat-icon aria-hidden="true">{{ item.icon }}</mat-icon>@if (!collapsed()) { <span>{{ item.label }}</span> }
          </a>
        }
      </nav>
    </aside>`,
  styles: [`:host{display:block;width:100%;height:100%;min-width:0}.sidebar nav{min-height:1px}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SidebarComponent {
  readonly collapsed = input(false); readonly navigate = output<void>();
  readonly items: NavItem[] = [
    { label: 'Dashboard', icon: 'space_dashboard', route: '/app/dashboard', roles: ['ADMIN'] },
    { label: 'Nova venda', icon: 'point_of_sale', route: '/app/vendas/nova', roles: ['ADMIN', 'CASHIER'] },
    { label: 'Histórico de vendas', icon: 'receipt_long', route: '/app/vendas/historico', roles: ['ADMIN', 'CASHIER'] },
    { label: 'Caixa', icon: 'account_balance_wallet', route: '/app/caixa/resumo', roles: ['ADMIN', 'CASHIER'] },
    { label: 'Produtos', icon: 'inventory_2', route: '/app/produtos', roles: ['ADMIN', 'CASHIER'] },
    { label: 'Estoque', icon: 'shelves', route: '/app/estoque', roles: ['ADMIN', 'CASHIER'] },
    { label: 'Compras', icon: 'shopping_bag', route: '/app/compras', roles: ['ADMIN'] },
    { label: 'Fornecedores', icon: 'local_shipping', route: '/app/fornecedores', roles: ['ADMIN'] },
    { label: 'Despesas', icon: 'payments', route: '/app/despesas', roles: ['ADMIN'] },
    { label: 'Integrações', icon: 'sync_alt', route: '/app/integracoes', roles: ['ADMIN'] },
    { label: 'Estornos', icon: 'undo', route: '/app/estornos', roles: ['ADMIN'] },
    { label: 'Configurações', icon: 'settings', route: '/app/configuracoes', roles: ['ADMIN'] }
  ];
  readonly visibleItems = computed(() => {
    const role = this.auth.currentUser()?.role;
    return role ? this.items.filter(item => item.roles.includes(role)) : [];
  });
  constructor(readonly auth: AuthService) {}
}
