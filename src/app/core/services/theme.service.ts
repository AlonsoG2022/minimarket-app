import { Injectable } from '@angular/core';

export type AppTheme = 'orange' | 'dark' | 'light' | 'el11';

const STORAGE_KEY = 'minimarket.theme';
const DEFAULT_THEME: AppTheme = 'orange';
const AVAILABLE: AppTheme[] = ['orange', 'dark', 'light', 'el11'];

@Injectable({ providedIn: 'root' })
export class ThemeService {
  readonly themes: ReadonlyArray<AppTheme> = AVAILABLE;

  /** Aplica el tema cacheado de inmediato para evitar parpadeo al iniciar la app. */
  applyStoredTheme(): void {
    this.applyTheme(this.getStoredTheme());
  }

  getStoredTheme(): AppTheme {
    if (typeof localStorage === 'undefined') {
      return DEFAULT_THEME;
    }
    return this.normalize(localStorage.getItem(STORAGE_KEY));
  }

  /** Aplica el tema al documento y lo cachea localmente. */
  applyTheme(theme: string | null | undefined): void {
    const safeTheme = this.normalize(theme);

    if (typeof document !== 'undefined') {
      document.documentElement.setAttribute('data-theme', safeTheme);
    }

    if (typeof localStorage !== 'undefined') {
      localStorage.setItem(STORAGE_KEY, safeTheme);
    }
  }

  private normalize(theme: string | null | undefined): AppTheme {
    const value = (theme ?? '').trim().toLowerCase() as AppTheme;
    return AVAILABLE.includes(value) ? value : DEFAULT_THEME;
  }
}
