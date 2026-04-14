import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { Product, Sale, User } from '../../core/models/minimarket.models';
import { ProductsService } from '../../core/services/products.service';
import { ReportsService } from '../../core/services/reports.service';
import { SalesService } from '../../core/services/sales.service';
import { UsersService } from '../../core/services/users.service';

@Component({
  selector: 'app-sales-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './sales-form.component.html',
  styleUrl: './sales-form.component.css'
})
export class SalesFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly salesService = inject(SalesService);
  private readonly productsService = inject(ProductsService);
  private readonly reportsService = inject(ReportsService);
  private readonly usersService = inject(UsersService);

  readonly form = this.fb.group({
    userId: [0, [Validators.required, Validators.min(1)]],
    paymentMethod: ['Efectivo', Validators.required],
    notes: [''],
    details: this.fb.array([this.createDetailGroup()])
  });

  products: Product[] = [];
  users: User[] = [];
  recentSales: Sale[] = [];
  lastSaleId?: number;
  loading = true;
  refreshingProducts = false;
  loadingSalesHistory = true;
  message = '';
  error = '';

  get details(): FormArray {
    return this.form.get('details') as FormArray;
  }

  ngOnInit(): void {
    this.loading = true;
    forkJoin({
      products: this.productsService.getAll(),
      users: this.usersService.getAll()
    }).subscribe({
      next: ({ products, users }) => {
        this.products = products.filter((product) => product.isActive);
        this.users = users.filter((user) => user.isActive);
        this.error = '';
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudo cargar la informacion para registrar ventas.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });

    this.loadRecentSales();
  }

  addItem(): void {
    this.details.push(this.createDetailGroup());
  }

  removeItem(index: number): void {
    if (this.details.length === 1) {
      return;
    }

    this.details.removeAt(index);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload = this.form.getRawValue();

    this.salesService.create({
      userId: Number(payload.userId),
      paymentMethod: payload.paymentMethod ?? 'Efectivo',
      notes: payload.notes ?? '',
      details: (payload.details ?? []).map((detail) => ({
        productId: Number(detail.productId),
        quantity: Number(detail.quantity)
      }))
    }).subscribe({
      next: (sale) => {
        this.productsService.invalidateCache();
        this.reportsService.invalidateDashboardCache();
        this.message = `Venta registrada correctamente. Codigo #${sale.id}.`;
        this.error = '';
        this.lastSaleId = sale.id;
        this.form.reset({
          userId: payload.userId,
          paymentMethod: 'Efectivo',
          notes: ''
        });
        this.form.setControl('details', this.fb.array([this.createDetailGroup()]));
        this.refreshingProducts = true;
        this.productsService.getAll(true).subscribe({
          next: (products) => {
            this.products = products.filter((product) => product.isActive);
            this.refreshingProducts = false;
            this.cdr.detectChanges();
          },
          error: () => {
            this.refreshingProducts = false;
            this.cdr.detectChanges();
          }
        });
        this.loadRecentSales(true);
        this.cdr.detectChanges();
      },
      error: (response) => {
        this.error = response.error?.message ?? 'No se pudo registrar la venta.';
        this.message = '';
        this.cdr.detectChanges();
      }
    });
  }

  getProductName(productId: number): string {
    return this.products.find((product) => product.id === Number(productId))?.name ?? '';
  }

  getProductPrice(productId: number): number {
    return this.products.find((product) => product.id === Number(productId))?.price ?? 0;
  }

  getTotal(): number {
    return this.details.controls.reduce((sum, control) => {
      const productId = Number(control.get('productId')?.value);
      const quantity = Number(control.get('quantity')?.value);
      return sum + this.getProductPrice(productId) * quantity;
    }, 0);
  }

  private loadRecentSales(forceRefresh = false): void {
    this.loadingSalesHistory = true;
    this.salesService.getAll(forceRefresh).subscribe({
      next: (sales) => {
        this.recentSales = sales.slice(0, 5);
        this.loadingSalesHistory = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingSalesHistory = false;
        this.cdr.detectChanges();
      }
    });
  }

  private createDetailGroup() {
    return this.fb.group({
      productId: [0, [Validators.required, Validators.min(1)]],
      quantity: [1, [Validators.required, Validators.min(1)]]
    });
  }
}
