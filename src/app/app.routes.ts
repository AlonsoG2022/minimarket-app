import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';
import { roleGuard } from './core/guards/role.guard';
import { LoginComponent } from './features/auth/login.component';
import { CategoryListComponent } from './features/categories/category-list.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { ProductListComponent } from './features/products/product-list.component';
import { PurchaseListComponent } from './features/purchases/purchase-list.component';
import { ReportsComponent } from './features/reports/reports.component';
import { SalesFormComponent } from './features/sales/sales-form.component';
import { SupplierListComponent } from './features/suppliers/supplier-list.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  {
    path: '',
    component: DashboardComponent,
    pathMatch: 'full',
    canActivate: [authGuard, roleGuard],
    data: { roles: ['admin', 'cajero'] }
  },
  {
    path: 'categorias',
    component: CategoryListComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['admin'] }
  },
  {
    path: 'productos',
    component: ProductListComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['admin'] }
  },
  {
    path: 'ventas',
    component: SalesFormComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['admin', 'cajero'] }
  },
  {
    path: 'compras',
    component: PurchaseListComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['admin'] }
  },
  {
    path: 'reportes',
    component: ReportsComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['admin'] }
  },
  {
    path: 'proveedores',
    component: SupplierListComponent,
    canActivate: [authGuard, roleGuard],
    data: { roles: ['admin'] }
  },
  { path: '**', redirectTo: '' }
];
