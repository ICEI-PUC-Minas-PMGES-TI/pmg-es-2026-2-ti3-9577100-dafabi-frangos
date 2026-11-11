import { ChangeDetectionStrategy, Component } from '@angular/core';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { ImportWorkflowComponent } from '../../components/import-workflow/import-workflow.component';
import { MarketplaceStockWorkflowComponent } from '../../components/marketplace-stock-workflow/marketplace-stock-workflow.component';

@Component({
  selector: 'app-ifood', standalone: true,
  imports: [PageHeaderComponent, ImportWorkflowComponent, MarketplaceStockWorkflowComponent],
  template: `
    <app-page-header eyebrow="Integrações" title="iFood" subtitle="Importe relatórios financeiros de pedidos e revise o resultado antes de gravar." />
    <app-import-workflow [provider]="'IFOOD'" />
    <app-marketplace-stock-workflow [provider]="'IFOOD'" />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class IfoodComponent {}
