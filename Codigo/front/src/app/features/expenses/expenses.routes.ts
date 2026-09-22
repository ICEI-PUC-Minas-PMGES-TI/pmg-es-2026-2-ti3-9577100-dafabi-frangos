import { Routes } from '@angular/router';
import { roleGuard } from '../../core/auth/role.guard';

export const EXPENSES_ROUTES: Routes = [
  { path: '', canActivate: [roleGuard], data: { roles: ['ADMIN'] },
    loadComponent: () => import('./pages/expense-list/expense-list.component').then(m => m.ExpenseListComponent) },
  { path: 'nova', canActivate: [roleGuard], data: { roles: ['ADMIN'] },
    loadComponent: () => import('./pages/expense-create/expense-create.component').then(m => m.ExpenseCreateComponent) },
  { path: ':id/editar', canActivate: [roleGuard], data: { roles: ['ADMIN'] },
    loadComponent: () => import('./pages/expense-edit/expense-edit.component').then(m => m.ExpenseEditComponent) }
];
