import { Pipe, PipeTransform } from '@angular/core';

@Pipe({
  name: 'solesPrice',
  standalone: true
})
export class SolesPricePipe implements PipeTransform {
  transform(value: number | string | null | undefined): string {
    const numericValue = typeof value === 'number' ? value : Number(value);

    if (!Number.isFinite(numericValue)) {
      return '0 S/';
    }

    const formatted = new Intl.NumberFormat('es-PE', {
      minimumFractionDigits: 0,
      maximumFractionDigits: 2
    }).format(numericValue);

    return `${formatted} S/`;
  }
}
