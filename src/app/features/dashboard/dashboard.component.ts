import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { forkJoin } from 'rxjs';
import { DashboardSummary, Product } from '../../core/models/minimarket.models';
import { ProductsService } from '../../core/services/products.service';
import { ReportsService } from '../../core/services/reports.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly reportsService = inject(ReportsService);
  private readonly productsService = inject(ProductsService);

  summary?: DashboardSummary;
  lowStockProducts: Product[] = [];
  loading = true;
  error = '';

  ngOnInit(): void {
    forkJoin({
      summary: this.reportsService.getDashboard(),
      products: this.productsService.getAll()
    }).subscribe({
      next: ({ summary, products }) => {
        this.summary = summary;
        this.lowStockProducts = products.filter((product) => product.stock <= product.minimumStock);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudo cargar el dashboard. Verifica que la API este activa.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }
}
