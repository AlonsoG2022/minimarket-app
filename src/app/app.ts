import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [FormsModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.shell.html',
  styleUrl: './app.shell.css'
})
export class App {
  private readonly router = inject(Router);
  productSearch = '';
  readonly storeName = 'Minimarket Casa';
  readonly expandedModules: Record<string, boolean> = {
    principal: false,
    ventas: false,
    inventario: false,
    mantenimiento: false
  };

  searchProducts(): void {
    const query = this.productSearch.trim();
    this.router.navigate(['/productos'], {
      queryParams: query ? { q: query } : {}
    });
  }

  openQuickSale(): void {
    this.router.navigate(['/ventas']);
  }

  toggleModule(moduleKey: string): void {
    this.expandedModules[moduleKey] = !this.expandedModules[moduleKey];
  }

  isModuleExpanded(moduleKey: string, prefixes: string[]): boolean {
    return this.expandedModules[moduleKey];
  }
}
