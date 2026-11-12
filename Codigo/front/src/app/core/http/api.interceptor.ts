import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { Injector, inject } from '@angular/core';
import { catchError, from, switchMap, throwError } from 'rxjs';
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
      const isAuthEndpoint = ['/auth/login', '/auth/logout', '/auth/refresh']
        .some(endpoint => req.url.endsWith(endpoint));
      if (!(error instanceof HttpErrorResponse) || error.status !== 401 || isAuthEndpoint) {
        return throwError(() => toApiError(error));
      }

      const auth = injector.get(AuthService);
      return from(auth.refreshSession()).pipe(
        switchMap(refreshed => {
          const accessToken = auth.accessToken();
          if (!refreshed || !accessToken) {
            auth.logout(true);
            return throwError(() => toApiError(error));
          }

          return next(req.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } })).pipe(
            catchError(retryError => {
              if (retryError instanceof HttpErrorResponse && retryError.status === 401) {
                auth.logout(true);
              }
              return throwError(() => toApiError(retryError));
            })
          );
        })
      );
    })
  );
};
