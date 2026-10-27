export interface ApiSupplier {
  id: string;
  storeId: string;
  name: string;
  phone: string;
  email: string;
  notes?: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ApiCreateSupplierRequest {
  name: string;
  phone: string;
  email: string;
  notes?: string;
  active: boolean;
}

export type ApiUpdateSupplierRequest = ApiCreateSupplierRequest;
