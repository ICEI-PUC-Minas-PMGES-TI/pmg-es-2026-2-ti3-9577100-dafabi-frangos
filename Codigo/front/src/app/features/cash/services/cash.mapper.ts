import { CashMovement, CashRegister } from '../../../core/models/domain.models';
import { ApiCashMovement, ApiCashRegister } from '../models/cash-api.models';

export function toCashRegister(api: ApiCashRegister): CashRegister {
  return {
    id: api.id,
    operator: api.operator,
    openedAt: api.openedAt,
    closedAt: api.closedAt ?? undefined,
    initialBalance: Number(api.initialBalance),
    expectedBalance: Number(api.expectedBalanceAtClose ?? api.expectedBalance),
    countedBalance: api.countedBalance == null ? undefined : Number(api.countedBalance),
    difference: api.difference == null ? undefined : Number(api.difference),
    justification: api.justification ?? undefined,
    status: api.status
  };
}

export function toCashMovement(api: ApiCashMovement): CashMovement {
  return {
    id: api.id,
    date: api.date || api.occurredAt,
    description: api.description,
    type: api.direction,
    amount: Number(api.amount),
    origin: api.origin ?? api.movementType,
    referenceId: api.referenceId ?? undefined
  };
}
