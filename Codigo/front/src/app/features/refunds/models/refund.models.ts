import { PaymentMethod, SaleOrigin } from '../../../core/models/domain.models';

export interface RefundRequest {
  saleId: string;
  reason: string;
}

export interface RefundSaleSummary {
  id: string;
  number: string;
  createdAt: string;
  origin: SaleOrigin;
  paymentMethod: PaymentMethod;
  total: number;
  cashAmountToReverse: number;
  eligible: boolean;
  ineligibilityReason?: string | null;
  items: Array<{ productId: string; productName: string; quantity: number; unitPrice: number; subtotal: number }>;
}

export interface ApiRefund {
  id: string;
  saleId: string;
  saleNumber: number;
  saleStatus: string;
  paymentMethod: PaymentMethod;
  amount: number;
  cashAmount: number;
  cashRegisterId: string | null;
  reason: string;
  refundedById: string;
  refundedBy: string;
  refundedAt: string;
}

export interface ApiRefundEligibility {
  saleId: string;
  saleNumber: number;
  dateTime: string;
  origin: 'COUNTER' | 'IFOOD' | 'FOOD_99';
  paymentMethod: PaymentMethod;
  status: string;
  total: number;
  cashAmountToReverse: number;
  eligible: boolean;
  ineligibilityCode: string | null;
  ineligibilityReason: string | null;
  items: RefundSaleSummary['items'];
  refund: ApiRefund | null;
}
