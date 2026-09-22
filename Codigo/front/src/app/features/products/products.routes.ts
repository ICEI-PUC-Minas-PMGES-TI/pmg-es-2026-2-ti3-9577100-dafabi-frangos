import { Routes } from '@angular/router';
import { roleGuard } from '../../core/auth/role.guard';
export const PRODUCTS_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./pages/product-list/product-list.component').then(m => m.ProductListComponent) },
  { path: 'novo', canActivate: [roleGuard], data: { roles: ['ADMIN'] }, loadComponent: () => import('./pages/product-create/product-create.component').then(m => m.ProductCreateComponent) },
  { path: ':id/editar', canActivate: [roleGuard], data: { roles: ['ADMIN'] }, loadComponent: () => import('./pages/product-edit/product-edit.component').then(m => m.ProductEditComponent) },
  { path: ':id', loadComponent: () => import('./pages/product-details/product-details.component').then(m => m.ProductDetailsComponent) }
];
