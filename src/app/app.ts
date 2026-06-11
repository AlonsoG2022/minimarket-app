import { Component, computed, effect, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { CompanyService } from './core/services/company.service';
import { ThemeService } from './core/services/theme.service';

@Component({
  selector: 'app-root',
  imports: [FormsModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.shell.html',
  styleUrl: './app.shell.css'
})
export class App {
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);
  private readonly theme = inject(ThemeService);
  private readonly companyService = inject(CompanyService);
  productSearch = '';
  readonly storeName = 'Minimarket Casa';
  readonly expandedModules: Record<string, boolean> = {
    principal: false,
    ventas: false,
    inventario: false,
    mantenimiento: false,
    configuracion: false
  };
  readonly session = computed(() => this.auth.session());
  readonly canAccessDashboard = computed(() => this.auth.hasRole(['admin', 'cajero']));
  readonly canAccessSales = computed(() => this.auth.hasRole(['admin', 'cajero']));
  readonly canAccessCash = computed(() => this.auth.hasRole(['admin', 'cajero']));
  readonly canAccessPurchases = computed(() => this.auth.hasRole(['admin']));
  readonly canAccessProducts = computed(() => this.auth.hasRole(['admin']));
  readonly canAccessReports = computed(() => this.auth.hasRole(['admin']));
  readonly canAccessCategories = computed(() => this.auth.hasRole(['admin']));
  readonly canAccessSuppliers = computed(() => this.auth.hasRole(['admin']));
  readonly canAccessCompany = computed(() => this.auth.hasRole(['admin']));

  constructor() {
    // Aplica el tema cacheado de inmediato (evita parpadeo) y luego lo sincroniza con la configuracion.
    this.theme.applyStoredTheme();
    effect(() => {
      if (this.auth.isAuthenticated()) {
        this.companyService.get().subscribe({
          next: (company) => this.theme.applyTheme(company.theme),
          error: () => {}
        });
      }
    });
  }

  searchProducts(): void {
    if (!this.canAccessProducts()) {
      return;
    }

    const query = this.productSearch.trim();
    this.router.navigate(['/productos'], {
      queryParams: query ? { q: query } : {}
    });
  }

  openQuickSale(): void {
    if (!this.canAccessSales()) {
      return;
    }

    this.router.navigate(['/ventas']);
  }

  toggleModule(moduleKey: string): void {
    this.expandedModules[moduleKey] = !this.expandedModules[moduleKey];
  }

  isModuleExpanded(moduleKey: string, prefixes: string[]): boolean {
    return this.expandedModules[moduleKey];
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }
}
