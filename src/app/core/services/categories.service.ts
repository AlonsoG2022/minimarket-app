import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay, tap } from 'rxjs';
import { API_BASE_URL } from './api-base';
import { Category, SaveCategory } from '../models/minimarket.models';

@Injectable({ providedIn: 'root' })
export class CategoriesService {
  private readonly http = inject(HttpClient);
  private categories$?: Observable<Category[]>;

  getAll(forceRefresh = false): Observable<Category[]> {
    if (!this.categories$ || forceRefresh) {
      this.categories$ = this.http
        .get<Category[]>(`${API_BASE_URL}/categories`)
        .pipe(shareReplay(1));
    }

    return this.categories$;
  }

  create(payload: SaveCategory): Observable<Category> {
    return this.http.post<Category>(`${API_BASE_URL}/categories`, payload).pipe(
      tap(() => this.invalidateCache())
    );
  }

  invalidateCache(): void {
    this.categories$ = undefined;
  }
}
