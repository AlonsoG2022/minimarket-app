import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay, tap } from 'rxjs';
import { API_BASE_URL } from './api-base';
import { CreateSale, Sale } from '../models/minimarket.models';

@Injectable({ providedIn: 'root' })
export class SalesService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE_URL}/sales`;
  private sales$?: Observable<Sale[]>;

  getAll(forceRefresh = false): Observable<Sale[]> {
    if (!this.sales$ || forceRefresh) {
      this.sales$ = this.http.get<Sale[]>(this.apiUrl).pipe(shareReplay(1));
    }

    return this.sales$;
  }

  create(payload: CreateSale): Observable<Sale> {
    return this.http.post<Sale>(this.apiUrl, payload).pipe(
      tap(() => this.invalidateCache())
    );
  }

  invalidateCache(): void {
    this.sales$ = undefined;
  }
}
