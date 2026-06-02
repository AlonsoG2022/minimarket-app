import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { SaveSupplier, Supplier } from '../../core/models/minimarket.models';
import { SuppliersService } from '../../core/services/suppliers.service';

@Component({
  selector: 'app-supplier-list',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './supplier-list.component.html',
  styleUrl: './supplier-list.component.css'
})
export class SupplierListComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly suppliersService = inject(SuppliersService);

  readonly form = this.fb.nonNullable.group({
    id: [0],
    name: ['', Validators.required],
    documentNumber: [''],
    contactName: [''],
    phone: [''],
    email: [''],
    address: [''],
    notes: [''],
    isActive: [true]
  });

  suppliers: Supplier[] = [];
  loading = true;
  isEditing = false;
  message = '';
  error = '';

  ngOnInit(): void {
    this.loadSuppliers();
  }

  loadSuppliers(forceRefresh = false): void {
    this.loading = true;
    this.suppliersService.getAll(forceRefresh).subscribe({
      next: (suppliers) => {
        this.suppliers = suppliers;
        this.loading = false;
        this.error = '';
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.error = 'No se pudieron cargar los proveedores.';
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
    const payload: SaveSupplier = {
      name: value.name,
      documentNumber: value.documentNumber || null,
      contactName: value.contactName || null,
      phone: value.phone || null,
      email: value.email || null,
      address: value.address || null,
      notes: value.notes || null,
      isActive: value.isActive
    };

    const request = this.isEditing
      ? this.suppliersService.update(value.id, payload)
      : this.suppliersService.create(payload);

    request.subscribe({
      next: () => {
        this.message = this.isEditing ? 'Proveedor actualizado correctamente.' : 'Proveedor registrado correctamente.';
        this.error = '';
        this.resetForm();
        this.loadSuppliers(true);
        this.cdr.detectChanges();
      },
      error: (response) => {
        this.message = '';
        this.error = response.error?.message ?? 'No se pudo guardar el proveedor.';
        this.cdr.detectChanges();
      }
    });
  }

  edit(supplier: Supplier): void {
    this.isEditing = true;
    this.message = '';
    this.error = '';
    this.form.patchValue({
      id: supplier.id,
      name: supplier.name,
      documentNumber: supplier.documentNumber ?? '',
      contactName: supplier.contactName ?? '',
      phone: supplier.phone ?? '',
      email: supplier.email ?? '',
      address: supplier.address ?? '',
      notes: supplier.notes ?? '',
      isActive: supplier.isActive
    });
  }

  remove(supplier: Supplier): void {
    if (!confirm(`Eliminar proveedor ${supplier.name}?`)) {
      return;
    }

    this.suppliersService.delete(supplier.id).subscribe({
      next: () => {
        this.message = 'Proveedor eliminado.';
        this.error = '';
        this.loadSuppliers(true);
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudo eliminar el proveedor.';
        this.cdr.detectChanges();
      }
    });
  }

  resetForm(): void {
    this.isEditing = false;
    this.form.reset({
      id: 0,
      name: '',
      documentNumber: '',
      contactName: '',
      phone: '',
      email: '',
      address: '',
      notes: '',
      isActive: true
    });
  }
}
