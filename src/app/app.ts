import { Component, computed, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';

@Component({
  selector: 'app-root',
  imports: [FormsModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.shell.html',
  styleUrl: './app.shell.css'
})
export class App {
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);
  productSearch = '';
  readonly storeName = 'Minimarket Casa';
  readonly expandedModules: Record<string, boolean> = {
    principal: false,
    ventas: false,
    inventario: false,
    mantenimiento: false
  };
  readonly session = computed(() => this.auth.session());
  readonly canAccessDashboard = computed(() => this.auth.hasRole(['admin', 'cajero']));
  readonly canAccessSales = computed(() => this.auth.hasRole(['admin', 'cajero']));
  readonly canAccessPurchases = computed(() => this.auth.hasRole(['admin']));
  readonly canAccessProducts = computed(() => this.auth.hasRole(['admin']));
  readonly canAccessReports = computed(() => this.auth.hasRole(['admin']));
  readonly canAccessCategories = computed(() => this.auth.hasRole(['admin']));
  readonly canAccessSuppliers = computed(() => this.auth.hasRole(['admin']));

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
