import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { read, utils, writeFile } from 'xlsx';
import { Category, Product, ProductImportError, ProductImportRow, SaveProduct } from '../../core/models/minimarket.models';
import { CategoriesService } from '../../core/services/categories.service';
import { ProductsService } from '../../core/services/products.service';
import { SolesPricePipe } from '../../shared/pipes/soles-price.pipe';

@Component({
  selector: 'app-product-list',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, SolesPricePipe],
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.css'
})
export class ProductListComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly route = inject(ActivatedRoute);
  private readonly productsService = inject(ProductsService);
  private readonly categoriesService = inject(CategoriesService);

  readonly form = this.fb.nonNullable.group({
    id: [0],
    name: ['', Validators.required],
    description: [''],
    price: [0, [Validators.required, Validators.min(0.1)]],
    stock: [0, [Validators.required, Validators.min(0)]],
    minimumStock: [0, [Validators.required, Validators.min(0)]],
    isActive: [true],
    categoryId: [0, [Validators.required, Validators.min(1)]]
  });

  products: Product[] = [];
  categories: Category[] = [];
  searchTerm = '';
  formVisible = false;
  isEditing = false;
  loadingProducts = true;
  loadingCategories = true;
  importingProducts = false;
  message = '';
  error = '';
  importErrors: ProductImportError[] = [];

  get filteredProducts(): Product[] {
    const term = this.searchTerm.trim().toLowerCase();
    if (!term) {
      return this.products;
    }

    return this.products.filter((product) =>
      product.name.toLowerCase().includes(term) ||
      product.sku.toLowerCase().includes(term) ||
      product.categoryName.toLowerCase().includes(term)
    );
  }

  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      this.searchTerm = params.get('q') ?? '';
      this.cdr.detectChanges();
    });

    this.loadData();
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

    this.loadingCategories = true;
    this.categoriesService.getAll().subscribe({
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
    const payload: SaveProduct = {
      name: value.name,
      description: value.description,
      price: Number(value.price),
      stock: Number(value.stock),
      minimumStock: Number(value.minimumStock),
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
    this.message = '';
    this.error = '';
    this.form.patchValue({
      id: product.id,
      name: product.name,
      description: product.description ?? '',
      price: product.price,
      stock: product.stock,
      minimumStock: product.minimumStock,
      isActive: product.isActive,
      categoryId: product.categoryId
    });
  }

  remove(product: Product): void {
    if (!confirm(`Eliminar producto ${product.name}?`)) {
      return;
    }

    this.productsService.delete(product.id).subscribe({
      next: () => {
        this.message = 'Producto eliminado.';
        this.error = '';
        this.loadData(true);
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudo eliminar el producto.';
        this.cdr.detectChanges();
      }
    });
  }

  resetForm(): void {
    this.formVisible = false;
    this.isEditing = false;
    this.form.reset({
      id: 0,
      name: '',
      description: '',
      price: 0,
      stock: 0,
      minimumStock: 0,
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
    this.form.reset({
      id: 0,
      name: '',
      description: '',
      price: 0,
      stock: 0,
      minimumStock: 0,
      isActive: true,
      categoryId: 0
    });
  }

  downloadImportTemplate(): void {
    const productsWorksheet = utils.aoa_to_sheet([
      ['NombreProducto', 'Precio', 'Categoria', 'Stock']
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

    const rows = this.products
      .slice()
      .sort((left, right) => left.name.localeCompare(right.name, 'es', { sensitivity: 'base' }))
      .map((product) => ({
        NombreProducto: product.name,
        Precio: product.price,
        Categoria: product.categoryName,
        Stock: product.stock,
        SKU: product.sku,
        Activo: product.isActive ? 'Si' : 'No'
      }));

    const worksheet = utils.json_to_sheet(rows);
    const workbook = utils.book_new();
    utils.book_append_sheet(workbook, worksheet, 'Productos');
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
        const categoryName = this.readCell(row, ['Categoria', 'Categoría', 'categoria']);
        const priceText = this.readCell(row, ['Precio', 'precio']);
        const stockText = this.readCell(row, ['Stock', 'stock']);
        const normalizedPrice = priceText.replace(',', '.').trim();
        const normalizedStock = stockText.replace(',', '.').trim();

        return {
          rowNumber: index + 2,
          name: name.trim(),
          categoryName: categoryName.trim(),
          price: Number(normalizedPrice),
          stock: normalizedStock ? Number(normalizedStock) : 0
        };
      })
      .filter((row) => row.name || row.categoryName || Number.isFinite(row.price) || row.stock > 0);
  }

  private readCell(row: Record<string, unknown>, keys: string[]): string {
    for (const key of keys) {
      if (key in row) {
        return String(row[key] ?? '');
      }
    }

    return '';
  }
}
