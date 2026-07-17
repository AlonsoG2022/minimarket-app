import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Category, SaveCategory } from '../../core/models/minimarket.models';
import { CategoriesService } from '../../core/services/categories.service';

@Component({
  selector: 'app-category-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './category-list.component.html',
  styleUrl: './category-list.component.css'
})
export class CategoryListComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly categoriesService = inject(CategoriesService);
  private editingCategoryId?: number;

  readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    description: [''],
    isActive: [true],
    priceAdjustmentPercentage: [0, [Validators.required, Validators.min(0)]]
  });

  categories: Category[] = [];
  loading = true;
  formVisible = false;
  isEditing = false;
  message = '';
  error = '';

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(): void {
    this.loading = true;
    this.categoriesService.getAllIncludingInactive().subscribe({
      next: (categories) => {
        this.categories = categories;
        this.loading = false;
        this.error = '';
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.error = 'No se pudieron cargar las categorias.';
        this.cdr.detectChanges();
      }
    });
  }

  toggleActive(category: Category): void {
    this.categoriesService
      .update(category.id, {
        name: category.name,
        description: category.description,
        isActive: !category.isActive,
        priceAdjustmentPercentage: category.priceAdjustmentPercentage ?? 0
      })
      .subscribe({
        next: () => {
          this.message = category.isActive
            ? `Categoria "${category.name}" desactivada. Sus productos ya no se muestran.`
            : `Categoria "${category.name}" activada.`;
          this.error = '';
          this.loadCategories();
          this.cdr.detectChanges();
        },
        error: (response) => {
          this.message = '';
          this.error = response.error?.message ?? 'No se pudo cambiar el estado de la categoria.';
          this.cdr.detectChanges();
        }
      });
  }

  edit(category: Category): void {
    this.formVisible = true;
    this.isEditing = true;
    this.editingCategoryId = category.id;
    this.message = '';
    this.error = '';
    this.form.reset({
      name: category.name,
      description: category.description ?? '',
      isActive: category.isActive,
      priceAdjustmentPercentage: category.priceAdjustmentPercentage ?? 0
    });
    this.cdr.detectChanges();
  }

  resetForm(): void {
    this.formVisible = false;
    this.isEditing = false;
    this.editingCategoryId = undefined;
    this.form.reset({
      name: '',
      description: '',
      isActive: true,
      priceAdjustmentPercentage: 0
    });
  }

  openCreateForm(): void {
    this.message = '';
    this.error = '';
    this.formVisible = true;
    this.isEditing = false;
    this.editingCategoryId = undefined;
    this.form.reset({
      name: '',
      description: '',
      isActive: true,
      priceAdjustmentPercentage: 0
    });
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const payload: SaveCategory = {
      name: value.name,
      description: value.description,
      isActive: value.isActive,
      priceAdjustmentPercentage: Number(value.priceAdjustmentPercentage) || 0
    };

    const request = this.isEditing && this.editingCategoryId
      ? this.categoriesService.update(this.editingCategoryId, payload)
      : this.categoriesService.create(payload);

    request.subscribe({
      next: () => {
        this.message = this.isEditing
          ? 'Categoria actualizada correctamente.'
          : 'Categoria registrada correctamente.';
        this.error = '';
        this.resetForm();
        this.loadCategories();
        this.cdr.detectChanges();
      },
      error: (response) => {
        this.message = '';
        this.error = response.error?.message ?? 'No se pudo guardar la categoria.';
        this.cdr.detectChanges();
      }
    });
  }
}
