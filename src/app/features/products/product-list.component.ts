import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { Category, Product, SaveProduct } from '../../core/models/minimarket.models';
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
  message = '';
  error = '';

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
}
