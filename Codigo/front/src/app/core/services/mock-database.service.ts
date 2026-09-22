import { Injectable, signal } from '@angular/core';
import { CashMovement, CashRegister, Expense, Integration, PerishableProduct, Product, Refund, Sale, StockMovement, Supplier } from '../models/domain.models';

@Injectable({ providedIn: 'root' })
export class MockDatabaseService {
  readonly cashRegister = signal<CashRegister | null>({
    id: 'cx-1509', operator: 'Carlos Oliveira', openedAt: '2026-09-15T08:02:00',
    initialBalance: 150, expectedBalance: 192.9, status: 'OPEN'
  });
  readonly products = signal<Product[]>([
    { id: 'p1', name: 'Frango assado tradicional', category: 'Frangos', price: 42.9, cost: 24.5, stock: 18, unit: 'un', barcode: '7891000101010', perishable: true, batch: 'FA-0915', expiry: '2026-09-16', status: 'ACTIVE', frequent: true },
    { id: 'p2', name: 'Marmita executiva', category: 'Marmitas', price: 24.9, cost: 12.2, stock: 26, unit: 'un', perishable: true, batch: 'ME-0915', expiry: '2026-09-15', status: 'ACTIVE', frequent: true },
    { id: 'p3', name: 'Meio frango assado', category: 'Frangos', price: 23.9, cost: 13.5, stock: 12, unit: 'un', perishable: true, status: 'ACTIVE', frequent: true },
    { id: 'p4', name: 'Farofa da casa 300 g', category: 'Complementos', price: 9.9, cost: 4.1, stock: 34, unit: 'pct', barcode: '7891000202020', perishable: false, status: 'ACTIVE', frequent: true },
    { id: 'p5', name: 'Refrigerante 2 L', category: 'Bebidas', price: 13.5, cost: 8.2, stock: 20, unit: 'un', barcode: '7894900011517', perishable: false, status: 'ACTIVE', frequent: true },
    { id: 'p6', name: 'Maionese caseira 250 g', category: 'Complementos', price: 11.9, cost: 5.8, stock: 0, unit: 'pote', perishable: true, status: 'ACTIVE', frequent: false },
    { id: 'p7', name: 'Suco de laranja 500 ml', category: 'Bebidas', price: 8.5, cost: 4.2, stock: 15, unit: 'un', barcode: '7891000303030', perishable: true, status: 'INACTIVE' }
  ]);

  readonly sales = signal<Sale[]>([
    { id: 'v1048', number: '#1048', createdAt: '2026-09-15T12:42:00', operator: 'Carlos Oliveira', origin: 'COUNTER', payment: { method: 'PIX', amount: 66.8 }, items: [{ productId: 'p1', productName: 'Frango assado tradicional', unitPrice: 42.9, quantity: 1, subtotal: 42.9 }, { productId: 'p3', productName: 'Meio frango assado', unitPrice: 23.9, quantity: 1, subtotal: 23.9 }], gross: 66.8, discount: 0, fees: 0, net: 66.8, status: 'COMPLETED', history: ['Venda criada no PDV', 'Pagamento confirmado via Pix'] },
    { id: 'v1047', number: '#1047', createdAt: '2026-09-15T12:18:00', operator: 'Fabiana Nogueira', origin: 'IFOOD', payment: { method: 'CREDIT', amount: 54.3 }, items: [{ productId: 'p2', productName: 'Marmita executiva', unitPrice: 24.9, quantity: 2, subtotal: 49.8 }], gross: 54.3, discount: 4.5, fees: 10.86, net: 38.94, status: 'COMPLETED', history: ['Pedido importado do iFood', 'Estoque atualizado'] },
    { id: 'v1046', number: '#1046', createdAt: '2026-09-15T11:55:00', operator: 'Integração 99Food', origin: '99FOOD', payment: { method: 'CREDIT', amount: 37.4 }, items: [{ productId: 'p2', productName: 'Marmita executiva', unitPrice: 24.9, quantity: 1, subtotal: 24.9 }, { productId: 'p5', productName: 'Refrigerante 2 L', unitPrice: 12.5, quantity: 1, subtotal: 12.5 }], gross: 37.4, discount: 0, fees: 6.73, net: 30.67, status: 'COMPLETED', history: ['Pedido importado da 99Food'] },
    { id: 'v1045', number: '#1045', createdAt: '2026-09-15T11:31:00', operator: 'Carlos Oliveira', origin: 'COUNTER', payment: { method: 'CASH', amount: 42.9, received: 50, change: 7.1 }, items: [{ productId: 'p1', productName: 'Frango assado tradicional', unitPrice: 42.9, quantity: 1, subtotal: 42.9 }], gross: 42.9, discount: 0, fees: 0, net: 42.9, status: 'COMPLETED', history: ['Venda criada no PDV', 'Pagamento em dinheiro confirmado'] }
  ]);

  readonly suppliers = signal<Supplier[]>([
    { id: 'f1', name: 'Granja Santa Clara', phone: '(31) 99812-4400', email: 'contato@granjasantaclara.com.br', notes: 'Entregas às terças e sextas.', active: true },
    { id: 'f2', name: 'Distribuidora Bom Sabor', phone: '(31) 3344-7788', email: 'vendas@bomsabor.com.br', active: true }
  ]);
  readonly expenses = signal<Expense[]>([
    { id: 'd1', description: 'Gás de cozinha', category: 'Operacional', amount: 128, date: '2026-09-14', paymentMethod: 'PIX', status: 'CONSOLIDATED' },
    { id: 'd2', description: 'Embalagens para marmita', category: 'Materiais', amount: 186.5, date: '2026-09-13', paymentMethod: 'DEBIT', status: 'CONSOLIDATED' },
    { id: 'd3', description: 'Manutenção da geladeira', category: 'Serviços', amount: 95, date: '2026-09-15', paymentMethod: 'PIX', notes: 'Aguardando confirmação do serviço.', status: 'PENDING' }
  ]);
  readonly movements = signal<StockMovement[]>([]);
  readonly perishables = signal<PerishableProduct[]>([
    { id: 'l1', productId: 'p1', productName: 'Frango assado tradicional', batch: 'FA-0915', receivedAt: '2026-09-15', expiry: '2026-09-16', quantity: 18, status: 'VALID' },
    { id: 'l2', productId: 'p2', productName: 'Marmita executiva', batch: 'ME-0915', receivedAt: '2026-09-15', expiry: '2026-09-15', quantity: 26, status: 'VALID' }
  ]);
  readonly integrations = signal<Integration[]>([
    { id: 'IFOOD', name: 'iFood', status: 'CONNECTED', lastSync: '2026-09-15T12:35:00', importedOrders: 8 },
    { id: '99FOOD', name: '99Food', status: 'CONNECTED', lastSync: '2026-09-15T11:20:00', importedOrders: 5 }
  ]);
  readonly refunds = signal<Refund[]>([
    { id: 'e1', saleId: 'v1039', saleNumber: '#1039', date: '2026-09-14T18:20:00', user: 'Fabiana Nogueira', reason: 'Cobrança duplicada no cartão', amount: 24.9, paymentMethod: 'CREDIT', status: 'COMPLETED' }
  ]);
  readonly cashMovements = signal<CashMovement[]>([
    { id: 'm2', date: '2026-09-15T11:31:00', description: 'Venda #1045', type: 'IN', amount: 42.9, origin: 'Dinheiro' }
  ]);
}
