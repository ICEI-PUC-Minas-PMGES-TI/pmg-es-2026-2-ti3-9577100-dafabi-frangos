import { Supplier } from '../../../core/models/domain.models';
import { SupplierFormValue } from '../models/supplier.models';
import { ApiCreateSupplierRequest, ApiSupplier, ApiUpdateSupplierRequest } from '../models/supplier-api.models';

export function toSupplier(api: ApiSupplier): Supplier {
  return { id: api.id, name: api.name, phone: api.phone, email: api.email, notes: api.notes ?? undefined, active: api.active };
}
export function toSupplierRequest(value: SupplierFormValue): ApiCreateSupplierRequest | ApiUpdateSupplierRequest {
  return { name: value.name.trim(), phone: value.phone.trim(), email: value.email.trim(), notes: value.notes?.trim() || undefined, active: value.active };
}
