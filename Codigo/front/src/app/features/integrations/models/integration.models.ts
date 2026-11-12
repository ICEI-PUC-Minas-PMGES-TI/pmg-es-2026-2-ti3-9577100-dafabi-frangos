export type IntegrationProvider = 'IFOOD' | 'FOOD_99';
export type IntegrationReportType = 'ORDERS' | 'ITEMS';
export type ImportBatchStatus = 'READY' | 'IMPORTED' | 'IMPORTED_WITH_ERRORS';

export interface ImportSummary {
  records: number;
  completedOrders: number;
  canceledOrders: number;
  partiallyCanceledOrders: number;
  grossItems: number;
  customerPaid: number;
  itemRevenue: number;
  itemQuantity: number;
  ifoodPromotions: number;
  storePromotions: number;
  serviceFees: number;
  feesAndCommissions: number | null;
  netReported: number;
  duplicates: number;
  invalidRows: number;
}

export interface ImportRowPreview {
  rowNumber: number;
  externalId: string | null;
  date: string | null;
  status: string | null;
  itemName: string | null;
  quantity: number | null;
  gross: number | null;
  netReported: number | null;
  valid: boolean;
  duplicateInFile: boolean;
  duplicateInSystem: boolean;
  issues: string[];
}

export interface ImportBatch {
  id: string;
  provider: IntegrationProvider;
  reportType: IntegrationReportType;
  fileName: string;
  periodStart: string | null;
  periodEnd: string | null;
  status: ImportBatchStatus;
  rowCount: number;
  validRowCount: number;
  invalidRowCount: number;
  duplicateRowCount: number;
  summary: ImportSummary;
  rows: ImportRowPreview[];
  createdAt: string;
  importedAt: string | null;
  alreadyImported: boolean;
}

export interface MarketplaceOrder {
  id: string;
  provider: IntegrationProvider;
  externalStoreId: string;
  externalOrderId: string;
  orderAt: string;
  rawStatus: string;
  normalizedStatus: string;
  grossItems: number | null;
  customerPaid: number | null;
  feesAndCommissions: number | null;
  netReported: number | null;
  paymentMethodRaw: string | null;
  marketingCost?: number | null;
  logisticsCost?: number | null;
  refundAmount?: number | null;
  freeDeliveryCost?: number | null;
  tipAmount?: number | null;
  originalItemCount?: number | null;
  completedAt?: string | null;
  cancelledAt?: string | null;
  cancellationResponsible?: string | null;
  cancellationReason?: string | null;
}

export interface MarketplaceItemSnapshot {
  id: string;
  provider: IntegrationProvider;
  externalStoreId: string;
  date: string;
  externalItemName: string;
  revenue: number;
  quantity: number;
}

export interface IntegrationOverview {
  provider: IntegrationProvider;
  lastImportedAt: string | null;
  importedOrders: number;
  itemSnapshots: number;
}

export interface MarketplaceMappingComponent { productId: string; productName: string; unit: string; quantityPerItem: number; }
export interface MarketplaceMapping { provider: IntegrationProvider; externalStoreId: string; externalItemName: string; ignored: boolean; components: MarketplaceMappingComponent[]; }
export interface MarketplaceStockEffect {
  snapshotId: string; provider: IntegrationProvider; externalStoreId: string; businessDate: string;
  externalItemName: string; productId: string | null; productName: string | null;
  reportedQuantity: number; appliedQuantity: number; quantityToApply: number; status: string; explanation: string;
}
export interface MarketplaceStockBaseline { provider: IntegrationProvider; externalStoreId: string; startDate: string; }
export interface MarketplaceSettlement {
  id: string; provider: IntegrationProvider; externalStoreId: string; externalSettlementId: string;
  periodStart: string; periodEnd: string; grossAmount: number | null; feesAmount: number | null;
  adjustmentsAmount: number | null; retainedAmount: number | null; amountReceived: number | null;
  payoutDate: string | null; status: string; sourceReference: string | null; updatedAt: string;
}
