import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormArray, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { CashSession, PrintJob, Product, Sale } from '../../core/models/minimarket.models';
import { AuthService } from '../../core/services/auth.service';
import { CashSessionsService } from '../../core/services/cash-sessions.service';
import { PrintJobsService } from '../../core/services/print-jobs.service';
import { ProductsService } from '../../core/services/products.service';
import { ReportsService } from '../../core/services/reports.service';
import { SalesService } from '../../core/services/sales.service';
import { SolesPricePipe } from '../../shared/pipes/soles-price.pipe';

@Component({
  selector: 'app-sales-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, SolesPricePipe],
  templateUrl: './sales-form.component.html',
  styleUrl: './sales-form.component.css'
})
export class SalesFormComponent implements OnInit {
  private readonly fixedMinimumStock = 5;
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
  private lowStockNoticeTimeout?: ReturnType<typeof setTimeout>;
  readonly authService = inject(AuthService);
  private readonly cashSessionsService = inject(CashSessionsService);
  private readonly salesService = inject(SalesService);
  private readonly printJobsService = inject(PrintJobsService);
  private readonly productsService = inject(ProductsService);
  private readonly reportsService = inject(ReportsService);

  readonly form = this.fb.group({
    paymentMethod: ['Efectivo', Validators.required],
    notes: [''],
    details: this.fb.array([])
  });

  products: Product[] = [];
  recentSales: Sale[] = [];
  recentPrintJobs: PrintJob[] = [];
  selectedSale?: Sale;
  currentCashSession?: CashSession | null;
  printableSale?: Sale;
  lastSaleId?: number;
  productSearch = '';
  quickQuantity = 1;
  showNotes = false;
  loading = true;
  loadingCashSession = true;
  refreshingProducts = false;
  loadingSalesHistory = true;
  loadingPrintJobs = true;
  message = '';
  error = '';
  lowStockNotice = '';
  printQueueMessage = '';
  printQueueError = '';

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
      .filter((product) =>
        product.name.toLowerCase().includes(term) ||
        product.sku.toLowerCase().includes(term) ||
        (product.barcode ?? '').toLowerCase().includes(term)
      )
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
    this.loadCurrentCashSession();
    this.productsService.getAll().subscribe({
      next: (products) => {
        this.products = products.filter((product) => product.isActive);
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
    this.loadRecentPrintJobs();
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
    const currentUser = this.authService.session();
    const ticketItemsSnapshot = this.currentItems.map((item) => ({ ...item }));
    const saleDetails = this.currentItems.map((item) => ({
      productId: item.productId,
      quantity: item.quantity
    }));

    if (!currentUser || this.form.get('paymentMethod')?.invalid || !saleDetails.length) {
      this.form.markAllAsTouched();
      if (!currentUser) {
        this.error = 'Tu sesión no está disponible. Vuelve a ingresar.';
        this.message = '';
      }
      if (!saleDetails.length) {
        this.error = 'Agrega al menos un producto antes de cobrar.';
        this.message = '';
      }
      return;
    }

    if (!this.currentCashSession || this.currentCashSession.status.toLowerCase() !== 'abierta') {
      this.error = 'Debes abrir caja antes de registrar una venta.';
      this.message = '';
      this.cdr.detectChanges();
      return;
    }

    const payload = this.form.getRawValue();

    this.salesService.create({
      userId: currentUser.id,
      cashSessionId: this.currentCashSession.id,
      paymentMethod: payload.paymentMethod ?? 'Efectivo',
      notes: payload.notes ?? '',
      details: saleDetails
    }).subscribe({
      next: (sale) => {
        const printableSale = {
          ...sale,
          details: sale.details.map((detail) => {
            const snapshot = ticketItemsSnapshot.find((item) => item.productId === detail.productId);
            return {
              ...detail,
              productName: detail.productName?.trim() || snapshot?.productName || `Producto #${detail.productId}`,
              unitPrice: detail.unitPrice || snapshot?.unitPrice || 0,
              subtotal: detail.subtotal || snapshot?.subtotal || 0
            };
          })
        };

        this.productsService.invalidateCache();
        this.reportsService.invalidateDashboardCache();
        this.message = `Venta registrada correctamente. Codigo #${sale.id}.`;
        this.error = '';
        this.clearLowStockNotice();
        this.lastSaleId = sale.id;
        this.printableSale = printableSale;
        this.productSearch = '';
        this.quickQuantity = 1;
        this.showNotes = false;
        this.form.reset({
          paymentMethod: 'Efectivo',
          notes: ''
        });
        this.form.setControl('details', this.fb.array([]));
        this.refreshingProducts = true;
        this.productsService.getAll(true).subscribe({
          next: (products) => {
            this.products = products.filter((product) => product.isActive);
            const notice = this.buildLowStockNotice(saleDetails.map((detail) => detail.productId));
            if (notice) {
              this.showLowStockNotice(notice);
            }
            this.refreshingProducts = false;
            this.cdr.detectChanges();
          },
          error: () => {
            this.refreshingProducts = false;
            this.cdr.detectChanges();
          }
        });
        this.cashSessionsService.invalidateCurrent(currentUser.id);
        this.loadCurrentCashSession(true);
        this.loadRecentSales(true);
        this.loadRecentPrintJobs(true);
        this.cdr.detectChanges();
      },
      error: (response) => {
        this.error = response.error?.message ?? 'No se pudo registrar la venta.';
        this.message = '';
        this.cdr.detectChanges();
      }
    });
  }

  private buildLowStockNotice(purchasedProductIds: number[]): string {
    const uniqueIds = [...new Set(purchasedProductIds)];
    const lowStockProducts = uniqueIds
      .map((productId) => this.products.find((product) => product.id === productId))
      .filter((product): product is Product => !!product && product.stock <= this.fixedMinimumStock);

    if (!lowStockProducts.length) {
      return '';
    }

    const labels = lowStockProducts.map((product) => `${product.name} (${product.stock})`);
    const productsText = labels.join(', ');
    return `Aviso: ${productsText} ya ${lowStockProducts.length === 1 ? 'esta' : 'estan'} en stock minimo (${this.fixedMinimumStock}).`;
  }

  dismissLowStockNotice(): void {
    this.clearLowStockNotice();
    this.cdr.detectChanges();
  }

  private showLowStockNotice(notice: string): void {
    this.clearLowStockNotice();
    this.lowStockNotice = notice;
    this.lowStockNoticeTimeout = setTimeout(() => {
      this.lowStockNotice = '';
      this.lowStockNoticeTimeout = undefined;
      this.cdr.detectChanges();
    }, 6000);
  }

  private clearLowStockNotice(): void {
    if (this.lowStockNoticeTimeout) {
      clearTimeout(this.lowStockNoticeTimeout);
      this.lowStockNoticeTimeout = undefined;
    }

    this.lowStockNotice = '';
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

  closePrintableTicket(): void {
    this.printableSale = undefined;
  }

  loadRecentPrintJobs(forceRefresh = false): void {
    this.loadingPrintJobs = true;
    this.printJobsService.getRecent(forceRefresh).subscribe({
      next: (jobs) => {
        this.recentPrintJobs = jobs;
        this.loadingPrintJobs = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingPrintJobs = false;
        this.cdr.detectChanges();
      }
    });
  }

  requeueLastTicket(sale: Sale): void {
    if (!sale.lastPrintJobId) {
      return;
    }

    this.requeuePrintJob(sale.lastPrintJobId, `Ticket de la venta #${sale.id} reencolado correctamente.`);
  }

  requeuePrintJob(jobId: number, successMessage = 'Trabajo de impresion reencolado correctamente.'): void {
    this.printJobsService.requeue(jobId).subscribe({
      next: () => {
        this.printQueueMessage = successMessage;
        this.printQueueError = '';
        this.loadRecentPrintJobs(true);
        this.loadRecentSales(true);
        this.cdr.detectChanges();
      },
      error: (response) => {
        this.printQueueError = response.error?.message ?? 'No se pudo reencolar el ticket.';
        this.printQueueMessage = '';
        this.cdr.detectChanges();
      }
    });
  }

  printTicket(): void {
    if (!this.printableSale) {
      return;
    }

    const receiptWindow = window.open('', '_blank', 'width=420,height=720');
    if (!receiptWindow) {
      this.error = 'No se pudo abrir la ventana de impresion. Revisa si el navegador bloqueo la accion.';
      this.cdr.detectChanges();
      return;
    }

    const sale = this.printableSale;
    const itemsHtml = sale.details.map((detail) => `
      <div class="item">
        <div class="product">
          <strong>${this.escapeHtml(detail.productName)}</strong>
          <span>${detail.quantity} x ${this.formatCurrency(detail.unitPrice)}</span>
        </div>
        <strong>${this.formatCurrency(detail.subtotal)}</strong>
      </div>
    `).join('');

    const totalUnits = sale.details.reduce((sum, detail) => sum + detail.quantity, 0);

    receiptWindow.document.write(`
      <!doctype html>
      <html lang="es">
        <head>
          <meta charset="utf-8">
          <title>Ticket ${sale.id}</title>
          <style>
            body { font-family: Arial, sans-serif; margin: 0; padding: 18px; color: #2d1e15; }
            .receipt { max-width: 340px; margin: 0 auto; }
            .header, .footer { text-align: center; }
            .header strong { display:block; font-size:18px; margin-bottom:4px; }
            .header small, .meta span, .item span, .notes span, .totals span { color:#6b5a4b; }
            .meta, .notes, .footer, .totals { display:flex; justify-content:space-between; gap:12px; }
            .meta { margin: 14px 0; }
            .meta div { flex: 1 1 0; display:grid; gap: 2px; }
            .item { display:flex; justify-content:space-between; gap:10px; align-items:start; padding:10px 0; border-bottom:1px dashed #d5c4b4; }
            .product { display:grid; gap:2px; }
            .product strong { font-size:14px; line-height:1.35; }
            .item strong:last-child { text-align:right; white-space:nowrap; }
            .totals { margin-top: 12px; }
            .totals div { flex:1 1 0; display:grid; gap:2px; }
            .notes { margin-top: 12px; }
            .footer { margin-top: 16px; padding-top: 12px; border-top:1px dashed #d5c4b4; font-size:20px; font-weight:700; }
          </style>
        </head>
        <body>
          <article class="receipt">
            <header class="header">
              <strong>Minimarket</strong>
              <div>Ticket de venta</div>
              <small>#${sale.id} · ${this.formatDateTime(sale.saleDate)}</small>
            </header>
            <section class="meta">
              <div><span>Cajero</span><strong>${this.escapeHtml(sale.userName)}</strong></div>
              <div><span>Pago</span><strong>${this.escapeHtml(sale.paymentMethod)}</strong></div>
            </section>
            <section>${itemsHtml}</section>
            <section class="totals">
              <div><span>Items</span><strong>${sale.details.length}</strong></div>
              <div><span>Unidades</span><strong>${totalUnits}</strong></div>
            </section>
            ${sale.notes ? `<section class="notes"><span>Notas</span><strong>${this.escapeHtml(sale.notes)}</strong></section>` : ''}
            <footer class="footer"><span>Total</span><strong>${this.formatCurrency(sale.total)}</strong></footer>
          </article>
          <script>
            window.onload = function () {
              window.print();
            };
          </script>
        </body>
      </html>
    `);
    receiptWindow.document.close();
  }

  private createDetailGroup(productId: number, quantity: number) {
    return this.fb.group({
      productId: [productId, [Validators.required, Validators.min(1)]],
      quantity: [quantity, [Validators.required, Validators.min(1)]]
    });
  }

  private loadCurrentCashSession(forceRefresh = false): void {
    const currentUser = this.authService.session();
    if (!currentUser) {
      this.loadingCashSession = false;
      return;
    }

    this.loadingCashSession = true;
    this.cashSessionsService.getCurrent(currentUser.id, forceRefresh).subscribe({
      next: (session) => {
        this.currentCashSession = session;
        this.loadingCashSession = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.currentCashSession = null;
        this.loadingCashSession = false;
        this.cdr.detectChanges();
      }
    });
  }

  private formatCurrency(value: number): string {
    return new Intl.NumberFormat('es-PE', {
      style: 'currency',
      currency: 'PEN',
      minimumFractionDigits: 2
    }).format(value);
  }

  private formatDateTime(value: string): string {
    return new Intl.DateTimeFormat('es-PE', {
      dateStyle: 'short',
      timeStyle: 'short'
    }).format(new Date(value));
  }

  private escapeHtml(value: string): string {
    return value
      .replaceAll('&', '&amp;')
      .replaceAll('<', '&lt;')
      .replaceAll('>', '&gt;')
      .replaceAll('"', '&quot;')
      .replaceAll("'", '&#39;');
  }

  getPrintStatusLabel(status?: string | null): string {
    switch ((status ?? '').toLowerCase()) {
      case 'pendiente':
        return 'Pendiente';
      case 'imprimiendo':
        return 'Imprimiendo';
      case 'impreso':
        return 'Impreso';
      case 'error':
        return 'Error';
      default:
        return 'Sin cola';
    }
  }
}
