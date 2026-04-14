import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay } from 'rxjs';
import { API_BASE_URL } from './api-base';
import { User } from '../models/minimarket.models';

@Injectable({ providedIn: 'root' })
export class UsersService {
  private readonly http = inject(HttpClient);
  private users$?: Observable<User[]>;

  getAll(forceRefresh = false): Observable<User[]> {
    if (!this.users$ || forceRefresh) {
      this.users$ = this.http.get<User[]>(`${API_BASE_URL}/users`).pipe(shareReplay(1));
    }

    return this.users$;
  }
}
