import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, shareReplay, tap } from 'rxjs';
import { API_BASE_URL } from './api-base';
import { CashSession, CloseCashSession, CreateCashMovement, OpenCashSession } from '../models/minimarket.models';

@Injectable({ providedIn: 'root' })
export class CashSessionsService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = `${API_BASE_URL}/cash-sessions`;
  private currentSessionCache = new Map<number, Observable<CashSession | null>>();

  getCurrent(userId: number, forceRefresh = false): Observable<CashSession | null> {
    if (!this.currentSessionCache.has(userId) || forceRefresh) {
      this.currentSessionCache.set(
        userId,
        this.http.get<CashSession | null>(`${this.apiUrl}/current/${userId}`).pipe(shareReplay(1))
      );
    }

    return this.currentSessionCache.get(userId)!;
  }

  getRecent(userId: number): Observable<CashSession[]> {
    return this.http.get<CashSession[]>(`${this.apiUrl}/user/${userId}`);
  }

  open(payload: OpenCashSession): Observable<CashSession> {
    return this.http.post<CashSession>(this.apiUrl, payload).pipe(
      tap(() => this.invalidateCurrent(payload.userId))
    );
  }

  addMovement(sessionId: number, payload: CreateCashMovement): Observable<CashSession> {
    return this.http.post<CashSession>(`${this.apiUrl}/${sessionId}/movements`, payload).pipe(
      tap(() => this.invalidateCurrent(payload.userId))
    );
  }

  close(sessionId: number, payload: CloseCashSession): Observable<CashSession> {
    return this.http.post<CashSession>(`${this.apiUrl}/${sessionId}/close`, payload).pipe(
      tap(() => this.invalidateCurrent(payload.userId))
    );
  }

  invalidateCurrent(userId: number): void {
    this.currentSessionCache.delete(userId);
  }
}
