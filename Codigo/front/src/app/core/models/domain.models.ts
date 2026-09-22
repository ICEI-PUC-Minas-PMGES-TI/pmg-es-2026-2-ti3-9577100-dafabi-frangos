export type Role = 'ADMIN' | 'CASHIER';
export type SaleOrigin = 'COUNTER' | 'IFOOD' | '99FOOD';
export type PaymentMethod = 'CASH' | 'PIX' | 'DEBIT' | 'CREDIT';
export type SaleStatus = 'OPEN' | 'COMPLETED' | 'CANCELED' | 'REFUNDED';
export type ProductStatus = 'ACTIVE' | 'INACTIVE';

export interface User { id: string; name: string; email: string; role: Role; active: boolean; passwordHash?: string; }
export interface Category { id: string; name: string; }
export interface Product { id: string; name: string; category: string; categoryId?: string; price: number; cost: number; stock: number; unit: string; barcode?: string; perishable: boolean; batch?: string; expiry?: string; status: ProductStatus; frequent?: boolean; version?: number; }
export interface SaleItem { productId: string; productName: string; unitPrice: number; quantity: number; subtotal: number; }
export interface Payment { method: PaymentMethod; amount: number; received?: number; change?: number; }
export interface Sale { id: string; externalId?: string; number: string; createdAt: string; operator: string; origin: SaleOrigin; payment: Payment; items: SaleItem[]; gross: number; discount: number; fees: number; net: number; status: SaleStatus; history: string[]; }
export interface CashRegister { id: string; operator: string; openedAt: string; closedAt?: string; initialBalance: number; expectedBalance: number; countedBalance?: number; difference?: number; justification?: string; status: 'OPEN' | 'CLOSED'; }
export interface CashMovement { id: string; date: string; description: string; type: 'IN' | 'OUT'; amount: number; origin: string; referenceId?: string; }
export interface Supplier { id: string; name: string; phone: string; email: string; notes?: string; active: boolean; }
export interface PurchaseItem { productId: string; productName: string; quantity: number; unitCost: number; total: number; }
export interface Purchase { id: string; supplierId: string; supplierName: string; date: string; items: PurchaseItem[]; total: number; status: 'REGISTERED' | 'CANCELED'; }
export interface Expense { id: string; description: string; category: string; amount: number; date: string; paymentMethod: PaymentMethod; notes?: string; status: 'PENDING' | 'CONSOLIDATED' | 'CANCELED'; }
export interface StockMovement { id: string; productId: string; productName: string; date: string; previousQuantity: number; newQuantity: number; difference: number; origin: string; user: string; reason: string; }
export interface PerishableProduct { id: string; productId: string; productName: string; batch: string; receivedAt: string; expiry: string; quantity: number; status: 'VALID' | 'EXPIRED' | 'USED'; }
export interface Integration { id: 'IFOOD' | '99FOOD'; name: string; status: 'CONNECTED' | 'UNAVAILABLE' | 'PENDING'; lastSync?: string; importedOrders: number; }
export interface ImportedSale { externalId: string; origin: Exclude<SaleOrigin, 'COUNTER'>; gross: number; discounts: number; fees: number; cancellations: number; net: number; importedAt: string; }
export interface Refund { id: string; saleId: string; saleNumber: string; date: string; user: string; reason: string; amount: number; paymentMethod: PaymentMethod; status: 'COMPLETED' | 'CANCELED'; }
export interface DashboardSummary { revenue: number; expenses: number; ifoodFees: number; food99Fees: number; estimatedResult: number; salesCount: number; counterSales: number; ifoodSales: number; food99Sales: number; }
