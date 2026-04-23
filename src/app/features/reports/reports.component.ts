import { CommonModule, DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { SalesSummary, TopSellingProduct } from '../../core/models/minimarket.models';
import { ReportsService } from '../../core/services/reports.service';
import { SolesPricePipe } from '../../shared/pipes/soles-price.pipe';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, SolesPricePipe],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.css'
})
export class ReportsComponent implements OnInit {
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly reportsService = inject(ReportsService);

  startDate = this.formatDate(new Date(Date.now() - 6 * 24 * 60 * 60 * 1000));
  endDate = this.formatDate(new Date());
  rows: SalesSummary[] = [];
  topProducts: TopSellingProduct[] = [];
  total = 0;
  loading = true;
  error = '';

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    forkJoin({
      salesSummary: this.reportsService.getSalesSummary(this.startDate, this.endDate),
      topProducts: this.reportsService.getTopSellingProducts(this.startDate, this.endDate),
    }).subscribe({
      next: ({ salesSummary, topProducts }) => {
        this.rows = salesSummary;
        this.topProducts = topProducts;
        this.total = salesSummary.reduce((sum, row) => sum + row.totalAmount, 0);
        this.error = '';
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No fue posible generar el reporte.';
        this.topProducts = [];
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
