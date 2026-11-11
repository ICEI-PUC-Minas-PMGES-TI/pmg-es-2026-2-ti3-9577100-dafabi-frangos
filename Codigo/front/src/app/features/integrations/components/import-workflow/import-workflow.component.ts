import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { firstValueFrom } from 'rxjs';
import { ImportBatch, IntegrationProvider } from '../../models/integration.models';
import { IntegrationsService } from '../../services/integrations.service';

@Component({
  selector: 'app-import-workflow',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, ReactiveFormsModule, MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    <section class="surface surface-body import-card" aria-labelledby="manual-import-title">
      <div class="section-heading">
        <div>
          <span class="eyebrow">Importação manual</span>
          <h2 id="manual-import-title">Enviar relatório</h2>
          <p>Selecione vários relatórios; cada arquivo será analisado e confirmado separadamente.</p>
        </div>
        <mat-icon aria-hidden="true">upload_file</mat-icon>
      </div>

      <input #fileInput class="visually-hidden" type="file" accept=".xlsx,.zip" multiple
        aria-label="Selecionar vários relatórios XLSX ou ZIP" (change)="selectFile($event)">
      <button type="button" class="drop-zone" (click)="fileInput.click()" [disabled]="uploading() || confirming()">
        <mat-icon>cloud_upload</mat-icon>
        <strong>{{ selectedFiles().length ? selectedFileNames() : 'Escolha os arquivos dos relatórios' }}</strong>
        <span>{{ provider() === 'FOOD_99' ? 'Aceita relatórios de pedidos e itens em ZIP ou XLSX' : 'Aceita relatórios de pedidos e cardápio em XLSX' }} · até 10 MB</span>
        <span class="choose-label">Selecionar arquivos</span>
      </button>

      @if (fileError()) {
        <div class="notice error" role="alert"><mat-icon>error</mat-icon>{{ fileError() }}</div>
      }
      @if (uploading()) {
        <div class="loading-line" role="status" aria-live="polite">
          <mat-progress-bar mode="indeterminate" />
          <span>Lendo o arquivo {{ fileIndex() + 1 }} de {{ selectedFiles().length }}…</span>
        </div>
      }
      @if (fileControl.value && !batch() && !uploading() && !fileError()) {
        <div class="file-actions">
          <button mat-button type="button" (click)="clearFile()">Remover arquivo</button>
          <button mat-flat-button type="button" class="primary-button" (click)="analyze()">
            <mat-icon>fact_check</mat-icon>Analisar relatório
          </button>
        </div>
      }

      @if (errorMessage()) {
        <div class="notice error" role="alert"><mat-icon>error</mat-icon>{{ errorMessage() }}</div>
      }

      @if (batch(); as current) {
        <div class="preview-heading">
          <div><h3>Prévia da importação</h3><p>{{ current.fileName }}</p></div>
          <span class="type-chip">Arquivo {{ fileIndex() + 1 }} de {{ selectedFiles().length }} · {{ current.reportType === 'ORDERS' ? 'Dados por pedido' : 'Vendas agregadas por item' }}</span>
        </div>
        <div class="period-note">
          Período encontrado: <strong>{{ current.periodStart ? (current.periodStart | date:'dd/MM/yyyy':'':'pt-BR') : 'não informado' }}</strong>
          @if (current.periodEnd && current.periodEnd !== current.periodStart) { até <strong>{{ current.periodEnd | date:'dd/MM/yyyy':'':'pt-BR' }}</strong> }
        </div>

        @if (current.reportType === 'ORDERS') {
          <div class="notice warning" role="status"><mat-icon>info</mat-icon>Totais abaixo reproduzem os campos disponíveis do arquivo e podem incluir cancelamentos. Campos não informados não representam necessariamente zero. Na 99Food, comissão e processamento não incluem logística nem marketing. Esta importação não movimenta o caixa nem o estoque.</div>
        }
        <div class="summary-grid">
          @if (current.reportType === 'ORDERS') {
            <div><small>Pedidos lidos</small><strong>{{ current.summary.records }}</strong></div>
            <div><small>Concluídos</small><strong>{{ current.summary.completedOrders }}</strong></div>
            <div><small>Cancelados / parciais</small><strong>{{ current.summary.canceledOrders }} / {{ current.summary.partiallyCanceledOrders }}</strong></div>
            <div><small>Valor dos itens</small><strong>{{ current.summary.grossItems | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong></div>
            <div><small>Pago pelo cliente</small><strong>{{ current.provider === 'FOOD_99' ? 'Não informado' : (current.summary.customerPaid | currency:'BRL':'symbol':'1.2-2':'pt-BR') }}</strong></div>
            <div class="highlight"><small>Líquido informado pelo portal</small><strong>{{ current.summary.netReported | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong></div>
            @if (current.provider === 'IFOOD') {
              <div><small>Promoções do iFood</small><strong>{{ current.summary.ifoodPromotions | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong></div>
              <div><small>Promoções da loja</small><strong>{{ current.summary.storePromotions | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong></div>
              <div><small>Taxas e comissões</small><strong>{{ current.summary.feesAndCommissions === null ? 'Não informado integralmente' : (current.summary.feesAndCommissions | currency:'BRL':'symbol':'1.2-2':'pt-BR') }}</strong></div>
            } @else {
              <div><small>Comissão e processamento (quando informados)</small><strong>{{ current.summary.feesAndCommissions === null ? 'Não informado integralmente' : (current.summary.feesAndCommissions | currency:'BRL':'symbol':'1.2-2':'pt-BR') }}</strong></div>
            }
          } @else {
            <div><small>Linhas de item</small><strong>{{ current.summary.records }}</strong></div>
            <div><small>Quantidade vendida</small><strong>{{ current.summary.itemQuantity }}</strong></div>
            <div class="highlight"><small>Receita dos itens</small><strong>{{ current.summary.itemRevenue | currency:'BRL':'symbol':'1.2-2':'pt-BR' }}</strong></div>
          }
          <div><small>Repetidos</small><strong>{{ current.duplicateRowCount }}</strong></div>
          <div><small>Linhas inválidas</small><strong>{{ current.invalidRowCount }}</strong></div>
        </div>

        @if (current.reportType === 'ORDERS' && (current.summary.canceledOrders || current.summary.partiallyCanceledOrders)) {
          <div class="notice warning" role="status"><mat-icon>info</mat-icon>Os valores dos pedidos cancelados são mantidos como informados pelo portal. Os totais desta prévia incluem os registros válidos; não representam faturamento concluído nem entrada no caixa.</div>
        }
        @if (current.reportType === 'ITEMS') {
          <div class="notice warning" role="status"><mat-icon>inventory_2</mat-icon>Este relatório é agregado por item e dia, sem ID de pedido. A importação não vai baixar estoque nem criar itens em vendas individuais.</div>
        }
        @if (current.alreadyImported) {
          <div class="notice success" role="status"><mat-icon>verified</mat-icon>Este arquivo já foi importado. Nenhum registro será duplicado.</div>
        }
        @if (current.invalidRowCount > 0) {
          <div class="notice warning" role="status"><mat-icon>warning</mat-icon>{{ current.invalidRowCount }} linha(s) inválida(s) ficarão fora desta importação.</div>
        }

        <div class="row-preview">
          <table>
            <caption>Linhas do arquivo para revisão</caption>
            <thead><tr><th scope="col">Linha</th><th scope="col">Pedido / produto</th><th scope="col">Data</th><th scope="col">Status</th><th scope="col">Valor / quantidade</th><th scope="col">Validação</th></tr></thead>
            <tbody>
              @for (row of current.rows; track row.rowNumber) {
                <tr [class.invalid-row]="!row.valid">
                  <td>{{ row.rowNumber }}</td>
                  <td>{{ row.externalId || row.itemName || '—' }}</td>
                  <td>{{ row.date ? (row.date | date:'dd/MM/yyyy':'':'pt-BR') : '—' }}</td>
                  <td>{{ row.status || '—' }}</td>
                  <td>@if(row.gross !== null){ {{ row.gross | currency:'BRL':'symbol':'1.2-2':'pt-BR' }} } @if(row.quantity !== null){ · {{ row.quantity }} un. }</td>
                  <td>
                    @if (!row.valid) { <span class="row-error">{{ row.issues.join(' ') }}</span> }
                    @else if (row.issues.length) { <span class="row-error">{{ row.issues.join(' ') }}</span> }
                    @else if (row.duplicateInFile) { Repetida no arquivo }
                    @else if (row.duplicateInSystem) { Já existe; será atualizada }
                    @else { Válida }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>

        @if (confirming()) { <div class="loading-line" role="status" aria-live="polite"><mat-progress-bar mode="indeterminate"/><span>Gravando os dados com segurança…</span></div> }
        @if (current.status === 'IMPORTED' || current.status === 'IMPORTED_WITH_ERRORS') {
          <div class="notice success" role="status"><mat-icon>check_circle</mat-icon>{{ current.status === 'IMPORTED_WITH_ERRORS' ? 'Importação concluída; as linhas inválidas foram mantidas fora do lote.' : 'Importação concluída. O histórico foi atualizado.' }}</div>
        }
        <div class="file-actions">
          @if (current.status === 'READY' && !current.alreadyImported) {
            <button mat-button type="button" (click)="clearFile()" [disabled]="confirming()">Descartar arquivos</button>
            <button mat-flat-button type="button" class="primary-button" (click)="confirmImport()" [disabled]="confirming() || !current.validRowCount">
              <mat-icon>check</mat-icon>{{ confirming() ? 'Importando…' : 'Confirmar este arquivo' }}
            </button>
          } @else if (current.alreadyImported && hasNextFile()) {
            <button mat-flat-button type="button" class="primary-button" (click)="advanceQueue()" [disabled]="uploading()"><mat-icon>arrow_forward</mat-icon>Continuar para o próximo arquivo</button>
          } @else {
            <button mat-button type="button" (click)="clearFile()">Selecionar outros arquivos</button>
          }
        </div>
      }
    </section>

    <section class="surface surface-body export-guide" aria-labelledby="export-guide-title">
      <span class="guide-icon"><mat-icon>download</mat-icon></span>
      <div class="guide-copy">
        <h2 id="export-guide-title">Exporte o relatório no portal</h2>
        <p>Baixe o relatório de vendas no portal da plataforma. Depois, selecione o arquivo acima, revise a prévia e confirme a importação.</p>
        <details class="tutorial">
          <summary>Ver tutorial de exportação e importação</summary>
          @if (provider() === 'IFOOD') {
            <ol>
              <li>No Portal do Parceiro, abra <strong>Análise → Relatórios</strong>.</li>
              <li>Para registrar pedidos, escolha <strong>Pedidos</strong>, informe o período e inclua os campos de cancelamento disponíveis. Baixe o XLSX quando estiver pronto.</li>
              <li>Para consultar itens, escolha <strong>Cardápio</strong> e exporte <strong>um dia por arquivo</strong>. O arquivo contém abas de itens e complementos.</li>
              <li>Volte a esta tela e selecione um ou mais arquivos. No Windows, mantenha <strong>Ctrl</strong> pressionado ao selecionar cada arquivo.</li>
              <li>Analise e confirme cada arquivo quando a prévia aparecer. A tela avança para o próximo automaticamente.</li>
            </ol>
            <p class="tutorial-note">O relatório de cardápio é agregado por dia e não informa os itens de cada pedido. Complementos podem fazer parte de combos. A importação atual não altera o estoque.</p>
          } @else {
            <ol>
              <li>No Portal 99Food, abra a área de relatórios e gere <strong>Dados do pedido</strong> para o período desejado. Mantenha o ID do pedido e os horários; inclua os campos financeiros, pagamento e cancelamento disponíveis.</li>
              <li>Gere também <strong>Dados do item</strong>, incluindo <strong>Receita do item</strong> e <strong>Volume de vendas do item</strong>. Esse relatório apresenta os itens por dia.</li>
              <li>Baixe os arquivos e selecione ambos nesta tela. No Windows, mantenha <strong>Ctrl</strong> pressionado ao selecionar cada arquivo.</li>
              <li>Analise e confirme cada arquivo separadamente. Se houver cancelamentos ou divergências na prévia, confira os dados no portal antes de confirmar.</li>
            </ol>
            <p class="tutorial-note">Pedidos e itens são relatórios com níveis de detalhe diferentes e seus totais podem divergir. A importação atual registra os dados, mas não altera o estoque nem o caixa.</p>
          }
        </details>
      </div>
      @if (provider() === 'IFOOD') {
        <a mat-stroked-button href="https://portal.ifood.com.br/merchant/profile" target="_blank" rel="noopener noreferrer">Abrir Portal do Parceiro <mat-icon>open_in_new</mat-icon></a>
      } @else {
        <a mat-stroked-button href="https://merchant.99app.com/pt-BR/manager/overview" target="_blank" rel="noopener noreferrer">Abrir Portal 99Food <mat-icon>open_in_new</mat-icon></a>
      }
    </section>

    <section class="history-block" aria-labelledby="import-history-title">
      <div class="history-heading"><div><h2 id="import-history-title">Importações recentes</h2><p>Os lotes concluídos ficam registrados para consulta e prevenção de repetição.</p></div><button mat-button type="button" (click)="loadHistory()" [disabled]="historyLoading()"><mat-icon>refresh</mat-icon>Atualizar</button></div>
      @if (historyLoading()) { <div class="surface surface-body loading-state"><mat-progress-bar mode="indeterminate"/><span>Carregando histórico…</span></div> }
      @else if (historyError()) { <div class="notice error" role="alert"><mat-icon>error</mat-icon>{{ historyError() }} <button mat-button (click)="loadHistory()">Tentar novamente</button></div> }
      @else if (history().length === 0) { <div class="surface surface-body empty-state"><mat-icon>history</mat-icon><strong>Nenhuma importação realizada</strong><span>Os relatórios enviados aparecerão aqui.</span></div> }
      @else { <div class="surface history-list"><div class="history-row history-labels"><span>Arquivo</span><span>Período</span><span>Registros</span><span>Status</span><span>Importado em</span></div>@for (item of history(); track item.id) {<div class="history-row"><span class="file-name">{{ item.fileName }}</span><span>{{ item.periodStart ? (item.periodStart | date:'dd/MM/yy':'':'pt-BR') : '—' }} @if(item.periodEnd && item.periodEnd !== item.periodStart){– {{ item.periodEnd | date:'dd/MM/yy':'':'pt-BR' }}}</span><span>{{ item.validRowCount }} / {{ item.rowCount }}</span><span class="history-status" [class.error-status]="item.status === 'IMPORTED_WITH_ERRORS'">{{ item.status === 'IMPORTED' ? 'Concluída' : item.status === 'READY' ? 'Aguardando confirmação' : 'Concluída com linhas inválidas' }}</span><span>{{ (item.importedAt || item.createdAt) | date:'dd/MM/yy HH:mm':'':'pt-BR' }}</span></div>}</div> }
    </section>
  `,
  styles: [`
    :host{display:block}.import-card{max-width:1040px}.section-heading,.history-heading{display:flex;justify-content:space-between;align-items:flex-start;gap:16px}.section-heading>mat-icon{font-size:26px;width:26px;height:26px;color:var(--red)}.eyebrow{font-size:.65rem;color:var(--red-dark);text-transform:uppercase;letter-spacing:.08em;font-weight:800}.section-heading h2,.history-heading h2{font-size:1rem;margin:5px 0}.section-heading p,.history-heading p{color:var(--muted);font-size:.76rem;line-height:1.5;margin:0}.visually-hidden{position:absolute;width:1px;height:1px;padding:0;margin:-1px;overflow:hidden;clip:rect(0,0,0,0);white-space:nowrap;border:0}.drop-zone{margin:18px 0 10px;width:100%;min-height:168px;border:2px dashed #d6d3d1;background:#fafaf9;border-radius:12px;display:grid;place-items:center;align-content:center;gap:7px;cursor:pointer;color:var(--muted)}.drop-zone:focus-visible{outline:3px solid #f4c542;outline-offset:2px}.drop-zone:disabled{opacity:.65;cursor:wait}.drop-zone>mat-icon{font-size:36px;width:36px;height:36px;color:var(--red)}.drop-zone strong{color:var(--text);font-size:.85rem}.drop-zone span{font-size:.7rem}.choose-label{color:var(--red-dark);font-weight:800}.file-actions{display:flex;justify-content:flex-end;gap:8px;align-items:center;margin-top:14px}.primary-button mat-icon{margin-right:5px}.notice{display:flex;align-items:flex-start;gap:8px;padding:11px 13px;border-radius:9px;font-size:.75rem;line-height:1.45;margin:12px 0}.notice mat-icon{flex:none;font-size:18px;width:18px;height:18px}.notice.warning{background:#fff8db;color:#694d0b;border:1px solid #f3dc86}.notice.error{background:#fff0ee;color:#8f1d14;border:1px solid #f3c4bf}.notice.success{background:#edf7ed;color:#246b2d;border:1px solid #b9dfbd}.loading-line{margin:14px 0;display:grid;gap:8px;color:var(--muted);font-size:.74rem}.preview-heading{display:flex;justify-content:space-between;gap:12px;align-items:center;margin-top:25px}.preview-heading h3{font-size:.95rem;margin:0}.preview-heading p{font-size:.7rem;color:var(--muted);margin:4px 0 0;overflow-wrap:anywhere}.type-chip{background:#fff3bf;color:#5f4611;font-size:.67rem;font-weight:700;padding:6px 9px;border-radius:99px}.period-note{margin:12px 0;color:var(--muted);font-size:.73rem}.period-note strong{color:var(--text)}.summary-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:8px}.summary-grid>div{padding:11px;border:1px solid var(--border);border-radius:8px;background:#fafaf9}.summary-grid small,.summary-grid strong{display:block}.summary-grid small{font-size:.65rem;color:var(--muted)}.summary-grid strong{font-size:.83rem;margin-top:4px;overflow-wrap:anywhere}.summary-grid .highlight{background:#fff8db;border-color:#f1dc91}.row-preview{max-height:360px;overflow:auto;border:1px solid var(--border);border-radius:8px;margin-top:14px}.row-preview table{width:100%;border-collapse:collapse;font-size:.68rem}.row-preview caption{text-align:left;padding:10px;font-weight:700;background:#fafaf9;position:sticky;top:0}.row-preview th,.row-preview td{padding:9px;border-bottom:1px solid var(--border);text-align:left;vertical-align:top}.row-preview th{font-size:.64rem;color:var(--muted);white-space:nowrap;background:#fff}.invalid-row{background:#fff5f3}.row-error{color:#8f1d14}.export-guide{max-width:1040px;margin-top:16px;display:grid;grid-template-columns:40px 1fr auto;gap:14px;align-items:center}.guide-icon{width:40px;height:40px;border-radius:10px;background:#fff8db;color:#8f1d14;display:grid;place-items:center}.guide-copy h2{font-size:.9rem;margin:0 0 4px}.guide-copy p,.guide-copy small{display:block;color:var(--muted);font-size:.72rem;line-height:1.5;margin:0}.guide-copy small{margin-top:5px}.tutorial{margin-top:12px;border-top:1px solid var(--border);padding-top:10px;font-size:.72rem;line-height:1.55;color:var(--text)}.tutorial summary{cursor:pointer;font-weight:700;color:var(--red-dark)}.tutorial summary:focus-visible{outline:2px solid var(--red);outline-offset:3px;border-radius:3px}.tutorial ol{padding-left:19px;margin:9px 0}.tutorial li{margin:5px 0}.tutorial-note{margin:8px 0 0;padding:9px 11px;background:#fff8db;border-radius:7px;color:#694d0b}.export-guide a{white-space:nowrap}.export-guide a mat-icon{font-size:17px;width:17px;height:17px;margin-left:4px}.history-block{max-width:1040px;margin-top:28px}.history-heading{align-items:center;margin-bottom:12px}.history-list{overflow:hidden}.history-row{display:grid;grid-template-columns:minmax(180px,2fr) 1fr .7fr 1.3fr 1fr;gap:10px;padding:11px 14px;border-top:1px solid var(--border);align-items:center;font-size:.69rem}.history-row:first-child{border-top:0}.history-labels{font-size:.63rem;color:var(--muted);font-weight:700;background:#fafaf9}.file-name{overflow-wrap:anywhere}.history-status{color:#246b2d}.history-status.error-status{color:#8f1d14}.loading-state{display:grid;gap:10px;color:var(--muted);font-size:.75rem}.empty-state{display:grid;place-items:center;gap:8px;text-align:center;color:var(--muted);font-size:.75rem}.empty-state mat-icon{color:#a8a29e}.empty-state strong{color:var(--text)}@media(max-width:800px){.summary-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.history-row{grid-template-columns:minmax(140px,1.5fr) 1fr .6fr 1fr;}.history-row>*:last-child{display:none}.history-labels>*:last-child{display:none}}@media(max-width:560px){.export-guide{grid-template-columns:36px 1fr}.export-guide>a{grid-column:2;justify-self:start}.preview-heading{align-items:flex-start;flex-direction:column}.row-preview table{min-width:700px}.history-row{grid-template-columns:minmax(125px,1.4fr) 1fr .7fr}.history-row>*:nth-child(4){display:none}.history-labels>*:nth-child(4){display:none}}
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class ImportWorkflowComponent implements OnInit {
  readonly provider = input.required<IntegrationProvider>();
  private readonly service = inject(IntegrationsService);
  readonly fileControl = new FormControl<File[] | null>(null);
  readonly selectedFiles = signal<File[]>([]);
  readonly fileIndex = signal(0);
  readonly batch = signal<ImportBatch | null>(null);
  readonly history = signal<ImportBatch[]>([]);
  readonly uploading = signal(false);
  readonly confirming = signal(false);
  readonly historyLoading = signal(false);
  readonly fileError = signal('');
  readonly errorMessage = signal('');
  readonly historyError = signal('');

  ngOnInit(): void { this.loadHistory(); }

  selectFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    this.fileError.set(''); this.errorMessage.set(''); this.batch.set(null); this.fileIndex.set(0);
    if (!files.length) { this.fileControl.setValue(null); this.selectedFiles.set([]); return; }
    const invalidType = files.find(file => !/\.(xlsx|zip)$/i.test(file.name));
    if (invalidType) { this.fileControl.setValue(null); this.selectedFiles.set([]); this.fileError.set(`O arquivo ${invalidType.name} não é XLSX ou ZIP.`); input.value = ''; return; }
    const oversized = files.find(file => file.size > 10 * 1024 * 1024);
    if (oversized) { this.fileControl.setValue(null); this.selectedFiles.set([]); this.fileError.set(`O arquivo ${oversized.name} excede o limite de 10 MB.`); input.value = ''; return; }
    this.selectedFiles.set(files); this.fileControl.setValue(files);
  }

  async analyze(): Promise<void> {
    const file = this.selectedFiles()[this.fileIndex()];
    if (!file) return;
    await this.analyzeFile(file);
  }

  private async analyzeFile(file: File): Promise<void> {
    this.uploading.set(true); this.errorMessage.set('');
    try {
      this.batch.set(await firstValueFrom(this.service.upload(this.provider(), file)));
      await this.loadHistory();
    } catch (error) {
      this.errorMessage.set(this.apiMessage(error, `Não foi possível analisar ${file.name}. Você pode tentar novamente ou selecionar outro relatório.`));
    } finally { this.uploading.set(false); }
  }

  async confirmImport(): Promise<void> {
    const current = this.batch();
    if (!current || current.status !== 'READY' || current.alreadyImported) return;
    let completed = false;
    this.confirming.set(true); this.errorMessage.set('');
    try {
      this.batch.set(await firstValueFrom(this.service.confirm(current.id)));
      await this.loadHistory();
      completed = true;
    } catch (error) {
      this.errorMessage.set(this.apiMessage(error, 'Não foi possível concluir a importação. O lote continua disponível para nova tentativa.'));
    } finally { this.confirming.set(false); }
    if (completed && this.hasNextFile()) await this.advanceQueue();
  }

  selectedFileNames(): string { return this.selectedFiles().map(file => file.name).join(', '); }
  hasNextFile(): boolean { return this.fileIndex() + 1 < this.selectedFiles().length; }

  async advanceQueue(): Promise<void> {
    if (!this.hasNextFile() || this.uploading() || this.confirming()) return;
    this.fileIndex.update(index => index + 1);
    this.batch.set(null);
    const file = this.selectedFiles()[this.fileIndex()];
    if (file) await this.analyzeFile(file);
  }

  clearFile(): void { this.fileControl.setValue(null); this.selectedFiles.set([]); this.fileIndex.set(0); this.batch.set(null); this.fileError.set(''); this.errorMessage.set(''); }

  async loadHistory(): Promise<void> {
    this.historyLoading.set(true); this.historyError.set('');
    try { this.history.set(await firstValueFrom(this.service.history(this.provider()))); }
    catch (error) { this.historyError.set(this.apiMessage(error, 'Não foi possível carregar o histórico de importações.')); }
    finally { this.historyLoading.set(false); }
  }

  private apiMessage(error: unknown, fallback: string): string {
    const value = error as { error?: { detail?: string; message?: string }; message?: string };
    return value?.error?.detail || value?.error?.message || value?.message || fallback;
  }
}
