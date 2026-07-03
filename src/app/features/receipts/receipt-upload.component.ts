import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Product, ReceiptConfirmItem, ReceiptScanResult, Supplier } from '../../core/models/minimarket.models';
import { ProductsService } from '../../core/services/products.service';
import { ReceiptsService } from '../../core/services/receipts.service';
import { SuppliersService } from '../../core/services/suppliers.service';

interface ReceiptRow {
  description: string;
  action: 'match' | 'create' | 'skip';
  productId: number | null;
  name: string;
  shortName: string;
  categoryName: string;
  price: number;
  cost: number;
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

  suppliers: Supplier[] = [];
  products: Product[] = [];
  supplierId = 0;

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
          this.rows = res.lines.map((line) => ({
            description: line.description,
            action: line.matchProductId ? 'match' : 'create',
            productId: line.matchProductId ?? null,
            name: line.suggestedName,
            shortName: line.suggestedShortName,
            categoryName: line.suggestedCategory,
            price: line.suggestedPrice,
            cost: line.unitCost,
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

  confirm(): void {
    if (!this.supplierId) {
      this.error = 'Selecciona el proveedor de esta boleta.';
      return;
    }

    const items: ReceiptConfirmItem[] = this.rows.map((row) => ({
      action: row.action,
      productId: row.action === 'match' ? row.productId : null,
      name: row.name,
      shortName: row.shortName,
      categoryName: row.categoryName,
      price: Number(row.price) || 0,
      cost: Number(row.cost) || 0
    }));

    this.confirming = true;
    this.message = '';
    this.error = '';

    this.receiptsService.confirm({ supplierId: this.supplierId, items }).subscribe({
      next: (res) => {
        this.confirming = false;
        this.message = `Guardado: ${res.created} creado(s), ${res.updated} actualizado(s), ${res.costsRecorded} costo(s) registrado(s).`;
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
