import { PaymentMethod, SaleItem } from '../../../core/models/domain.models';
export interface CartState { items: SaleItem[]; total: number; }
export interface PaymentRequest { method: PaymentMethod; received?: number; }
export interface SaleFilters { origin: string; payment: string; status: string; query: string; }
