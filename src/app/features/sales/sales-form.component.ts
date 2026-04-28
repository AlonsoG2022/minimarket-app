import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { Product, Sale, User } from '../../core/models/minimarket.models';
import { ProductsService } from '../../core/services/products.service';
import { ReportsService } from '../../core/services/reports.service';
import { SalesService } from '../../core/services/sales.service';
import { UsersService } from '../../core/services/users.service';
import { SolesPricePipe } from '../../shared/pipes/soles-price.pipe';

@Component({
  selector: 'app-sales-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, SolesPricePipe],
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
    details: this.fb.array([])
  });

  products: Product[] = [];
  users: User[] = [];
  recentSales: Sale[] = [];
  selectedSale?: Sale;
  lastSaleId?: number;
  productSearch = '';
  quickQuantity = 1;
  showNotes = false;
  loading = true;
  refreshingProducts = false;
  loadingSalesHistory = true;
  message = '';
  error = '';

  get details(): FormArray {
    return this.form.get('details') as FormArray;
  }

  get hasSearchTerm(): boolean {
    return this.productSearch.trim().length > 0;
  }

  get filteredProducts(): Product[] {
    const term = this.productSearch.trim().toLowerCase();
    if (!term) {
      return [];
    }

    return this.products
      .filter((product) => product.name.toLowerCase().includes(term))
      .slice(0, 8);
  }

  get currentItems(): Array<{
    index: number;
    productId: number;
    productName: string;
    sku: string;
    quantity: number;
    unitPrice: number;
    subtotal: number;
    stock: number;
  }> {
    return this.details.controls
      .map((control, index) => {
        const productId = Number(control.get('productId')?.value);
        const quantity = Number(control.get('quantity')?.value);
        const product = this.products.find((item) => item.id === productId);

        if (!product) {
          return null;
        }

        return {
          index,
          productId,
          productName: product.name,
          sku: product.sku,
          quantity,
          unitPrice: product.price,
          subtotal: product.price * quantity,
          stock: product.stock
        };
      })
      .filter((item): item is NonNullable<typeof item> => item !== null);
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
        if (!this.form.get('userId')?.value && this.users.length) {
          this.form.patchValue({ userId: this.users[0].id });
        }
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

  addProduct(product: Product, quantity = this.quickQuantity): void {
    const safeQuantity = Math.max(1, Number(quantity) || 1);
    const existingIndex = this.details.controls.findIndex(
      (control) => Number(control.get('productId')?.value) === product.id
    );

    if (existingIndex >= 0) {
      const control = this.details.at(existingIndex);
      const currentQuantity = Number(control.get('quantity')?.value) || 0;
      control.patchValue({ quantity: currentQuantity + safeQuantity });
    } else {
      this.details.push(this.createDetailGroup(product.id, safeQuantity));
    }

    this.productSearch = '';
    this.quickQuantity = 1;
  }

  addFirstMatchingProduct(): void {
    const product = this.filteredProducts[0];
    if (!product) {
      return;
    }

    this.addProduct(product);
  }

  removeItem(index: number): void {
    this.details.removeAt(index);
  }

  updateQuantity(index: number, quantity: number): void {
    const control = this.details.at(index);
    control.patchValue({ quantity: Math.max(1, Number(quantity) || 1) });
  }

  incrementQuantity(index: number): void {
    const control = this.details.at(index);
    const currentQuantity = Number(control.get('quantity')?.value) || 1;
    control.patchValue({ quantity: currentQuantity + 1 });
  }

  decrementQuantity(index: number): void {
    const control = this.details.at(index);
    const currentQuantity = Number(control.get('quantity')?.value) || 1;
    if (currentQuantity <= 1) {
      return;
    }

    control.patchValue({ quantity: currentQuantity - 1 });
  }

  submit(): void {
    const saleDetails = this.currentItems.map((item) => ({
      productId: item.productId,
      quantity: item.quantity
    }));

    if (this.form.get('userId')?.invalid || this.form.get('paymentMethod')?.invalid || !saleDetails.length) {
      this.form.markAllAsTouched();
      if (!saleDetails.length) {
        this.error = 'Agrega al menos un producto antes de cobrar.';
        this.message = '';
      }
      return;
    }

    const payload = this.form.getRawValue();

    this.salesService.create({
      userId: Number(payload.userId),
      paymentMethod: payload.paymentMethod ?? 'Efectivo',
      notes: payload.notes ?? '',
      details: saleDetails
    }).subscribe({
      next: (sale) => {
        this.productsService.invalidateCache();
        this.reportsService.invalidateDashboardCache();
        this.message = `Venta registrada correctamente. Codigo #${sale.id}.`;
        this.error = '';
        this.lastSaleId = sale.id;
        this.productSearch = '';
        this.quickQuantity = 1;
        this.showNotes = false;
        this.form.reset({
          userId: payload.userId,
          paymentMethod: 'Efectivo',
          notes: ''
        });
        this.form.setControl('details', this.fb.array([]));
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
    return this.currentItems.reduce((sum, item) => sum + item.subtotal, 0);
  }

  private loadRecentSales(forceRefresh = false): void {
    this.loadingSalesHistory = true;
    this.salesService.getAll(forceRefresh).subscribe({
      next: (sales) => {
        this.recentSales = sales.slice(0, 5);
        if (this.selectedSale) {
          this.selectedSale = this.recentSales.find((sale) => sale.id === this.selectedSale?.id);
        }
        this.loadingSalesHistory = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingSalesHistory = false;
        this.cdr.detectChanges();
      }
    });
  }

  showSaleDetail(sale: Sale): void {
    this.selectedSale = sale;
  }

  closeSaleDetail(): void {
    this.selectedSale = undefined;
  }

  private createDetailGroup(productId: number, quantity: number) {
    return this.fb.group({
      productId: [productId, [Validators.required, Validators.min(1)]],
      quantity: [quantity, [Validators.required, Validators.min(1)]]
    });
  }
}
