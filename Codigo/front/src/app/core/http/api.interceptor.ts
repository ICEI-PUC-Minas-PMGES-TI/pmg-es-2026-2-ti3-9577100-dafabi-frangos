import { HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { toApiError } from './api-error';

export const apiInterceptor: HttpInterceptorFn = (req, next) => {
  const request = req.clone({ setHeaders: { 'X-Client': 'dafabi-web' } });
  return next(request).pipe(catchError(error => throwError(() => toApiError(error))));
};
