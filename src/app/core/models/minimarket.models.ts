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
  barcode?: string | null;
  purchaseBarcode?: string | null;
  description?: string | null;
  price: number;
  cost: number;
  stock: number;
  minimumStock: number;
  salesUnitName: string;
  purchaseUnitName: string;
  unitsPerPurchaseUnit: number;
  isActive: boolean;
  categoryId: number;
  categoryName: string;
}

export interface SaveProduct {
  name: string;
  barcode?: string | null;
  purchaseBarcode?: string | null;
  description?: string | null;
  price: number;
  salesUnitName: string;
  purchaseUnitName: string;
  unitsPerPurchaseUnit: number;
  stock: number;
  minimumStock: number;
  isActive: boolean;
  categoryId: number;
}

export interface ProductImportRow {
  rowNumber: number;
  name: string;
  price: number;
  categoryName: string;
  stock: number;
}

export interface ProductImportError {
  rowNumber: number;
  message: string;
}

export interface ProductImportResult {
  createdCount: number;
  errors: ProductImportError[];
}

export interface SaveCategory {
  name: string;
  description?: string | null;
  isActive: boolean;
}

export interface Supplier {
  id: number;
  name: string;
  documentNumber?: string | null;
  contactName?: string | null;
  phone?: string | null;
  email?: string | null;
  address?: string | null;
  notes?: string | null;
  isActive: boolean;
}

export interface SaveSupplier {
  name: string;
  documentNumber?: string | null;
  contactName?: string | null;
  phone?: string | null;
  email?: string | null;
  address?: string | null;
  notes?: string | null;
  isActive: boolean;
}

export interface User {
  id: number;
  fullName: string;
  username: string;
  role: string;
  isActive: boolean;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthSession {
  id: number;
  fullName: string;
  username: string;
  role: string;
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

export interface PurchaseDetail {
  id: number;
  productId: number;
  productName: string;
  packageQuantity: number;
  unitsPerPackage: number;
  totalUnits: number;
  packageCost: number;
  unitCost: number;
  subtotal: number;
  purchaseUnitName: string;
  barcodeSnapshot?: string | null;
}

export interface Purchase {
  id: number;
  purchaseDate: string;
  supplierId: number;
  supplierName: string;
  userId: number;
  userName: string;
  invoiceNumber?: string | null;
  notes?: string | null;
  total: number;
  details: PurchaseDetail[];
}

export interface CreatePurchase {
  supplierId: number;
  userId: number;
  invoiceNumber?: string | null;
  notes?: string | null;
  details: Array<{
    productId: number;
    packageQuantity: number;
    unitsPerPackage: number;
    packageCost: number;
    purchaseUnitName?: string | null;
    barcodeSnapshot?: string | null;
  }>;
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
