import { Routes } from '@angular/router';
import { roleGuard } from '../../core/auth/role.guard';
export const INVENTORY_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./pages/inventory-list/inventory-list.component').then(m => m.InventoryListComponent) },
  { path: 'ajustes', canActivate: [roleGuard], data: { roles: ['ADMIN'] }, loadComponent: () => import('./pages/stock-adjustment/stock-adjustment.component').then(m => m.StockAdjustmentComponent) },
  { path: 'pereciveis', canActivate: [roleGuard], data: { roles: ['ADMIN'] }, loadComponent: () => import('./pages/perishables/perishables.component').then(m => m.PerishablesComponent) }
];
