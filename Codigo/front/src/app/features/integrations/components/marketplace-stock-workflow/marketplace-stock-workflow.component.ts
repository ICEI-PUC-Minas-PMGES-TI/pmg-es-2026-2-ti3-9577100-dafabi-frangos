import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input, OnInit, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { firstValueFrom } from 'rxjs';
import { ProductsApiService } from '../../../products/services/products-api.service';
import { ApiProductListItem } from '../../../products/models/product-api.models';
import { MarketplaceItemSnapshot, MarketplaceMapping, MarketplaceStockEffect, MarketplaceSettlement, IntegrationProvider } from '../../models/integration.models';
import { IntegrationsService } from '../../services/integrations.service';

interface DraftComponent { productId: string; quantityPerItem: number; }
interface MappingDraft { ignored: boolean; components: DraftComponent[]; }

@Component({
  selector: 'app-marketplace-stock-workflow', standalone: true,
  imports: [DatePipe, MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    <section class="surface surface-body stock-workflow">
      <div class="heading"><div><span class="eyebrow">Vínculo com o estoque</span><h2>Associe os itens exportados aos produtos DaFabi</h2>
        <p>Faça isso uma vez para cada loja e item. Combos podem ser associados a vários componentes.</p></div><mat-icon>inventory_2</mat-icon></div>
      @if (loading()) { <div class="loading"><mat-progress-bar mode="indeterminate"/><span>Carregando itens e produtos…</span></div> }
      @else if (error()) { <div class="notice error" role="alert">{{ error() }} <button mat-button (click)="load()">Tentar novamente</button></div> }
      @else if (!stores().length) { <div class="empty"><mat-icon>cloud_download</mat-icon><strong>Importe primeiro um relatório de itens</strong><span>Os nomes dos itens e a loja aparecerão aqui após importar um relatório de cardápio do iFood ou Dados do item da 99Food.</span></div> }
      @else {
        <label class="field">Loja no marketplace
          <select [value]="storeId()" (change)="changeStore($any($event.target).value)">
            @for (store of stores(); track store.id) { <option [value]="store.id">{{ store.name }} · {{ store.id }}</option> }
          </select>
        </label>

        <div class="notice warning"><mat-icon>info</mat-icon>Antes de começar, faça a contagem física e atualize o estoque DaFabi. A data inicial limita quais dias podem alterar o saldo. Relatórios anteriores ficam apenas como histórico.</div>

        @if (mappingError()) { <div class="notice warning" role="status"><mat-icon>warning</mat-icon>{{ mappingError() }}</div> }
        @if (mappingLoading()) { <div class="loading"><mat-progress-bar mode="indeterminate"/><span>Carregando associações…</span></div> }
        @else if (mappingRows().length) {
          @if (!products().length) { <div class="notice warning" role="status"><mat-icon>inventory_2</mat-icon>Cadastre produtos ativos no estoque antes de associar os itens da plataforma.</div> }
          <div class="mapping-list">
            @for (mapping of mappingRows(); track mapping.externalItemName) {
              <article class="mapping-row">
                <div class="item-name"><strong>{{ mapping.externalItemName }}</strong><small>Nome do relatório · {{ itemSummary(mapping.externalItemName) }}</small></div>
                <div class="components">
                  @if (!draft(mapping.externalItemName).ignored) {
                    @for (component of draft(mapping.externalItemName).components; track $index; let i = $index) {
                      <div class="component-line">
                        <label class="field">Produto DaFabi
                          <select [value]="component.productId" (change)="setProduct(mapping.externalItemName,i,$any($event.target).value)">
                            <option value="">Selecione um produto</option>
                            @for(product of products();track product.id){<option [value]="product.id">{{ product.name }} ({{ product.unit }})</option>}
                          </select>
                        </label>
                        <label class="field quantity">Unidades por item
                          <input type="number" min="0.001" step="0.001" [value]="component.quantityPerItem" (change)="setQuantity(mapping.externalItemName,i,$any($event.target).value)">
                        </label>
                        <button mat-icon-button type="button" aria-label="Remover componente" (click)="removeComponent(mapping.externalItemName,i)"><mat-icon>delete_outline</mat-icon></button>
                      </div>
                    }
                    <button mat-button type="button" (click)="addComponent(mapping.externalItemName)"><mat-icon>add</mat-icon>Adicionar componente do combo</button>
                  } @else { <p class="muted">Este item não movimentará estoque.</p> }
                  <label class="ignore"><input type="checkbox" [checked]="draft(mapping.externalItemName).ignored" (change)="setIgnored(mapping.externalItemName,$any($event.target).checked)"> Não controlar este item no estoque</label>
                  <button mat-stroked-button type="button" (click)="saveMapping(mapping.externalItemName)" [disabled]="!mappingApiAvailable() || savingItem()===mapping.externalItemName || (!draft(mapping.externalItemName).ignored && !draftValid(mapping.externalItemName))">
                    <mat-icon>save</mat-icon>{{ savingItem()===mapping.externalItemName ? 'Salvando…' : 'Salvar associação' }}
                  </button>
                  @if (savedItem()===mapping.externalItemName) { <span class="saved" role="status">Associação salva</span> }
                </div>
              </article>
            }
          </div>
        } @else if (!hasItemsForStore()) { <div class="empty"><mat-icon>inventory_2</mat-icon><strong>Nenhum item encontrado</strong><span>Os relatórios importados não contêm itens para esta loja.</span></div> }

        <div class="baseline-block">
          <h3>Defina a data inicial do controle</h3>
          <p>Conte o estoque físico, atualize o saldo em Estoque e informe a partir de qual dia os relatórios serão aplicados. Essa data é independente para cada loja e plataforma.</p>
          <div class="baseline-form"><label class="field">Primeiro dia com baixa integrada<input type="date" [value]="baselineDate()" (change)="baselineDate.set($any($event.target).value)"></label>
            <button mat-flat-button class="primary-button" type="button" (click)="saveBaseline()" [disabled]="!baselineDate() || baselineSaving()"><mat-icon>event_available</mat-icon>{{ baselineSaving()?'Salvando…':'Salvar data inicial' }}</button>
            @if(baselineSaved()){<span class="saved" role="status">Data inicial: {{ baselineSaved() | date:'dd/MM/yyyy':'':'pt-BR' }}</span>}
          </div>
        </div>

        <div class="effects-block">
          <h3>Prévia das baixas e devoluções</h3>
          <p>Revise o período antes de aplicar. Aumentos nas vendas baixam estoque; correções para baixo exigem confirmar a devolução física dos produtos.</p>
          <div class="effect-filters"><label class="field">De<input type="date" [value]="startDate()" (change)="startDate.set($any($event.target).value)"></label><label class="field">Até<input type="date" [value]="endDate()" (change)="endDate.set($any($event.target).value)"></label>
            <button mat-stroked-button type="button" (click)="preview()" [disabled]="previewLoading()"><mat-icon>preview</mat-icon>Conferir prévia</button></div>
          @if(previewLoading()){<div class="loading"><mat-progress-bar mode="indeterminate"/><span>Calculando efeitos no estoque…</span></div>}
          @if(effectError()){<div class="notice error" role="alert">{{ effectError() }}</div>}
          @if(effects().length){
            <div class="effect-list"><div class="effect-head"><span>Item do portal</span><span>Produto DaFabi</span><span>Vendido</span><span>Já aplicado</span><span>Variação</span><span>Situação</span></div>
              @for(effect of effects();track effect.snapshotId+'-'+(effect.productId||'none')){<div class="effect-row"><span>{{effect.externalItemName}}</span><span>{{effect.productName||'—'}}</span><span>{{effect.reportedQuantity}}</span><span>{{effect.appliedQuantity}}</span><span>{{effect.quantityToApply}}</span><span class="effect-status" [class.pending]="effect.status!=='APPLIED'&&effect.status!=='IGNORED'&&effect.status!=='BEFORE_BASELINE'">{{statusLabel(effect.status)}}<small>{{effect.explanation}}</small></span></div>}
            </div>
            @if(hasReturnReview()){<label class="return-confirm"><input type="checkbox" [checked]="confirmReturns()" (change)="confirmReturns.set($any($event.target).checked)"> Confirmei no portal e na operação que os itens cancelados/retificados voltaram fisicamente ao estoque.</label>}
            @if(hasUnresolved()){<div class="notice warning"><mat-icon>warning</mat-icon>Resolva os itens sem associação, unidades fracionadas e saldos insuficientes antes de aplicar este período.</div>}
            <div class="actions"><button mat-flat-button class="primary-button" type="button" (click)="apply()" [disabled]="!canApply() || applying()"><mat-icon>inventory</mat-icon>{{applying()?'Aplicando…':'Aplicar movimentos aprovados'}}</button></div>
          } @else if (previewed()) { <div class="empty"><strong>Nenhum efeito de estoque para este período.</strong></div> }
        </div>
        <div class="settlement-block">
          <h3>Conferir repasses da plataforma</h3>
          <p>Consulte a área Financeiro do portal e registre aqui cada período de pagamento. Repasses ficam separados das vendas e não entram novamente no faturamento nem no caixa físico.</p>
          <div class="notice warning"><mat-icon>account_balance</mat-icon>Este lançamento é digitado a partir do resumo financeiro do portal; os relatórios de repasse ainda não têm um formato de arquivo validado para leitura automática.</div>
          <div class="settlement-form">
            <label class="field">ID ou referência do repasse<input [value]="settlementId()" (input)="settlementId.set($any($event.target).value)" maxlength="160"></label>
            <label class="field">Período inicial<input type="date" [value]="settlementStart()" (change)="settlementStart.set($any($event.target).value)"></label>
            <label class="field">Período final<input type="date" [value]="settlementEnd()" (change)="settlementEnd.set($any($event.target).value)"></label>
            <label class="field">Valor bruto<input type="number" min="0" step="0.01" [value]="settlementGross()" (input)="settlementGross.set($any($event.target).value)"></label>
            <label class="field">Taxas e comissões<input type="number" min="0" step="0.01" [value]="settlementFees()" (input)="settlementFees.set($any($event.target).value)"></label>
            <label class="field">Ajustes<input type="number" step="0.01" [value]="settlementAdjustments()" (input)="settlementAdjustments.set($any($event.target).value)"></label>
            <label class="field">Valor retido<input type="number" min="0" step="0.01" [value]="settlementRetained()" (input)="settlementRetained.set($any($event.target).value)"></label>
            <label class="field">Valor recebido<input type="number" min="0" step="0.01" [value]="settlementReceived()" (input)="settlementReceived.set($any($event.target).value)"></label>
            <label class="field">Data do crédito<input type="date" [value]="settlementPayoutDate()" (change)="settlementPayoutDate.set($any($event.target).value)"></label>
            <label class="field">Situação<select [value]="settlementStatus()" (change)="settlementStatus.set($any($event.target).value)"><option value="PENDENTE">Pendente</option><option value="PAGO">Pago</option><option value="RETIDO">Retido</option><option value="NAO_NECESSARIO">Pagamento não necessário</option></select></label>
            <label class="field source-reference">Observação do portal<textarea [value]="settlementReference()" (input)="settlementReference.set($any($event.target).value)" maxlength="1000"></textarea></label>
          </div>
          @if(settlementStatus()==='PAGO'&&(!settlementPayoutDate()||!settlementReceived())){<p class="validation-note" role="alert">Para marcar como pago, informe a data do crédito e o valor recebido.</p>}
          <div class="actions"><button mat-flat-button class="primary-button" type="button" (click)="saveSettlement()" [disabled]="settlementSaving() || !settlementValid()"><mat-icon>save</mat-icon>{{settlementSaving()?'Salvando…':'Registrar repasse'}}</button></div>
          @if(settlements().length){<div class="settlement-history"><div class="settlement-head"><span>Referência</span><span>Período</span><span>Taxas / ajustes / retenção</span><span>Recebido</span><span>Situação</span></div>@for(row of settlements();track row.id){<div class="settlement-row"><strong>{{row.externalSettlementId}}</strong><span>{{row.periodStart|date:'dd/MM/yy':'':'pt-BR'}}–{{row.periodEnd|date:'dd/MM/yy':'':'pt-BR'}}</span><span>{{moneyOrDash(row.feesAmount)}} / {{moneyOrDash(row.adjustmentsAmount)}} / {{moneyOrDash(row.retainedAmount)}}</span><span>{{moneyOrDash(row.amountReceived)}}</span><span>{{row.status==='PAGO'?'Pago':row.status==='RETIDO'?'Retido':row.status==='NAO_NECESSARIO'?'Não necessário':'Pendente'}}</span></div>}</div>}
        </div>
        @if(message()){<div class="notice success" role="status">{{message()}}</div>}
        @if(errorMessage()){<div class="notice error" role="alert">{{errorMessage()}}</div>}
      }
    </section>
  `,
  styles: [`
    :host{display:block}.stock-workflow{max-width:1100px;margin-top:18px}.heading{display:flex;justify-content:space-between;gap:16px}.heading h2{font-size:1rem;margin:5px 0}.heading p,.baseline-block p,.effects-block p,.settlement-block p{font-size:.75rem;color:var(--muted);line-height:1.5;margin:0 0 12px}.heading>mat-icon{color:var(--red);font-size:26px}.eyebrow{font-size:.65rem;color:var(--red-dark);text-transform:uppercase;letter-spacing:.08em;font-weight:800}.notice{display:flex;gap:8px;align-items:flex-start;padding:11px 13px;border-radius:9px;font-size:.75rem;line-height:1.5;margin:14px 0}.notice mat-icon{font-size:18px;width:18px;height:18px;flex:none}.notice.warning{background:#fff8db;color:#694d0b;border:1px solid #f3dc86}.notice.error{background:#fff0ee;color:#8f1d14;border:1px solid #f3c4bf}.notice.success{background:#edf7ed;color:#246b2d;border:1px solid #b9dfbd}.field{display:grid;gap:5px;color:var(--muted);font-size:.68rem;font-weight:700}.field select,.field input,.field textarea{min-height:38px;border:1px solid var(--border);border-radius:7px;padding:7px 9px;color:var(--text);background:#fff}.field select:focus-visible,.field input:focus-visible,.field textarea:focus-visible{outline:2px solid var(--red);outline-offset:1px}.mapping-list{display:grid;gap:9px;margin-top:13px}.mapping-row{display:grid;grid-template-columns:minmax(180px,.7fr) minmax(360px,1.6fr);gap:14px;padding:13px;border:1px solid var(--border);border-radius:9px}.item-name strong,.item-name small{display:block}.item-name strong{font-size:.78rem}.item-name small,.muted{font-size:.66rem;color:var(--muted);margin-top:4px}.components{display:grid;gap:8px}.component-line{display:grid;grid-template-columns:1fr 140px 40px;align-items:end;gap:8px}.components>button{justify-self:start}.ignore{display:flex;align-items:center;gap:7px;font-size:.69rem;color:var(--text)}.saved{font-size:.68rem;color:#246b2d}.baseline-block,.effects-block,.settlement-block{border-top:1px solid var(--border);margin-top:20px;padding-top:17px}.baseline-block h3,.effects-block h3,.settlement-block h3{font-size:.88rem;margin:0 0 6px}.baseline-form,.effect-filters{display:flex;align-items:end;gap:10px;flex-wrap:wrap}.baseline-form .field{min-width:220px}.effect-filters .field{min-width:150px}.effect-list,.settlement-history{overflow:auto;border:1px solid var(--border);border-radius:8px;margin-top:12px}.effect-head,.effect-row{display:grid;grid-template-columns:1.25fr 1fr .55fr .55fr .55fr 1.4fr;gap:8px;padding:9px 11px;border-bottom:1px solid var(--border);font-size:.67rem;min-width:760px}.effect-head,.settlement-head{font-weight:700;color:var(--muted);background:#fafaf9}.effect-row:last-child,.settlement-row:last-child{border-bottom:0}.effect-status{font-weight:700;color:#246b2d}.effect-status.pending{color:#8a6200}.effect-status small{display:block;font-weight:400;line-height:1.4;margin-top:3px;color:var(--muted)}.return-confirm{display:flex;gap:8px;align-items:flex-start;margin:12px 0;font-size:.72rem;color:#694d0b}.actions{display:flex;justify-content:flex-end;margin-top:12px}.empty{display:grid;justify-items:center;text-align:center;gap:7px;padding:24px;color:var(--muted);font-size:.73rem}.empty strong{color:var(--text)}.empty mat-icon{color:#a8a29e}.loading{display:grid;gap:8px;margin:14px 0;color:var(--muted);font-size:.72rem}.settlement-form{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:9px}.source-reference{grid-column:1/-1}.settlement-head,.settlement-row{display:grid;grid-template-columns:1.1fr 1fr 1.6fr 1fr .8fr;gap:8px;padding:9px 11px;border-bottom:1px solid var(--border);font-size:.67rem;min-width:680px}.settlement-head{position:sticky;top:0}.settlement-row strong{overflow-wrap:anywhere}
    @media(max-width:700px){.mapping-row{grid-template-columns:1fr}.component-line{grid-template-columns:1fr 110px 36px}.baseline-form{align-items:stretch;flex-direction:column}.baseline-form .field{min-width:0}}
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MarketplaceStockWorkflowComponent implements OnInit {
  readonly provider=input.required<IntegrationProvider>();
  private readonly integrations=inject(IntegrationsService);
  private readonly productsApi=inject(ProductsApiService);
  readonly loading=signal(true);readonly mappingLoading=signal(false);readonly previewLoading=signal(false);
  readonly baselineSaving=signal(false);readonly applying=signal(false);readonly savingItem=signal('');
  readonly error=signal('');readonly errorMessage=signal('');readonly mappingError=signal('');readonly mappingApiAvailable=signal(true);readonly effectError=signal('');readonly message=signal('');readonly savedItem=signal('');
  readonly items=signal<MarketplaceItemSnapshot[]>([]);readonly products=signal<ApiProductListItem[]>([]);readonly stores=signal<{id:string;name:string}[]>([]);
  readonly storeId=signal('');readonly mappings=signal<MarketplaceMapping[]>([]);readonly drafts=signal<Record<string,MappingDraft>>({});
  readonly baselineDate=signal('');readonly baselineSaved=signal('');readonly startDate=signal('');readonly endDate=signal('');
  readonly effects=signal<MarketplaceStockEffect[]>([]);readonly previewed=signal(false);readonly confirmReturns=signal(false);
  readonly settlements=signal<MarketplaceSettlement[]>([]);readonly settlementId=signal('');readonly settlementStart=signal('');readonly settlementEnd=signal('');
  readonly settlementGross=signal('');readonly settlementFees=signal('');readonly settlementAdjustments=signal('');readonly settlementRetained=signal('');readonly settlementReceived=signal('');readonly settlementPayoutDate=signal('');readonly settlementStatus=signal('PENDENTE');readonly settlementReference=signal('');readonly settlementSaving=signal(false);

  ngOnInit():void{void this.load();}
  async load():Promise<void>{this.loading.set(true);this.error.set('');try{
    const [items,productPage]=await Promise.all([firstValueFrom(this.integrations.items(this.provider())),firstValueFrom(this.productsApi.list({status:'ACTIVE',page:0,size:100}))]);
    this.items.set(items);this.products.set(productPage.content);
    const stores=[...new Map(items.map(item=>[item.externalStoreId,{id:item.externalStoreId,name:item.externalStoreId}])).values()];
    this.stores.set(stores);if(stores.length){const selected=this.storeId()&&stores.some(s=>s.id===this.storeId())?this.storeId():stores[0].id;this.storeId.set(selected);await this.loadStore(selected);}
  }catch{this.error.set('Não foi possível carregar itens importados e produtos. Confira a conexão e tente novamente.');}finally{this.loading.set(false);}}
  async changeStore(value:string){this.storeId.set(value);await this.loadStore(value);}
  private async loadStore(value:string){this.mappingLoading.set(true);this.message.set('');this.errorMessage.set('');this.mappingError.set('');this.mappingApiAvailable.set(true);this.mappings.set([]);
    const names=[...new Set(this.items().filter(item=>item.externalStoreId===value).map(item=>item.externalItemName))];
    try{
      const maps=await firstValueFrom(this.integrations.mappings(this.provider(),value));
      this.mappings.set(maps);
    }catch{
      this.mappingApiAvailable.set(false);
      this.mappingError.set('Os itens do relatório foram carregados, mas o servidor não disponibiliza as associações agora. Você pode conferir os itens, mas não salvar vínculos nem aplicar baixas até atualizar o backend.');
    }
    const drafts:Record<string,MappingDraft>={};for(const map of this.mappings())drafts[map.externalItemName]={ignored:map.ignored,components:map.components.length?map.components.map(c=>({productId:c.productId,quantityPerItem:c.quantityPerItem})):[{productId:'',quantityPerItem:1}]};
    for(const name of names)if(!drafts[name])drafts[name]={ignored:false,components:[{productId:'',quantityPerItem:1}]};this.drafts.set(drafts);
    const dates=this.items().filter(item=>item.externalStoreId===value).map(item=>item.date).sort();this.startDate.set(dates[0]??'');this.endDate.set(dates[dates.length-1]??'');
    this.baselineDate.set('');this.baselineSaved.set('');this.effects.set([]);this.previewed.set(false);
    try{const baseline=await firstValueFrom(this.integrations.getStockBaseline(this.provider(),value));this.baselineDate.set(baseline?.startDate??'');this.baselineSaved.set(baseline?.startDate??'');}
    catch{this.errorMessage.set('Não foi possível carregar a data inicial do estoque. A lista de itens continua disponível.');}
    try{this.settlements.set(await firstValueFrom(this.integrations.settlements(this.provider(),value)));}
    catch{this.settlements.set([]);}
    this.mappingLoading.set(false);
  }
  mappingRows():MarketplaceMapping[]{
    const byName=new Map(this.mappings().map(mapping=>[mapping.externalItemName,mapping]));
    const names=new Set(this.items().filter(item=>item.externalStoreId===this.storeId()).map(item=>item.externalItemName));
    return [...names].sort((a,b)=>a.localeCompare(b,'pt-BR')).map(name=>byName.get(name)??{
      provider:this.provider(),externalStoreId:this.storeId(),externalItemName:name,ignored:false,components:[]
    });
  }
  hasItemsForStore():boolean{return this.items().some(item=>item.externalStoreId===this.storeId());}
  itemSummary(name:string):string{
    const rows=this.items().filter(item=>item.externalStoreId===this.storeId()&&item.externalItemName===name);
    const quantity=rows.reduce((sum,item)=>sum+Number(item.quantity||0),0);
    const revenue=rows.reduce((sum,item)=>sum+Number(item.revenue||0),0);
    return `${quantity.toLocaleString('pt-BR')} un. · ${new Intl.NumberFormat('pt-BR',{style:'currency',currency:'BRL'}).format(revenue)}`;
  }
  draft(name:string):MappingDraft{return this.drafts()[name]??{ignored:false,components:[{productId:'',quantityPerItem:1}]};}
  update(name:string,change:(draft:MappingDraft)=>MappingDraft){this.drafts.update(all=>({...all,[name]:change(this.draft(name))}));this.savedItem.set('');}
  setIgnored(name:string,value:boolean){this.update(name,d=>({...d,ignored:value,components:value?[]:(d.components.length?d.components:[{productId:'',quantityPerItem:1}])}));}
  setProduct(name:string,index:number,value:string){this.update(name,d=>({...d,components:d.components.map((c,i)=>i===index?{...c,productId:value}:c)}));}
  setQuantity(name:string,index:number,value:string){const quantity=Number(value);this.update(name,d=>({...d,components:d.components.map((c,i)=>i===index?{...c,quantityPerItem:quantity}:c)}));}
  addComponent(name:string){this.update(name,d=>({...d,components:[...d.components,{productId:'',quantityPerItem:1}]}));}
  removeComponent(name:string,index:number){this.update(name,d=>({...d,components:d.components.filter((_,i)=>i!==index)}));}
  draftValid(name:string):boolean{const d=this.draft(name);return d.components.length>0&&d.components.every(c=>!!c.productId&&Number.isFinite(c.quantityPerItem)&&c.quantityPerItem>0);}
  async saveMapping(name:string){if(!this.mappingApiAvailable())return;const d=this.draft(name);this.savingItem.set(name);this.errorMessage.set('');try{const saved=await firstValueFrom(this.integrations.saveMapping(this.provider(),this.storeId(),name,{ignored:d.ignored,components:d.ignored?[]:d.components.map(c=>({productId:c.productId,quantityPerItem:c.quantityPerItem}))}));this.mappings.update(rows=>[...rows.filter(row=>row.externalItemName!==name),saved]);this.savedItem.set(name);this.message.set('Associação salva. Confira novamente a prévia do estoque antes de aplicá-lo.');}catch{this.errorMessage.set(`Não foi possível salvar a associação de “${name}”.`);}finally{this.savingItem.set('');}}
  async saveBaseline(){this.baselineSaving.set(true);this.errorMessage.set('');try{const response=await firstValueFrom(this.integrations.setStockBaseline(this.provider(),this.storeId(),this.baselineDate()));this.baselineSaved.set(response.startDate);this.message.set('Data inicial salva. Os relatórios anteriores a essa data não serão baixados do estoque.');this.effects.set([]);this.previewed.set(false);}catch{this.errorMessage.set('Não foi possível salvar a data inicial do estoque.');}finally{this.baselineSaving.set(false);}}
  async preview(){if(!this.startDate()||!this.endDate())return;this.previewLoading.set(true);this.effectError.set('');this.previewed.set(false);try{this.effects.set(await firstValueFrom(this.integrations.stockEffects(this.provider(),this.storeId(),this.startDate(),this.endDate())));this.previewed.set(true);}catch{this.effectError.set('Não foi possível calcular a prévia. Confira o período e a loja selecionada.');}finally{this.previewLoading.set(false);}}
  hasReturnReview():boolean{return this.effects().some(e=>e.status==='RETURN_REVIEW'||e.quantityToApply<0);}
  hasUnresolved():boolean{return this.effects().some(e=>['UNMAPPED','BASELINE_REQUIRED','UNIT_REVIEW','INSUFFICIENT_STOCK'].includes(e.status))||this.hasReturnReview()&&!this.confirmReturns();}
  canApply():boolean{return this.previewed()&&!!this.baselineSaved()&&!this.hasUnresolved()&&this.effects().some(e=>e.status==='PENDING'||e.status==='RETURN_REVIEW');}
  async apply(){this.applying.set(true);this.effectError.set('');this.message.set('');try{this.effects.set(await firstValueFrom(this.integrations.applyStock(this.provider(),this.storeId(),this.startDate(),this.endDate(),this.confirmReturns())));this.message.set('Movimentações de estoque processadas. Confira as situações exibidas na prévia.');this.confirmReturns.set(false);this.previewed.set(true);}catch{this.effectError.set('Não foi possível aplicar os movimentos. Nenhuma atualização parcial do período foi confirmada.');}finally{this.applying.set(false);}}
  statusLabel(status:string):string{return({PENDING:'Pendente de confirmação',APPLIED:'Aplicado',IGNORED:'Ignorado',UNMAPPED:'Sem associação',BASELINE_REQUIRED:'Data inicial necessária',BEFORE_BASELINE:'Anterior à data inicial',UNIT_REVIEW:'Revisar unidade',INSUFFICIENT_STOCK:'Saldo insuficiente',RETURN_REVIEW:'Conferir devolução'} as Record<string,string>)[status]??status;}
  settlementValid():boolean{
    const values=[[this.settlementGross(),false],[this.settlementFees(),false],[this.settlementAdjustments(),true],[this.settlementRetained(),false],[this.settlementReceived(),false]] as const;
    const amountsValid=values.every(([value,negativeAllowed])=>value.trim()===''||(Number.isFinite(Number(value))&&(negativeAllowed||Number(value)>=0)));
    const paidValid=this.settlementStatus()!=='PAGO'||(!!this.settlementPayoutDate()&&this.settlementReceived().trim()!==''&&Number(this.settlementReceived())>=0);
    return !!this.settlementId().trim()&&!!this.settlementStart()&&!!this.settlementEnd()&&this.settlementStart()<=this.settlementEnd()&&amountsValid&&paidValid;
  }
  private amount(value:string):number|null{return value.trim()===''?null:Number(value);}
  moneyOrDash(value:number|null):string{return value===null?'—':new Intl.NumberFormat('pt-BR',{style:'currency',currency:'BRL'}).format(value);}
  async saveSettlement(){if(!this.settlementValid())return;this.settlementSaving.set(true);this.errorMessage.set('');try{
    const saved=await firstValueFrom(this.integrations.saveSettlement(this.provider(),{externalStoreId:this.storeId(),externalSettlementId:this.settlementId().trim(),periodStart:this.settlementStart(),periodEnd:this.settlementEnd(),grossAmount:this.amount(this.settlementGross()),feesAmount:this.amount(this.settlementFees()),adjustmentsAmount:this.amount(this.settlementAdjustments()),retainedAmount:this.amount(this.settlementRetained()),amountReceived:this.amount(this.settlementReceived()),payoutDate:this.settlementPayoutDate()||null,status:this.settlementStatus(),sourceReference:this.settlementReference().trim()||null}));
    this.settlements.update(rows=>[saved,...rows.filter(row=>row.id!==saved.id)]);this.message.set('Repasse registrado. Ele permanece separado do faturamento e do caixa.');
    this.settlementId.set('');this.settlementGross.set('');this.settlementFees.set('');this.settlementAdjustments.set('');this.settlementRetained.set('');this.settlementReceived.set('');this.settlementPayoutDate.set('');this.settlementReference.set('');
  }catch{this.errorMessage.set('Não foi possível registrar o repasse. Confira a referência e o período.');}finally{this.settlementSaving.set(false);}}
}
