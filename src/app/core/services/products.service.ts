import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay, tap } from 'rxjs';
import { API_BASE_URL } from './api-base';
import { Product, SaveProduct } from '../models/minimarket.models';

@Injectable({ providedIn: 'root' })
export class ProductsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE_URL}/products`;
  private products$?: Observable<Product[]>;

  getAll(forceRefresh = false): Observable<Product[]> {
    if (!this.products$ || forceRefresh) {
      this.products$ = this.http.get<Product[]>(this.apiUrl).pipe(shareReplay(1));
    }

    return this.products$;
  }

  create(payload: SaveProduct): Observable<Product> {
    return this.http.post<Product>(this.apiUrl, payload).pipe(
      tap(() => this.invalidateCache())
    );
  }

  update(id: number, payload: SaveProduct): Observable<Product> {
    return this.http.put<Product>(`${this.apiUrl}/${id}`, payload).pipe(
      tap(() => this.invalidateCache())
    );
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`).pipe(
      tap(() => this.invalidateCache())
    );
  }

  invalidateCache(): void {
    this.products$ = undefined;
  }
}
