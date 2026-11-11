import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Injector, inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthSession, AuthService, SESSION_KEY } from '../auth/auth.service';
import { toApiError } from './api-error';

function readStoredSession(): AuthSession | null {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as Partial<AuthSession> | null;
    if (!parsed?.accessToken || !parsed.user?.id) return null;
    return parsed as AuthSession;
  } catch {
    return null;
  }
}

export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  const injector = inject(Injector);
  const session = readStoredSession();

  const headers: Record<string, string> = {
    'X-Client': 'dafabi-web'
  };

  if (session?.accessToken) {
    headers['Authorization'] = `Bearer ${session.accessToken}`;
  }
  if (session?.user) {
    headers['X-User-Id'] = session.user.id;
    headers['X-User-Name'] = session.user.name;
    headers['X-User-Role'] = session.user.role;
  }

  const request = req.clone({ setHeaders: headers });

  return next(request).pipe(
    catchError(error => {
      const isAuthEndpoint = req.url.endsWith('/auth/login') || req.url.endsWith('/auth/logout');
      if (error instanceof HttpErrorResponse && error.status === 401 && !isAuthEndpoint) {
        injector.get(AuthService).logout(true);
      }
      return throwError(() => toApiError(error));
    })
  );
};
