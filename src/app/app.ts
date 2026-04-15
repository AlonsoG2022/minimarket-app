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

  searchProducts(): void {
    const query = this.productSearch.trim();
    this.router.navigate(['/productos'], {
      queryParams: query ? { q: query } : {}
    });
  }
}
