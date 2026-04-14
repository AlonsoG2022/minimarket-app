import { Routes } from '@angular/router';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { ProductListComponent } from './features/products/product-list.component';
import { ReportsComponent } from './features/reports/reports.component';
import { SalesFormComponent } from './features/sales/sales-form.component';

export const routes: Routes = [
  { path: '', component: DashboardComponent, pathMatch: 'full' },
  { path: 'productos', component: ProductListComponent },
  { path: 'ventas', component: SalesFormComponent },
  { path: 'reportes', component: ReportsComponent },
  { path: '**', redirectTo: '' }
];
