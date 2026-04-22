import { Routes } from '@angular/router';
import { CategoryListComponent } from './features/categories/category-list.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { ProductListComponent } from './features/products/product-list.component';
import { ReportsComponent } from './features/reports/reports.component';
import { SalesFormComponent } from './features/sales/sales-form.component';

export const routes: Routes = [
  { path: '', component: DashboardComponent, pathMatch: 'full' },
  { path: 'categorias', component: CategoryListComponent },
  { path: 'productos', component: ProductListComponent },
  { path: 'ventas', component: SalesFormComponent },
  { path: 'reportes', component: ReportsComponent },
  { path: '**', redirectTo: '' }
];
