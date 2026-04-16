export interface Category {
  id: number;
  name: string;
  description?: string | null;
  isActive: boolean;
}

export interface Product {
  id: number;
  name: string;
  sku: string;
  description?: string | null;
  price: number;
  stock: number;
  minimumStock: number;
  isActive: boolean;
  categoryId: number;
  categoryName: string;
}

export interface SaveProduct {
  name: string;
  sku: string;
  description?: string | null;
  price: number;
  stock: number;
  minimumStock: number;
  isActive: boolean;
  categoryId: number;
}

export interface User {
  id: number;
  fullName: string;
  username: string;
  role: string;
  isActive: boolean;
}

export interface SaleDetail {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface Sale {
  id: number;
  saleDate: string;
  userId: number;
  userName: string;
  paymentMethod: string;
  total: number;
  notes?: string | null;
  details: SaleDetail[];
}

export interface CreateSale {
  userId: number;
  paymentMethod: string;
  notes?: string | null;
  details: Array<{ productId: number; quantity: number }>;
}

export interface SalesSummary {
  date: string;
  totalAmount: number;
  saleCount: number;
}

export interface TopSellingProduct {
  productId: number;
  productName: string;
  sku: string;
  totalQuantity: number;
  totalAmount: number;
}

export interface DashboardSummary {
  todaySales: number;
  todayTransactions: number;
  productCount: number;
  lowStockProducts: number;
}
