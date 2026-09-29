export type ApiCashStatus = 'OPEN' | 'CLOSED';
export type ApiCashDirection = 'IN' | 'OUT';

export interface ApiCashRegister {
  id: string;
  storeId: string;
  operatorId: string | null;
  operator: string;
  openedAt: string;
  closedAt: string | null;
  initialBalance: number;
  expectedBalance: number;
  expectedBalanceAtClose: number | null;
  countedBalance: number | null;
  difference: number | null;
  justification: string | null;
  status: ApiCashStatus;
}

export interface ApiCashSummary {
  id: string;
  storeId: string;
  operator: string;
  status: ApiCashStatus;
  openedAt: string;
  closedAt: string | null;
  initialBalance: number;
  cashSales: number;
  cashExpenses: number;
  cashRefunds: number;
  expectedBalance: number;
  countedBalance: number | null;
  difference: number | null;
  justification: string | null;
}

export interface ApiCashMovement {
  id: string;
  cashRegisterId: string;
  direction: ApiCashDirection;
  type: string;
  movementType: string;
  amount: number;
  origin: string | null;
  description: string;
  referenceType: string | null;
  referenceId: string | null;
  occurredAt: string;
  date: string;
}

export interface ApiOpenCashRequest {
  initialBalance: number;
  storeId: string;
  operatorId: string;
  operator: string;
}

export interface ApiCloseCashRequest {
  countedBalance: number;
  justification?: string;
}
