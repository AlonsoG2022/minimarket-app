import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { read, utils, writeFile } from 'xlsx';
import { Category, Product, ProductImportError, ProductImportRow, SaveProduct } from '../../core/models/minimarket.models';
import { CategoriesService } from '../../core/services/categories.service';
import { CompanyService } from '../../core/services/company.service';
import { ProductsService } from '../../core/services/products.service';
import { SolesPricePipe } from '../../shared/pipes/soles-price.pipe';
import { generateShortName } from '../../shared/short-name';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, SolesPricePipe],
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.css'
})
export class ProductListComponent implements OnInit {
  minimumStock = 5;
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly route = inject(ActivatedRoute);
  private readonly productsService = inject(ProductsService);
  private readonly categoriesService = inject(CategoriesService);
  private readonly companyService = inject(CompanyService);

  shortNameTouched = false;

  readonly form = this.fb.nonNullable.group({
    id: [0],
    name: ['', Validators.required],
    shortName: [''],
    barcode: [''],
    description: [''],
    price: [0, [Validators.required, Validators.min(0.1)]],
    expirationDate: [''],
    salesUnitName: ['unidad', Validators.required],
    purchaseUnitName: ['unidad', Validators.required],
    unitsPerPurchaseUnit: [1, [Validators.required, Validators.min(1)]],
    stock: [0, [Validators.required, Validators.min(0)]],
    minimumStock: [5, [Validators.required, Validators.min(0)]],
    isActive: [true],
    categoryId: [0, [Validators.required, Validators.min(1)]]
  });

  products: Product[] = [];
  categories: Category[] = [];
  searchTerm = '';
  showInactive = false;
  formVisible = false;
  isEditing = false;
  loadingProducts = true;
  loadingCategories = true;
  importingProducts = false;
  message = '';
  error = '';
  importErrors: ProductImportError[] = [];

  // Visibles = activos cuya categoria tambien esta activa. Con "ver ocultos" se muestran todos.
  // Las categorias cargadas aqui son solo las activas, por eso una categoria inactiva oculta sus productos.
  get visibleProducts(): Product[] {
    if (this.showInactive) {
      return this.products;
    }

    const activeCategoryIds = new Set(this.categories.map((category) => category.id));
    return this.products.filter((product) => product.isActive && activeCategoryIds.has(product.categoryId));
  }

  get lowStockProducts(): Product[] {
    return this.visibleProducts
      .filter((product) => product.stock <= this.minimumStock)
      .sort((left, right) => left.stock - right.stock || left.name.localeCompare(right.name, 'es', { sensitivity: 'base' }));
  }

  get filteredProducts(): Product[] {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) {
      return this.visibleProducts;
    }

    return this.visibleProducts.filter((product) =>
      product.name.toLowerCase().includes(term) ||
      product.sku.toLowerCase().includes(term) ||
      product.categoryName.toLowerCase().includes(term) ||
      (product.barcode ?? '').toLowerCase().includes(term) ||
      (product.purchaseBarcode ?? '').toLowerCase().includes(term)
    );
  }

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      this.searchTerm = params.get('q') ?? '';
      this.cdr.detectChanges();
    });

    this.companyService.get().subscribe({
      next: (company) => {
        this.minimumStock = company.minimumStock;
        this.cdr.detectChanges();
      },
      error: () => {}
    });

    // Sugerencia en vivo: mientras se escribe el nombre, propone un nombre corto
    // (a menos que el usuario ya lo haya editado a mano).
    this.form.controls.name.valueChanges.subscribe((name) => {
      if (!this.shortNameTouched) {
        this.form.controls.shortName.setValue(generateShortName(name || ''), { emitEvent: false });
      }
    });

    this.loadData();
  }

  onShortNameInput(): void {
    this.shortNameTouched = true;
  }

  get lowStockSummary(): string {
    const lowStock = this.lowStockProducts;
    if (!lowStock.length) {
      return '';
    }

    const preview = lowStock.slice(0, 3).map((product) => `${product.name} (${product.stock})`);
    const remaining = lowStock.length - preview.length;
    const list = remaining > 0 ? `${preview.join(', ')} y ${remaining} mas` : preview.join(', ');
    return `${lowStock.length} ${lowStock.length === 1 ? 'producto esta' : 'productos estan'} en el umbral (${this.minimumStock}) o por debajo. Los mas bajos: ${list}.`;
  }

  loadData(forceRefreshProducts = false): void {
    this.loadingProducts = true;
    this.productsService.getAll(forceRefreshProducts).subscribe({
      next: (products) => {
        this.products = products;
        this.error = '';
        this.loadingProducts = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudo cargar la lista de productos.';
        this.loadingProducts = false;
        this.cdr.detectChanges();
      }
    });

    // Se refresca tambien la lista de categorias: al importar/crear se pueden auto-crear categorias,
    // y la visibilidad de productos depende de las categorias activas conocidas por el front.
    this.loadingCategories = true;
    this.categoriesService.getAll(forceRefreshProducts).subscribe({
      next: (categories) => {
        this.categories = categories;
        this.loadingCategories = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudieron cargar las categorias.';
        this.loadingCategories = false;
        this.cdr.detectChanges();
      }
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const normalizedBarcode = value.barcode.trim() || null;
    const payload: SaveProduct = {
      name: value.name,
      shortName: value.shortName?.trim() || null,
      barcode: normalizedBarcode,
      purchaseBarcode: normalizedBarcode,
      description: value.description,
      price: Number(value.price),
      expirationDate: value.expirationDate || null,
      salesUnitName: value.salesUnitName,
      purchaseUnitName: value.purchaseUnitName,
      unitsPerPurchaseUnit: Number(value.unitsPerPurchaseUnit),
      stock: this.isEditing ? Number(value.stock) : 0,
      minimumStock: this.minimumStock,
      isActive: value.isActive,
      categoryId: Number(value.categoryId)
    };

    const request = this.isEditing
      ? this.productsService.update(value.id, payload)
      : this.productsService.create(payload);

    request.subscribe({
      next: () => {
        this.message = this.isEditing ? 'Producto actualizado correctamente.' : 'Producto creado correctamente.';
        this.error = '';
        this.resetForm();
        this.loadData(true);
        this.cdr.detectChanges();
      },
      error: (response) => {
        this.error = response.error?.message ?? 'No se pudo guardar el producto.';
        this.message = '';
        this.cdr.detectChanges();
      }
    });
  }

  edit(product: Product): void {
    this.formVisible = true;
    this.isEditing = true;
    this.shortNameTouched = true;
    this.message = '';
    this.error = '';
    this.form.patchValue({
      id: product.id,
      name: product.name,
      shortName: product.shortName ?? '',
      barcode: product.barcode ?? product.purchaseBarcode ?? '',
      description: product.description ?? '',
      price: product.price,
      expirationDate: product.expirationDate ?? '',
      salesUnitName: product.salesUnitName,
      purchaseUnitName: product.purchaseUnitName,
      unitsPerPurchaseUnit: product.unitsPerPurchaseUnit,
      stock: product.stock,
      minimumStock: this.minimumStock,
      isActive: product.isActive,
      categoryId: product.categoryId
    });
  }

  remove(product: Product): void {
    if (!confirm(`Eliminar producto ${product.name}?`)) {
      return;
    }

    this.productsService.delete(product.id).subscribe({
      next: (result) => {
        this.message = result.message;
        this.error = '';
        this.loadData(true);
        this.cdr.detectChanges();
      },
      error: (response) => {
        this.error = response.error?.message ?? 'No se pudo eliminar el producto.';
        this.cdr.detectChanges();
      }
    });
  }

  resetForm(): void {
    this.formVisible = false;
    this.isEditing = false;
    this.shortNameTouched = false;
    this.form.reset({
      id: 0,
      name: '',
      shortName: '',
      barcode: '',
      description: '',
      price: 0,
      expirationDate: '',
      salesUnitName: 'unidad',
      purchaseUnitName: 'unidad',
      unitsPerPurchaseUnit: 1,
      stock: 0,
      minimumStock: this.minimumStock,
      isActive: true,
      categoryId: 0
    });
  }

  clearSearch(): void {
    this.searchTerm = '';
  }

  openCreateForm(): void {
    this.message = '';
    this.error = '';
    this.formVisible = true;
    this.isEditing = false;
    this.shortNameTouched = false;
    this.form.reset({
      id: 0,
      name: '',
      shortName: '',
      barcode: '',
      description: '',
      price: 0,
      expirationDate: '',
      salesUnitName: 'unidad',
      purchaseUnitName: 'unidad',
      unitsPerPurchaseUnit: 1,
      stock: 0,
      minimumStock: this.minimumStock,
      isActive: true,
      categoryId: 0
    });
  }

  downloadImportTemplate(): void {
    const productsWorksheet = utils.aoa_to_sheet([
      ['NombreProducto', 'NombreCorto', 'Precio', 'Costo', 'Categoria', 'CodigoBarras', 'UnidadVenta', 'UnidadCompra', 'UnidadesPorCompra', 'Stock', 'FechaCaducidad']
    ]);
    const categoriesWorksheet = utils.aoa_to_sheet([
      ['Categoria'],
      ...this.categories.map((category) => [category.name])
    ]);
    const workbook = utils.book_new();
    utils.book_append_sheet(workbook, productsWorksheet, 'Productos');
    utils.book_append_sheet(workbook, categoriesWorksheet, 'Categorias');
    writeFile(workbook, 'plantilla-productos-minimarket.xlsx');
  }

  exportProducts(): void {
    if (!this.products.length) {
      this.error = 'No hay productos para exportar.';
      this.message = '';
      this.cdr.detectChanges();
      return;
    }

    // Las columnas deben coincidir con la plantilla de importacion (downloadImportTemplate)
    // para poder exportar, editar y volver a importar el mismo archivo.
    const rows = this.products
      .slice()
      .sort((left, right) => left.name.localeCompare(right.name, 'es', { sensitivity: 'base' }))
      .map((product) => ({
        NombreProducto: product.name,
        NombreCorto: product.shortName ?? '',
        Precio: product.price,
        Costo: product.cost,
        Categoria: product.categoryName,
        CodigoBarras: product.barcode ?? product.purchaseBarcode ?? '',
        UnidadVenta: product.salesUnitName,
        UnidadCompra: product.purchaseUnitName,
        UnidadesPorCompra: product.unitsPerPurchaseUnit,
        Stock: product.stock,
        FechaCaducidad: product.expirationDate ?? ''
      }));

    const worksheet = utils.json_to_sheet(rows);
    const workbook = utils.book_new();
    utils.book_append_sheet(workbook, worksheet, 'Productos');

    // Segunda hoja con las categorias actuales (se actualiza con las que existan al exportar).
    const categoriesWorksheet = utils.aoa_to_sheet([
      ['Categoria'],
      ...this.categories
        .slice()
        .sort((left, right) => left.name.localeCompare(right.name, 'es', { sensitivity: 'base' }))
        .map((category) => [category.name])
    ]);
    utils.book_append_sheet(workbook, categoriesWorksheet, 'Categorias');

    writeFile(workbook, 'productos-minimarket.xlsx');
  }

  openImportPicker(input: HTMLInputElement): void {
    input.value = '';
    input.click();
  }

  importFromFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) {
      return;
    }

    this.importingProducts = true;
    this.message = '';
    this.error = '';
    this.importErrors = [];

    const reader = new FileReader();
    reader.onload = () => {
      try {
        const workbook = read(reader.result, { type: 'array' });
        const sheetName = workbook.SheetNames[0];
        const sheet = workbook.Sheets[sheetName];
        const rawRows = utils.sheet_to_json<Record<string, unknown>>(sheet, {
          defval: '',
          raw: false
        });

        const rows = this.mapImportRows(rawRows);
        if (!rows.length) {
          this.error = 'El archivo no contiene filas validas para importar.';
          this.importingProducts = false;
          this.cdr.detectChanges();
          return;
        }

        this.productsService.importRows(rows).subscribe({
          next: (result) => {
            this.importErrors = result.errors;
            this.message = result.createdCount
              ? `Se importaron ${result.createdCount} producto(s) correctamente.`
              : '';
            this.error = !result.createdCount && result.errors.length
              ? 'No se pudo importar ninguna fila. Revisa el detalle de errores.'
              : '';
            this.importingProducts = false;
            this.loadData(true);
            this.cdr.detectChanges();
          },
          error: (response) => {
            this.error = response.error?.message ?? 'No se pudo importar el archivo.';
            this.importingProducts = false;
            this.cdr.detectChanges();
          }
        });
      } catch {
        this.error = 'No se pudo leer el archivo seleccionado.';
        this.importingProducts = false;
        this.cdr.detectChanges();
      }
    };

    reader.onerror = () => {
      this.error = 'No se pudo leer el archivo seleccionado.';
      this.importingProducts = false;
      this.cdr.detectChanges();
    };

    reader.readAsArrayBuffer(file);
  }

  private mapImportRows(rawRows: Record<string, unknown>[]): ProductImportRow[] {
    return rawRows
      .map((row, index) => {
        const name = this.readCell(row, ['NombreProducto', 'Nombre Producto', 'nombreproducto']);
        const shortName = this.readCell(row, ['NombreCorto', 'Nombre Corto', 'nombrecorto']);
        const categoryName = this.readCell(row, ['Categoria', 'Categoría', 'categoria']);
        const priceText = this.readCell(row, ['Precio', 'precio']);
        const costText = this.readCell(row, ['Costo', 'costo']);
        const barcode = this.readCell(row, ['CodigoBarras', 'Codigo Barras', 'codigoBarras', 'codigobarras']);
        const salesUnitName = this.readCell(row, ['UnidadVenta', 'Unidad Venta', 'unidadventa']);
        const purchaseUnitName = this.readCell(row, ['UnidadCompra', 'Unidad Compra', 'unidadcompra']);
        const unitsPerPurchaseText = this.readCell(row, ['UnidadesPorCompra', 'Unidades Por Compra', 'unidadesporcompra']);
        const stockText = this.readCell(row, ['Stock', 'stock']);
        const expirationDateText = this.readCell(row, ['FechaCaducidad', 'Fecha Caducidad', 'fechacaducidad', 'Caducidad']);
        const normalizedPrice = priceText.replace(',', '.').trim();
        const normalizedCost = costText.replace(',', '.').trim();
        const normalizedUnitsPerPurchase = unitsPerPurchaseText.replace(',', '.').trim();
        const normalizedStock = stockText.replace(',', '.').trim();

        return {
          rowNumber: index + 2,
          name: name.trim(),
          shortName: shortName.trim() || null,
          categoryName: categoryName.trim(),
          barcode: barcode.trim() || null,
          description: null,
          price: Number(normalizedPrice),
          cost: normalizedCost && Number.isFinite(Number(normalizedCost)) ? Number(normalizedCost) : null,
          salesUnitName: salesUnitName.trim() || 'unidad',
          purchaseUnitName: purchaseUnitName.trim() || 'unidad',
          unitsPerPurchaseUnit: normalizedUnitsPerPurchase ? Number(normalizedUnitsPerPurchase) : 1,
          stock: normalizedStock ? Number(normalizedStock) : 0,
          expirationDate: this.normalizeImportDate(expirationDateText),
          isActive: true
        };
      })
      .filter((row) => row.name || row.categoryName || Number.isFinite(row.price) || row.stock > 0 || !!row.barcode);
  }

  private readCell(row: Record<string, unknown>, keys: string[]): string {
    for (const key of keys) {
      if (key in row) {
        return String(row[key] ?? '');
      }
    }

    return '';
  }

  private normalizeImportDate(rawValue: string): string | null {
    const value = rawValue.trim();
    if (!value) {
      return null;
    }

    if (/^\d{4}-\d{2}-\d{2}$/.test(value)) {
      return value;
    }

    const slashMatch = value.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})$/);
    if (slashMatch) {
      const [, first, second, year] = slashMatch;
      return `${year}-${second.padStart(2, '0')}-${first.padStart(2, '0')}`;
    }

    return value;
  }

}
