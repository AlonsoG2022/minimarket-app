import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { API_BASE_URL } from './api-base';
import { DashboardSummary, SalesSummary } from '../models/minimarket.models';

@Injectable({ providedIn: 'root' })
export class ReportsService {
  private readonly http = inject(HttpClient);
  private dashboard$?: Observable<DashboardSummary>;

  getDashboard(forceRefresh = false): Observable<DashboardSummary> {
    if (!this.dashboard$ || forceRefresh) {
      this.dashboard$ = this.http
        .get<DashboardSummary>(`${API_BASE_URL}/reports/dashboard`)
        .pipe(shareReplay(1));
    }

    return this.dashboard$;
  }

  getSalesSummary(startDate: string, endDate: string): Observable<SalesSummary[]> {
    const params = new HttpParams()
      .set('startDate', startDate)
      .set('endDate', endDate);

    return this.http.get<SalesSummary[]>(`${API_BASE_URL}/reports/sales-summary`, { params });
  }

  invalidateDashboardCache(): void {
    this.dashboard$ = undefined;
  }
}
