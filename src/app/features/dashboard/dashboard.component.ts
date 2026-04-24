import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { forkJoin } from 'rxjs';
import { DashboardSummary, Product, Sale } from '../../core/models/minimarket.models';
import { ProductsService } from '../../core/services/products.service';
import { ReportsService } from '../../core/services/reports.service';
import { SalesService } from '../../core/services/sales.service';
import { SolesPricePipe } from '../../shared/pipes/soles-price.pipe';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, SolesPricePipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly reportsService = inject(ReportsService);
  private readonly productsService = inject(ProductsService);
  private readonly salesService = inject(SalesService);

  summary?: DashboardSummary;
  lowStockProducts: Product[] = [];
  recentSales: Sale[] = [];
  loading = true;
  error = '';

  get lowStockStatus(): 'critical' | 'warning' | 'ok' {
    if (!this.lowStockProducts.length) {
      return 'ok';
    }

    return this.lowStockProducts.some((product) => product.stock === 0 || product.stock < product.minimumStock)
      ? 'critical'
      : 'warning';
  }

  ngOnInit(): void {
    forkJoin({
      summary: this.reportsService.getDashboard(),
      products: this.productsService.getAll(),
      sales: this.salesService.getAll()
    }).subscribe({
      next: ({ summary, products, sales }) => {
        this.summary = summary;
        this.lowStockProducts = products.filter((product) => product.stock <= product.minimumStock);
        this.recentSales = sales.slice(0, 5);
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
