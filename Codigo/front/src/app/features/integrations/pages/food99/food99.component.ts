import { ChangeDetectionStrategy, Component } from '@angular/core';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { ImportWorkflowComponent } from '../../components/import-workflow/import-workflow.component';
import { MarketplaceStockWorkflowComponent } from '../../components/marketplace-stock-workflow/marketplace-stock-workflow.component';

@Component({
  selector: 'app-food99', standalone: true,
  imports: [PageHeaderComponent, ImportWorkflowComponent, MarketplaceStockWorkflowComponent],
  template: `
    <app-page-header eyebrow="Integrações" title="99Food" subtitle="Importe o relatório exportado do portal e revise os dados antes de gravar." />
    <app-import-workflow [provider]="'FOOD_99'" />
    <app-marketplace-stock-workflow [provider]="'FOOD_99'" />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class Food99Component {}
