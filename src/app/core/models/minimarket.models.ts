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
  expirationDate?: string | null;
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
  expirationDate?: string | null;
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
  barcode?: string | null;
  description?: string | null;
  salesUnitName?: string | null;
  purchaseUnitName?: string | null;
  unitsPerPurchaseUnit?: number;
  stock: number;
  expirationDate?: string | null;
  isActive?: boolean;
}

export interface ProductImportError {
  rowNumber: number;
  message: string;
}

export interface ProductImportResult {
  createdCount: number;
  errors: ProductImportError[];
}

export interface DeleteProductResult {
  message: string;
  deactivated: boolean;
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
  cashSessionId?: number | null;
  printStatus?: string | null;
  lastPrintJobId?: number | null;
  paymentMethod: string;
  subTotal: number;
  igv: number;
  total: number;
  notes?: string | null;
  details: SaleDetail[];
}

export interface PrintJob {
  id: number;
  saleId?: number | null;
  sourceType: string;
  documentType: string;
  status: string;
  attempts: number;
  printerName?: string | null;
  requestedAt: string;
  startedAt?: string | null;
  processedAt?: string | null;
  lastError?: string | null;
}

export interface CreateSale {
  userId: number;
  cashSessionId?: number | null;
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
  subTotal: number;
  igv: number;
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

export interface Company {
  id: number;
  businessName: string;
  legalName: string;
  taxId: string;
  addressLine: string;
  phone: string;
  tagline: string;
  documentTitle: string;
  customerLabel: string;
  footerLine1: string;
  footerLine2: string;
  showTicketPreview: boolean;
  minimumStock: number;
  theme: string;
}

export interface SaveCompany {
  businessName: string;
  legalName: string;
  taxId: string;
  addressLine: string;
  phone: string;
  tagline: string;
  documentTitle: string;
  customerLabel: string;
  footerLine1: string;
  footerLine2: string;
  showTicketPreview: boolean;
  minimumStock: number;
  theme: string;
}

export interface CashMovement {
  id: number;
  movementDate: string;
  type: string;
  amount: number;
  description?: string | null;
  referenceType?: string | null;
  referenceId?: number | null;
}

export interface CashSession {
  id: number;
  userId: number;
  userName: string;
  openedAt: string;
  closedAt?: string | null;
  openingAmount: number;
  closingExpectedAmount?: number | null;
  closingCountedAmount?: number | null;
  difference?: number | null;
  status: string;
  notes?: string | null;
  currentAmount: number;
  movements: CashMovement[];
}

export interface OpenCashSession {
  userId: number;
  openingAmount: number;
  notes?: string | null;
}

export interface CreateCashMovement {
  userId: number;
  type: string;
  amount: number;
  description?: string | null;
}

export interface CloseCashSession {
  userId: number;
  countedAmount: number;
  notes?: string | null;
}
