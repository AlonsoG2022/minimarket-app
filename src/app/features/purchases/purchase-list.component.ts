import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, ElementRef, OnDestroy, OnInit, ViewChild, inject } from '@angular/core';
import { FormArray, FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { CategoriesService } from '../../core/services/categories.service';
import { AuthService } from '../../core/services/auth.service';
import { ProductsService } from '../../core/services/products.service';
import { PurchasesService } from '../../core/services/purchases.service';
import { SuppliersService } from '../../core/services/suppliers.service';
import { Category, CreatePurchase, Product, Purchase, SaveProduct, Supplier } from '../../core/models/minimarket.models';
import { SolesPricePipe } from '../../shared/pipes/soles-price.pipe';

@Component({
  selector: 'app-purchase-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, SolesPricePipe],
  templateUrl: './purchase-list.component.html',
  styleUrl: './purchase-list.component.css'
})
export class PurchaseListComponent implements OnInit {
  @ViewChild('barcodeInputRef') private barcodeInputRef?: ElementRef<HTMLInputElement>;
  @ViewChild('createProductNameRef') private createProductNameRef?: ElementRef<HTMLInputElement>;

  private readonly fixedMinimumStock = 5;
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly authService = inject(AuthService);
  private readonly categoriesService = inject(CategoriesService);
  private readonly suppliersService = inject(SuppliersService);
  private readonly productsService = inject(ProductsService);
  private readonly purchasesService = inject(PurchasesService);

  readonly productCreateForm = this.fb.nonNullable.group({
    name: ['', Validators.required],
    categoryId: [0, [Validators.required, Validators.min(1)]],
    price: [0, [Validators.required, Validators.min(0.1)]],
    barcode: [''],
    expirationDate: [''],
    salesUnitName: ['unidad', Validators.required],
    purchaseUnitName: ['unidad', Validators.required],
    unitsPerPurchaseUnit: [1, [Validators.required, Validators.min(1)]],
    description: ['']
  });

  readonly form = this.fb.group({
    supplierId: [0, [Validators.required, Validators.min(1)]],
    invoiceNumber: [''],
    notes: [''],
    details: this.fb.array([])
  });

  suppliers: Supplier[] = [];
  categories: Category[] = [];
  products: Product[] = [];
  purchases: Purchase[] = [];
  selectedPurchase?: Purchase;
  barcodeInput = '';
  pendingScannedCode = '';
  quickPackageQuantity = 1;
  manualProductId = 0;
  loading = true;
  loadingHistory = true;
  loadingCategories = true;
  showCreateProductModal = false;
  message = '';
  error = '';
  createProductError = '';

  get details(): FormArray {
    return this.form.get('details') as FormArray;
  }

  get currentItems(): Array<{
    index: number;
    productId: number;
    productName: string;
    purchaseUnitName: string;
    packageQuantity: number;
    unitsPerPackage: number;
    packageCost: number;
    totalUnits: number;
    unitCost: number;
    subtotal: number;
    barcodeSnapshot?: string | null;
  }> {
    return this.details.controls
      .map((control, index) => {
        const productId = Number(control.get('productId')?.value);
        const packageQuantity = Number(control.get('packageQuantity')?.value);
        const unitsPerPackage = Number(control.get('unitsPerPackage')?.value);
        const packageCost = Number(control.get('packageCost')?.value);
        const purchaseUnitName = String(control.get('purchaseUnitName')?.value ?? 'unidad');
        const barcodeSnapshot = String(control.get('barcodeSnapshot')?.value ?? '') || null;
        const product = this.products.find((item) => item.id === productId);

        if (!product) {
          return null;
        }

        const totalUnits = Math.max(1, packageQuantity) * Math.max(1, unitsPerPackage);
        const subtotal = Math.max(0, packageCost) * Math.max(1, packageQuantity);
        const unitCost = totalUnits > 0 ? subtotal / totalUnits : 0;

        return {
          index,
          productId,
          productName: product.name,
          purchaseUnitName,
          packageQuantity,
          unitsPerPackage,
          packageCost,
          totalUnits,
          unitCost,
          subtotal,
          barcodeSnapshot
        };
      })
      .filter((item): item is NonNullable<typeof item> => item !== null);
  }

  ngOnInit(): void {
    this.loadBaseData();
    this.loadPurchases();
    this.focusBarcodeInput();
  }

  ngOnDestroy(): void {
    this.barcodeInputRef = undefined;
  }

  private loadBaseData(forceRefreshProducts = false): void {
    this.loading = true;

    this.categoriesService.getAll().subscribe({
      next: (categories) => {
        this.categories = categories.filter((category) => category.isActive);
        this.loadingCategories = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingCategories = false;
        this.error = 'No se pudieron cargar las categorias.';
        this.cdr.detectChanges();
      }
    });

    this.suppliersService.getAll().subscribe({
      next: (suppliers) => {
        this.suppliers = suppliers.filter((supplier) => supplier.isActive);
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudieron cargar los proveedores.';
        this.cdr.detectChanges();
      }
    });

    this.productsService.getAll(forceRefreshProducts).subscribe({
      next: (products) => {
        this.products = products.filter((product) => product.isActive);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudieron cargar los productos para compras.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private loadPurchases(forceRefresh = false): void {
    this.loadingHistory = true;
    this.purchasesService.getAll(forceRefresh).subscribe({
      next: (purchases) => {
        this.purchases = purchases.slice(0, 8);
        if (this.selectedPurchase) {
          this.selectedPurchase = this.purchases.find((purchase) => purchase.id === this.selectedPurchase?.id);
        }
        this.loadingHistory = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingHistory = false;
        this.cdr.detectChanges();
      }
    });
  }

  addByBarcode(): void {
    const scannedCode = this.barcodeInput.trim();
    if (!scannedCode) {
      return;
    }

    const product = this.findProductByScannedCode(scannedCode);
    if (!product) {
      this.pendingScannedCode = scannedCode;
      this.error = '';
      this.message = '';
      this.openCreateProductModal(scannedCode);
      this.cdr.detectChanges();
      return;
    }

    this.addProduct(product, this.quickPackageQuantity, scannedCode);
  }

  addManualProduct(): void {
    const product = this.products.find((item) => item.id === Number(this.manualProductId));
    if (!product) {
      return;
    }

    this.addProduct(product, this.quickPackageQuantity, null);
    this.manualProductId = 0;
  }

  addProduct(product: Product, packageQuantity = this.quickPackageQuantity, barcodeSnapshot: string | null = null): void {
    const safePackageQuantity = Math.max(1, Number(packageQuantity) || 1);
    const existingIndex = this.details.controls.findIndex(
      (control) => Number(control.get('productId')?.value) === product.id
    );

    if (existingIndex >= 0) {
      const control = this.details.at(existingIndex);
      const currentQuantity = Number(control.get('packageQuantity')?.value) || 0;
      control.patchValue({
        packageQuantity: currentQuantity + safePackageQuantity,
        barcodeSnapshot: barcodeSnapshot ?? control.get('barcodeSnapshot')?.value ?? null
      });
    } else {
      this.details.push(this.createDetailGroup(product, safePackageQuantity, barcodeSnapshot));
    }

    this.barcodeInput = '';
    this.pendingScannedCode = '';
    this.quickPackageQuantity = 1;
    this.message = '';
    this.error = '';
    this.cdr.detectChanges();
    this.focusBarcodeInput();
  }

  removeItem(index: number): void {
    this.details.removeAt(index);
  }

  updateLineValue(index: number, field: 'packageQuantity' | 'unitsPerPackage' | 'packageCost', value: number): void {
    const control = this.details.at(index);
    const normalizedValue = field === 'packageCost'
      ? Math.max(0, Number(value) || 0)
      : Math.max(1, Number(value) || 1);
    control.patchValue({ [field]: normalizedValue });
  }

  submit(): void {
    const currentUser = this.authService.session();
    if (!currentUser) {
      this.error = 'Tu sesion no esta disponible. Vuelve a ingresar.';
      this.message = '';
      return;
    }

    if (this.form.invalid || !this.currentItems.length) {
      this.form.markAllAsTouched();
      this.error = !this.currentItems.length
        ? 'Agrega al menos un producto antes de registrar la compra.'
        : 'Completa los datos obligatorios de la compra.';
      this.message = '';
      return;
    }

    const rawValue = this.form.getRawValue();
    const payload: CreatePurchase = {
      supplierId: Number(rawValue.supplierId),
      userId: currentUser.id,
      invoiceNumber: rawValue.invoiceNumber || null,
      notes: rawValue.notes || null,
      details: this.currentItems.map((item) => ({
        productId: item.productId,
        packageQuantity: item.packageQuantity,
        unitsPerPackage: item.unitsPerPackage,
        packageCost: item.packageCost,
        purchaseUnitName: item.purchaseUnitName,
        barcodeSnapshot: item.barcodeSnapshot ?? null
      }))
    };

    this.purchasesService.create(payload).subscribe({
      next: (purchase) => {
        this.productsService.invalidateCache();
        this.message = `Compra registrada correctamente. Codigo #${purchase.id}.`;
        this.error = '';
        this.form.reset({
          supplierId: rawValue.supplierId,
          invoiceNumber: '',
          notes: ''
        });
        this.form.setControl('details', this.fb.array([]));
        this.barcodeInput = '';
        this.pendingScannedCode = '';
        this.quickPackageQuantity = 1;
        this.loadBaseData(true);
        this.loadPurchases(true);
        this.cdr.detectChanges();
        this.focusBarcodeInput();
      },
      error: (response) => {
        this.error = response.error?.message ?? 'No se pudo registrar la compra.';
        this.message = '';
        this.cdr.detectChanges();
      }
    });
  }

  getTotal(): number {
    return this.currentItems.reduce((sum, item) => sum + item.subtotal, 0);
  }

  showPurchaseDetail(purchase: Purchase): void {
    this.selectedPurchase = purchase;
  }

  closePurchaseDetail(): void {
    this.selectedPurchase = undefined;
  }

  openCreateProductModal(scannedCode: string): void {
    this.showCreateProductModal = true;
    this.createProductError = '';
    this.productCreateForm.reset({
      name: '',
      categoryId: 0,
      price: 0,
      barcode: scannedCode,
      expirationDate: '',
      salesUnitName: 'unidad',
      purchaseUnitName: 'unidad',
      unitsPerPurchaseUnit: 1,
      description: ''
    });
    setTimeout(() => this.createProductNameRef?.nativeElement.focus(), 0);
  }

  closeCreateProductModal(): void {
    this.showCreateProductModal = false;
    this.createProductError = '';
    this.focusBarcodeInput();
  }

  createProductOnTheFly(): void {
    if (this.productCreateForm.invalid) {
      this.productCreateForm.markAllAsTouched();
      return;
    }

    const value = this.productCreateForm.getRawValue();
    const normalizedBarcode = value.barcode.trim() || null;
    const payload: SaveProduct = {
      name: value.name,
      barcode: normalizedBarcode,
      purchaseBarcode: normalizedBarcode,
      description: value.description || null,
      price: Number(value.price),
      expirationDate: value.expirationDate || null,
      salesUnitName: value.salesUnitName,
      purchaseUnitName: value.purchaseUnitName,
      unitsPerPurchaseUnit: Number(value.unitsPerPurchaseUnit),
      stock: 0,
      minimumStock: this.fixedMinimumStock,
      isActive: true,
      categoryId: Number(value.categoryId)
    };

    this.productsService.create(payload).subscribe({
      next: (product) => {
        this.productsService.invalidateCache();
        this.products = [...this.products, product].sort((left, right) =>
          left.name.localeCompare(right.name, 'es', { sensitivity: 'base' })
        );
        this.showCreateProductModal = false;
        this.createProductError = '';
        this.message = `Producto ${product.name} creado y listo para agregar a la compra.`;
        this.error = '';
        this.addProduct(product, this.quickPackageQuantity, this.pendingScannedCode || product.barcode || product.purchaseBarcode || null);
        this.productCreateForm.reset({
          name: '',
          categoryId: 0,
          price: 0,
          barcode: '',
          expirationDate: '',
          salesUnitName: 'unidad',
          purchaseUnitName: 'unidad',
          unitsPerPurchaseUnit: 1,
          description: ''
        });
        this.cdr.detectChanges();
      },
      error: (response) => {
        this.createProductError = response.error?.message ?? 'No se pudo crear el producto.';
        this.cdr.detectChanges();
      }
    });
  }

  private focusBarcodeInput(): void {
    setTimeout(() => this.barcodeInputRef?.nativeElement.focus(), 0);
  }

  private findProductByScannedCode(scannedCode: string): Product | undefined {
    const normalizedCode = scannedCode.trim().toLowerCase();
    return this.products.find((product) =>
      (product.purchaseBarcode ?? '').toLowerCase() === normalizedCode ||
      (product.barcode ?? '').toLowerCase() === normalizedCode ||
      product.sku.toLowerCase() === normalizedCode
    );
  }

  private createDetailGroup(product: Product, packageQuantity: number, barcodeSnapshot: string | null) {
    return this.fb.group({
      productId: [product.id, [Validators.required, Validators.min(1)]],
      packageQuantity: [packageQuantity, [Validators.required, Validators.min(1)]],
      unitsPerPackage: [product.unitsPerPurchaseUnit || 1, [Validators.required, Validators.min(1)]],
      packageCost: [product.cost * Math.max(1, product.unitsPerPurchaseUnit || 1), [Validators.required, Validators.min(0.01)]],
      purchaseUnitName: [product.purchaseUnitName || 'unidad', Validators.required],
      barcodeSnapshot: [barcodeSnapshot]
    });
  }
}
