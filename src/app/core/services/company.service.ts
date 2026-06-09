import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay, tap } from 'rxjs';
import { API_BASE_URL } from './api-base';
import { Company, SaveCompany } from '../models/minimarket.models';

@Injectable({ providedIn: 'root' })
export class CompanyService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE_URL}/company`;
  private company$?: Observable<Company>;

  get(forceRefresh = false): Observable<Company> {
    if (!this.company$ || forceRefresh) {
      this.company$ = this.http.get<Company>(this.apiUrl).pipe(shareReplay(1));
    }
    return this.company$;
  }

  update(payload: SaveCompany): Observable<Company> {
    return this.http.put<Company>(this.apiUrl, payload).pipe(
      tap(() => this.invalidateCache())
    );
  }

  invalidateCache(): void {
    this.company$ = undefined;
  }
}
