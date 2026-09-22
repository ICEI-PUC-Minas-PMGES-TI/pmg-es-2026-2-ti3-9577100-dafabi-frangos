import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const appRoutes: Routes = [
  { path: 'login', loadChildren: () => import('./features/auth/auth.routes').then(m => m.AUTH_ROUTES) },
  {
    path: 'app',
    canActivate: [authGuard],
    loadComponent: () => import('./core/layout/shell/shell.component').then(m => m.ShellComponent),
    children: [
      { path: 'dashboard', loadChildren: () => import('./features/dashboard/dashboard.routes').then(m => m.DASHBOARD_ROUTES) },
      { path: 'vendas', loadChildren: () => import('./features/sales/sales.routes').then(m => m.SALES_ROUTES) },
      { path: 'caixa', loadChildren: () => import('./features/cash/cash.routes').then(m => m.CASH_ROUTES) },
      { path: 'produtos', loadChildren: () => import('./features/products/products.routes').then(m => m.PRODUCTS_ROUTES) },
      { path: 'estoque', loadChildren: () => import('./features/inventory/inventory.routes').then(m => m.INVENTORY_ROUTES) },
      { path: 'compras', loadChildren: () => import('./features/purchases/purchases.routes').then(m => m.PURCHASES_ROUTES) },
      { path: 'fornecedores', loadChildren: () => import('./features/suppliers/suppliers.routes').then(m => m.SUPPLIERS_ROUTES) },
      { path: 'despesas', loadChildren: () => import('./features/expenses/expenses.routes').then(m => m.EXPENSES_ROUTES) },
      { path: 'integracoes', loadChildren: () => import('./features/integrations/integrations.routes').then(m => m.INTEGRATIONS_ROUTES) },
      { path: 'estornos', loadChildren: () => import('./features/refunds/refunds.routes').then(m => m.REFUNDS_ROUTES) },
      { path: 'configuracoes', loadChildren: () => import('./features/settings/settings.routes').then(m => m.SETTINGS_ROUTES) },
      { path: 'acesso-negado', loadComponent: () => import('./shared/components/error-state/error-state.component').then(m => m.ErrorStateComponent), data: { denied: true } },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '', redirectTo: 'app/dashboard', pathMatch: 'full' },
  { path: '**', redirectTo: 'app/dashboard' }
];
