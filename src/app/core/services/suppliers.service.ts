import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay, tap } from 'rxjs';
import { Supplier, SaveSupplier } from '../models/minimarket.models';
import { API_BASE_URL } from './api-base';

@Injectable({ providedIn: 'root' })
export class SuppliersService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE_URL}/suppliers`;
  private suppliers$?: Observable<Supplier[]>;

  getAll(forceRefresh = false): Observable<Supplier[]> {
    if (!this.suppliers$ || forceRefresh) {
      this.suppliers$ = this.http.get<Supplier[]>(this.apiUrl).pipe(shareReplay(1));
    }

    return this.suppliers$;
  }

  create(payload: SaveSupplier): Observable<Supplier> {
    return this.http.post<Supplier>(this.apiUrl, payload).pipe(
      tap(() => this.invalidateCache())
    );
  }

  update(id: number, payload: SaveSupplier): Observable<Supplier> {
    return this.http.put<Supplier>(`${this.apiUrl}/${id}`, payload).pipe(
      tap(() => this.invalidateCache())
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      tap(() => this.invalidateCache())
    );
  }

  invalidateCache(): void {
    this.suppliers$ = undefined;
  }
}
