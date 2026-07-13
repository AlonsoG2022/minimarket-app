import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Product, ReceiptConfirmItem, ReceiptScanResult, Supplier } from '../../core/models/minimarket.models';
import { AuthService } from '../../core/services/auth.service';
import { ProductsService } from '../../core/services/products.service';
import { ReceiptsService } from '../../core/services/receipts.service';
import { SuppliersService } from '../../core/services/suppliers.service';

interface ReceiptRow {
  description: string;
  action: 'match' | 'create' | 'skip' | 'gasto';
  productId: number | null;
  name: string;
  shortName: string;
  categoryName: string;
  quantity: number;
  packUnits: number;
  price: number;
  cost: number;
  lineTotal: number;
  matchProductName: string | null;
  matchScore: number;
}

@Component({
  selector: 'app-receipt-upload',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './receipt-upload.component.html'
})
export class ReceiptUploadComponent implements OnInit {
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly receiptsService = inject(ReceiptsService);
  private readonly suppliersService = inject(SuppliersService);
  private readonly productsService = inject(ProductsService);
  private readonly authService = inject(AuthService);

  suppliers: Supplier[] = [];
  products: Product[] = [];
  supplierId = 0;
  newSupplierName = '';
  newSupplierRuc = '';
  invoiceNumber = '';

  scanning = false;
  confirming = false;
  message = '';
  error = '';
  result: ReceiptScanResult | null = null;
  rows: ReceiptRow[] = [];

  ngOnInit(): void {
    this.suppliersService.getAll().subscribe({
      next: (list) => {
        this.suppliers = list.filter((s) => s.isActive);
        this.cdr.detectChanges();
      }
    });
    this.productsService.getAll().subscribe({
      next: (list) => {
        this.products = list.filter((p) => p.isActive);
        this.cdr.detectChanges();
      }
    });
  }

  onFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    this.scanning = true;
    this.message = '';
    this.error = '';
    this.result = null;
    this.rows = [];

    const reader = new FileReader();
    reader.onload = () => {
      const dataUrl = reader.result as string;
      const mediaType = dataUrl.substring(5, dataUrl.indexOf(';'));
      const base64 = dataUrl.substring(dataUrl.indexOf(',') + 1);

      this.receiptsService.scan({ imageBase64: base64, mediaType }).subscribe({
        next: (res) => {
          this.result = res;
          this.supplierId = res.supplierId ?? 0;
          this.newSupplierName = res.supplierName ?? '';
          this.newSupplierRuc = res.supplierRuc ?? '';
          this.invoiceNumber = res.invoiceNumber ?? '';
          this.rows = res.lines.map((line) => ({
            description: line.description,
            action: (this.isFlete(line.description) ? 'gasto' : line.matchProductId ? 'match' : 'create') as ReceiptRow['action'],
            productId: line.matchProductId ?? null,
            name: line.suggestedName,
            shortName: line.suggestedShortName,
            categoryName: line.suggestedCategory,
            quantity: Math.max(1, Math.round(line.quantity || 1)),
            packUnits: Math.max(1, line.packUnits || 1),
            price: line.suggestedPrice,
            cost: line.unitCost,
            lineTotal: line.lineTotal,
            matchProductName: line.matchProductName ?? null,
            matchScore: line.matchScore
          }));
          this.scanning = false;
          this.cdr.detectChanges();
        },
        error: (err) => {
          this.scanning = false;
          this.error = err?.error?.message ?? 'No se pudo leer la boleta.';
          this.cdr.detectChanges();
        }
      });
    };
    reader.onerror = () => {
      this.scanning = false;
      this.error = 'No se pudo leer la imagen seleccionada.';
      this.cdr.detectChanges();
    };
    reader.readAsDataURL(file);
    input.value = '';
  }

  // Descarta la boleta cargada y limpia el formulario para empezar de nuevo.
  cancel(): void {
    this.result = null;
    this.rows = [];
    this.message = '';
    this.error = '';
    this.supplierId = 0;
    this.newSupplierName = '';
    this.newSupplierRuc = '';
    this.invoiceNumber = '';
  }

  // Detecta lineas de flete/reparto/servicio para registrarlas como gasto en caja (no como producto).
  isFlete(description: string): boolean {
    const d = (description || '').toLowerCase();
    return /\b(flete|reparto|delivery|env[ií]o|transporte)\b/.test(d) || d.includes('servicio de flete');
  }

  // Al cambiar la cantidad o las unidades por paquete, recalcula el costo por unidad
  // usando el total real de la boleta (Total ÷ unidades que entran).
  recomputeCost(row: ReceiptRow): void {
    const units = Math.max(1, Math.round(Number(row.quantity) || 1)) * Math.max(1, Math.round(Number(row.packUnits) || 1));
    if (units > 0 && row.lineTotal > 0) {
      row.cost = Math.round((Number(row.lineTotal) / units) * 100) / 100;
    }
  }

  confirm(): void {
    if (this.supplierId === 0 && !this.newSupplierName.trim()) {
      this.error = 'Indica el nombre del proveedor nuevo (o elige uno existente).';
      return;
    }

    const items: ReceiptConfirmItem[] = this.rows.map((row) => ({
      action: row.action,
      productId: row.action === 'match' ? row.productId : null,
      name: row.name,
      shortName: row.shortName,
      categoryName: row.categoryName,
      quantity: Math.max(1, Math.round(Number(row.quantity) || 1)),
      packUnits: Math.max(1, Math.round(Number(row.packUnits) || 1)),
      price: Number(row.price) || 0,
      cost: Number(row.cost) || 0
    }));

    this.confirming = true;
    this.message = '';
    this.error = '';

    this.receiptsService
      .confirm({
        supplierId: this.supplierId,
        newSupplierName: this.supplierId === 0 ? this.newSupplierName.trim() : null,
        newSupplierRuc: this.supplierId === 0 ? this.newSupplierRuc.trim() : null,
        userId: this.authService.session()?.id ?? 0,
        invoiceNumber: this.invoiceNumber.trim() || null,
        items
      })
      .subscribe({
      next: (res) => {
        this.confirming = false;
        this.message = `Compra registrada (#${res.purchaseId}): ${res.created} creado(s), ${res.updated} actualizado(s). Stock y costos actualizados.`;
        this.result = null;
        this.rows = [];
        this.productsService.invalidateCache();
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.confirming = false;
        this.error = err?.error?.message ?? 'No se pudo guardar.';
        this.cdr.detectChanges();
      }
    });
  }
}
