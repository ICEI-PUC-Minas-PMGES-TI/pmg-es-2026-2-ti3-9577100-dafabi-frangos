import { ApplicationConfig, LOCALE_ID } from '@angular/core';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding, withInMemoryScrolling } from '@angular/router';
import { appRoutes } from './app.routes';
import { DateAdapter, MAT_DATE_LOCALE, provideNativeDateAdapter } from '@angular/material/core';
import { BrDateAdapter } from './core/i18n/br-date-adapter';
import { apiInterceptor } from './core/http/api.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    { provide: LOCALE_ID, useValue: 'pt-BR' },
    provideNativeDateAdapter(),
    { provide: DateAdapter, useClass: BrDateAdapter },
    { provide: MAT_DATE_LOCALE, useValue: 'pt-BR' },
    provideAnimationsAsync(),
    provideHttpClient(withInterceptors([apiInterceptor])),
    provideRouter(appRoutes, withComponentInputBinding(), withInMemoryScrolling({ scrollPositionRestoration: 'top' }))
  ]
};
