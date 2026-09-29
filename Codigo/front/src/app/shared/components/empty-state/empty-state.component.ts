import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({ selector: 'app-empty-state', standalone: true, imports: [RouterLink, MatButtonModule, MatIconModule], template: `<div class="state-card"><mat-icon>{{ icon() }}</mat-icon><h2>{{ title() }}</h2><p>{{ message() }}</p>@if (actionLabel() && actionLink()) { <a mat-stroked-button [routerLink]="actionLink()">{{ actionLabel() }}</a> } @else if (actionLabel()) { <button mat-stroked-button (click)="action.emit()">{{ actionLabel() }}</button> }</div>`, changeDetection: ChangeDetectionStrategy.OnPush })
export class EmptyStateComponent { readonly icon = input('inventory_2'); readonly title = input('Nada por aqui'); readonly message = input('Não encontramos registros para exibir.'); readonly actionLabel = input(''); readonly actionLink = input<string | null>(null); readonly action = output<void>(); }
