import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { API_BASE_URL } from './api-base';
import { PrintJob } from '../models/minimarket.models';

@Injectable({ providedIn: 'root' })
export class PrintJobsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE_URL}/print-jobs`;
  private recentJobs$?: Observable<PrintJob[]>;

  getRecent(forceRefresh = false): Observable<PrintJob[]> {
    if (!this.recentJobs$ || forceRefresh) {
      this.recentJobs$ = this.http
        .get<PrintJob[]>(`${this.apiUrl}/recent`)
        .pipe(shareReplay(1));
    }

    return this.recentJobs$;
  }

  requeue(jobId: number): Observable<PrintJob> {
    return this.http.post<PrintJob>(`${this.apiUrl}/${jobId}/requeue`, {});
  }

  invalidateCache(): void {
    this.recentJobs$ = undefined;
  }
}
