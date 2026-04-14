import { CommonModule, DatePipe } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SalesSummary } from '../../core/models/minimarket.models';
import { ReportsService } from '../../core/services/reports.service';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.css'
})
export class ReportsComponent implements OnInit {
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly reportsService = inject(ReportsService);

  startDate = this.formatDate(new Date(Date.now() - 6 * 24 * 60 * 60 * 1000));
  endDate = this.formatDate(new Date());
  rows: SalesSummary[] = [];
  total = 0;
  loading = true;
  error = '';

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.reportsService.getSalesSummary(this.startDate, this.endDate).subscribe({
      next: (rows) => {
        this.rows = rows;
        this.total = rows.reduce((sum, row) => sum + row.totalAmount, 0);
        this.error = '';
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No fue posible generar el reporte.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }
}
