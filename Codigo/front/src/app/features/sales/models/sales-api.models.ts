import { PaymentMethod, SaleOrigin, SaleStatus } from '../../../core/models/domain.models';
import { SpringPage } from '../../products/models/product-api.models';

export interface ApiSaleItem {
  id: string;
  productId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface ApiSale {
  id: string;
  saleNumber: number;
  cashRegisterId: string | null;
  operatorId: string;
  dateTime: string;
  origin: 'COUNTER' | 'IFOOD' | 'FOOD_99';
  paymentMethod: PaymentMethod;
  subtotal: number;
  total: number;
  status: 'COMPLETED' | 'CANCELLED' | 'REFUNDED';
  items: ApiSaleItem[];
  amountReceived?: number | null;
  change?: number | null;
}

export interface ApiCreateSaleRequest {
  items: Array<{ productId: string; quantity: number }>;
  paymentMethod: PaymentMethod;
  amountReceived?: number;
  storeId: string;
  operatorId: string;
}

export interface SaleQuery {
  query?: string;
  startDate?: string;
  endDate?: string;
  origin?: SaleOrigin | 'ALL';
  paymentMethod?: PaymentMethod | 'ALL';
  status?: SaleStatus | 'ALL';
  page?: number;
  size?: number;
}

export type ApiSalePage = SpringPage<ApiSale>;
