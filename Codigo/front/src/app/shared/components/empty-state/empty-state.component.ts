import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({ selector: 'app-empty-state', standalone: true, imports: [MatButtonModule, MatIconModule], template: `<div class="state-card"><mat-icon>{{ icon() }}</mat-icon><h2>{{ title() }}</h2><p>{{ message() }}</p>@if (actionLabel()) { <button mat-stroked-button (click)="action.emit()">{{ actionLabel() }}</button> }</div>`, changeDetection: ChangeDetectionStrategy.OnPush })
export class EmptyStateComponent { readonly icon = input('inventory_2'); readonly title = input('Nada por aqui'); readonly message = input('Não encontramos registros para exibir.'); readonly actionLabel = input(''); readonly action = output<void>(); }
