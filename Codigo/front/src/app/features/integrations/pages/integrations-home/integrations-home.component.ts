import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { firstValueFrom } from 'rxjs';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { IntegrationOverview } from '../../models/integration.models';
import { IntegrationsService } from '../../services/integrations.service';

@Component({
  selector: 'app-integrations-home', standalone: true,
  imports: [DatePipe, RouterLink, MatButtonModule, MatIconModule, MatProgressBarModule, PageHeaderComponent],
  template: `
    <app-page-header eyebrow="Administração" title="Integrações" subtitle="Importe e acompanhe os relatórios do iFood e da 99Food." />
    <div class="notice warning"><mat-icon>info</mat-icon>As importações de pedidos e itens são independentes. Relatórios agregados de itens não criam produtos dentro de pedidos nem movimentam o caixa.</div>
    @if (loading()) { <section class="surface surface-body state"><mat-progress-bar mode="indeterminate"/><span>Carregando integrações…</span></section> }
    @else if (error()) { <div class="notice error" role="alert"><mat-icon>error</mat-icon>{{ error() }} <button mat-button (click)="load()">Tentar novamente</button></div> }
    @else if (items().length === 0) { <div class="surface surface-body state"><mat-icon>sync_problem</mat-icon><strong>Não foi possível carregar as integrações.</strong></div> }
    @else {
      <section class="integration-grid">
        @for(item of items(); track item.provider) {
          <article class="surface surface-body">
            <div class="integration-head"><span class="app-badge" [class.food99]="item.provider === 'FOOD_99'">{{ item.provider === 'IFOOD' ? 'iF' : '99' }}</span><span class="manual-status"><mat-icon>upload_file</mat-icon>Importação por arquivo</span></div>
            <h2>{{ item.provider === 'IFOOD' ? 'iFood' : '99Food' }}</h2>
            <p>Envie o relatório exportado para analisar e confirmar a importação.</p>
            <dl><div><dt>Última importação</dt><dd>{{ item.lastImportedAt ? (item.lastImportedAt | date:'dd/MM/yyyy HH:mm':'':'pt-BR') : 'Ainda não realizada' }}</dd></div><div><dt>Pedidos importados</dt><dd>{{ item.importedOrders }}</dd></div><div><dt>Linhas de itens agregados</dt><dd>{{ item.itemSnapshots }}</dd></div></dl>
            <a mat-flat-button class="primary-button" [routerLink]="item.provider === 'IFOOD' ? '/app/integracoes/ifood' : '/app/integracoes/99food'">Abrir integração <mat-icon>arrow_forward</mat-icon></a>
          </article>
        }
      </section>
    }
  `,
  styles: [`
    .integration-grid{display:grid;grid-template-columns:repeat(2,minmax(0,430px));gap:18px}.integration-head{display:flex;justify-content:space-between;align-items:center;gap:8px}.app-badge{width:48px;height:48px;border-radius:12px;background:#ea1d2c;color:#fff;display:grid;place-items:center;font-weight:800}.app-badge.food99{background:#f7d019;color:#242424}.integration-grid h2{margin:16px 0 6px;font-size:1.05rem}.integration-grid p{color:var(--muted);font-size:.8rem;line-height:1.5}.integration-grid dl div{display:flex;justify-content:space-between;gap:8px;padding:10px 0;border-bottom:1px solid var(--border);font-size:.76rem}dt{color:var(--muted)}dd{margin:0;font-weight:700;text-align:right}.integration-grid a{width:100%;margin-top:18px}.manual-status{display:inline-flex;align-items:center;gap:5px;color:var(--muted);font-size:.65rem}.manual-status mat-icon{color:var(--red);font-size:16px;width:16px;height:16px}.state{max-width:600px;display:grid;gap:10px;color:var(--muted);font-size:.75rem}.state mat-icon{color:#a8a29e}.notice{display:flex;gap:8px;align-items:flex-start;padding:11px 13px;border-radius:9px;font-size:.75rem;line-height:1.45;margin:12px 0}.notice mat-icon{font-size:18px;width:18px;height:18px}.notice.warning{background:#fff8db;color:#694d0b;border:1px solid #f3dc86}.notice.error{background:#fff0ee;color:#8f1d14;border:1px solid #f3c4bf}@media(max-width:750px){.integration-grid{grid-template-columns:1fr}}
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class IntegrationsHomeComponent implements OnInit {
  private readonly service = inject(IntegrationsService);
  readonly items = signal<IntegrationOverview[]>([]);
  readonly loading = signal(true);
  readonly error = signal('');
  ngOnInit(): void { this.load(); }
  async load(): Promise<void> {
    this.loading.set(true); this.error.set('');
    try { this.items.set(await firstValueFrom(this.service.overview())); }
    catch { this.error.set('Não foi possível carregar os dados das integrações.'); }
    finally { this.loading.set(false); }
  }
}
