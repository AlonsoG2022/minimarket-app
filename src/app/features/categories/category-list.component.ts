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

  readonly form = this.fb.nonNullable.group({
    name: ['', Validators.required],
    description: [''],
    isActive: [true]
  });

  categories: Category[] = [];
  loading = true;
  message = '';
  error = '';

  ngOnInit(): void {
    this.loadCategories();
  }

  loadCategories(forceRefresh = false): void {
    this.loading = true;
    this.categoriesService.getAll(forceRefresh).subscribe({
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

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const payload: SaveCategory = {
      name: value.name,
      description: value.description,
      isActive: value.isActive
    };

    this.categoriesService.create(payload).subscribe({
      next: () => {
        this.message = 'Categoria registrada correctamente.';
        this.error = '';
        this.form.reset({
          name: '',
          description: '',
          isActive: true
        });
        this.loadCategories(true);
        this.cdr.detectChanges();
      },
      error: (response) => {
        this.message = '';
        this.error = response.error?.message ?? 'No se pudo registrar la categoria.';
        this.cdr.detectChanges();
      }
    });
  }
}
