import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../../../environments/environment';
import { DashboardView } from '../models/dashboard.models';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  load(startDate: string, endDate: string): Promise<DashboardView> {
    const params = new HttpParams().set('startDate', startDate).set('endDate', endDate);
    return firstValueFrom(this.http.get<DashboardView>(`${environment.apiBaseUrl}/dashboard`, { params }));
  }
}
