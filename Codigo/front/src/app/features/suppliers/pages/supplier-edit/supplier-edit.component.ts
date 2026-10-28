import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiError } from '../../../../core/http/api-error';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../../../shared/components/error-state/error-state.component';
import { LoadingStateComponent } from '../../../../shared/components/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { SupplierFormComponent } from '../../components/supplier-form/supplier-form.component';
import { SupplierFormValue } from '../../models/supplier.models';
import { SuppliersService } from '../../services/suppliers.service';

@Component({ selector: 'app-supplier-edit', standalone: true, imports: [PageHeaderComponent, SupplierFormComponent, EmptyStateComponent, ErrorStateComponent, LoadingStateComponent], template: `<app-page-header eyebrow="Fornecedores" title="Editar fornecedor" subtitle="Atualize os dados de contato e status."/>@if(loading()){<app-loading-state/>}@else if(error()){<app-error-state title="Não foi possível carregar o fornecedor" [message]="error()!"/>}@else if(supplier()){<app-supplier-form [supplier]="supplier()!" [saving]="saving()" (saved)="save($event)" (cancel)="router.navigate(['/app/fornecedores'])"/>}@else{<app-empty-state icon="search_off" title="Fornecedor não encontrado" message="O cadastro solicitado não existe."/>}`, changeDetection: ChangeDetectionStrategy.OnPush })
export class SupplierEditComponent {
  readonly router = inject(Router); private readonly route = inject(ActivatedRoute); private readonly service = inject(SuppliersService); private readonly snack = inject(MatSnackBar);
  readonly supplier = signal<Awaited<ReturnType<SuppliersService['find']>> | null>(null); readonly loading = signal(true); readonly saving = signal(false); readonly error = signal<string | null>(null);
  constructor() { void this.load(); }
  async load(): Promise<void> {
    const id = this.route.snapshot.paramMap.get('id');
    if (!id) { this.error.set('Identificador do fornecedor inválido.'); this.loading.set(false); return; }
    try { this.supplier.set(await this.service.find(id)); }
    catch (error) { this.error.set(error instanceof ApiError ? error.message : 'Tente novamente em alguns instantes.'); }
    finally { this.loading.set(false); }
  }
  async save(value: SupplierFormValue): Promise<void> {
    const supplier = this.supplier(); if (!supplier) return; this.saving.set(true);
    try { await this.service.save(value, supplier.id); this.snack.open('Fornecedor atualizado com sucesso.', 'Fechar', { duration: 3500 }); await this.router.navigate(['/app/fornecedores']); }
    catch (error) { this.snack.open(error instanceof ApiError ? error.message : 'Não foi possível editar o fornecedor.', 'Fechar', { duration: 4500 }); }
    finally { this.saving.set(false); }
  }
}
