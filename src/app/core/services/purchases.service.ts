import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay, tap } from 'rxjs';
import { API_BASE_URL } from './api-base';
import { CreatePurchase, Purchase } from '../models/minimarket.models';

@Injectable({ providedIn: 'root' })
export class PurchasesService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE_URL}/purchases`;
  private purchases$?: Observable<Purchase[]>;

  getAll(forceRefresh = false): Observable<Purchase[]> {
    if (!this.purchases$ || forceRefresh) {
      this.purchases$ = this.http.get<Purchase[]>(this.apiUrl).pipe(shareReplay(1));
    }

    return this.purchases$;
  }

  create(payload: CreatePurchase): Observable<Purchase> {
    return this.http.post<Purchase>(this.apiUrl, payload).pipe(
      tap(() => this.invalidateCache())
    );
  }

  invalidateCache(): void {
    this.purchases$ = undefined;
  }
}
