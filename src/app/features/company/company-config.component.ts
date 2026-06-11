import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { SaveCompany } from '../../core/models/minimarket.models';
import { CompanyService } from '../../core/services/company.service';
import { ThemeService } from '../../core/services/theme.service';

@Component({
  selector: 'app-company-config',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './company-config.component.html',
  styleUrl: './company-config.component.css'
})
export class CompanyConfigComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
  private readonly companyService = inject(CompanyService);
  private readonly themeService = inject(ThemeService);

  readonly themes = this.themeService.themes;

  readonly form = this.fb.nonNullable.group({
    businessName: ['', Validators.required],
    legalName: [''],
    taxId: ['', Validators.required],
    addressLine: [''],
    phone: [''],
    tagline: [''],
    documentTitle: [''],
    customerLabel: [''],
    footerLine1: [''],
    footerLine2: [''],
    showTicketPreview: [true],
    minimumStock: [5, [Validators.required, Validators.min(0)]],
    theme: ['orange']
  });

  loading = true;
  saving = false;
  message = '';
  error = '';

  ngOnInit(): void {
    this.companyService.get().subscribe({
      next: (company) => {
        this.form.patchValue(company);
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loading = false;
        this.error = 'No se pudo cargar la configuracion de empresa.';
        this.cdr.detectChanges();
      }
    });

    // Vista previa en vivo: aplica el tema apenas se cambia el combo.
    this.form.controls.theme.valueChanges.subscribe((theme) => this.themeService.applyTheme(theme));
  }

  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.message = '';
    this.error = '';

    const value = this.form.getRawValue();
    const payload: SaveCompany = {
      businessName: value.businessName,
      legalName: value.legalName,
      taxId: value.taxId,
      addressLine: value.addressLine,
      phone: value.phone,
      tagline: value.tagline,
      documentTitle: value.documentTitle,
      customerLabel: value.customerLabel,
      footerLine1: value.footerLine1,
      footerLine2: value.footerLine2,
      showTicketPreview: value.showTicketPreview,
      minimumStock: value.minimumStock,
      theme: value.theme
    };

    this.companyService.update(payload).subscribe({
      next: () => {
        this.saving = false;
        this.message = 'Configuracion guardada correctamente.';
        this.cdr.detectChanges();
      },
      error: () => {
        this.saving = false;
        this.error = 'No se pudo guardar la configuracion.';
        this.cdr.detectChanges();
      }
    });
  }
}
