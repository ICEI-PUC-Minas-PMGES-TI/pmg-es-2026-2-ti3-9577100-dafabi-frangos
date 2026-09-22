import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Role } from '../models/domain.models';
import { AuthService } from './auth.service';

export const roleGuard: CanActivateFn = route => {
  const roles = (route.data?.['roles'] ?? []) as Role[];
  return inject(AuthService).hasRole(roles) ? true : inject(Router).createUrlTree(['/app/acesso-negado']);
};
