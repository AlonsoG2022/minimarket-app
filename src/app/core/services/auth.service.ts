import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from './api-base';
import { AuthSession, LoginRequest } from '../models/minimarket.models';

const SESSION_KEY = 'minimarket.auth.session';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly sessionState = signal<AuthSession | null>(this.readStoredSession());

  readonly session = computed(() => this.sessionState());
  readonly isAuthenticated = computed(() => this.sessionState() !== null);
  readonly role = computed(() => this.sessionState()?.role ?? '');

  login(payload: LoginRequest): Observable<AuthSession> {
    return this.http.post<AuthSession>(`${API_BASE_URL}/auth/login`, payload).pipe(
      tap((session) => this.persistSession(session))
    );
  }

  logout(): void {
    this.sessionState.set(null);
    sessionStorage.removeItem(SESSION_KEY);
  }

  hasRole(roles: string[]): boolean {
    const currentRole = this.role().trim().toLowerCase();
    return roles.some((role) => role.trim().toLowerCase() === currentRole);
  }

  private persistSession(session: AuthSession): void {
    this.sessionState.set(session);
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  }

  private readStoredSession(): AuthSession | null {
    if (typeof window === 'undefined') {
      return null;
    }

    const rawSession = sessionStorage.getItem(SESSION_KEY);
    if (!rawSession) {
      return null;
    }

    try {
      return JSON.parse(rawSession) as AuthSession;
    } catch {
      sessionStorage.removeItem(SESSION_KEY);
      return null;
    }
  }
}
