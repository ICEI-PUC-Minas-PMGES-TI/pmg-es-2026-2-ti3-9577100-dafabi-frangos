import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({ selector: 'app-loading-state', standalone: true, template: `<div class="skeleton-wrap" role="status" aria-live="polite"><span class="sr-only">{{ label() }}</span>@for (item of rows; track item) { <div class="skeleton-row"><span></span><span></span><span></span></div> }</div>`, changeDetection: ChangeDetectionStrategy.OnPush })
export class LoadingStateComponent { readonly label = input('Carregando dados…'); readonly rows = [1, 2, 3, 4]; }
