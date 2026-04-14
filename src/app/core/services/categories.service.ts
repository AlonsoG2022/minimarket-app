import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { API_BASE_URL } from './api-base';
import { Category } from '../models/minimarket.models';

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
}
