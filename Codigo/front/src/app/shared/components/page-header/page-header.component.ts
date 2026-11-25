import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-page-header', standalone: true, imports: [MatIconModule, MatButtonModule, RouterLink],
  template: `
    <header class="page-header">
      <div><p class="eyebrow">{{ eyebrow() }}</p><h1>{{ title() }}</h1>@if (subtitle()) { <p class="subtitle">{{ subtitle() }}</p> }</div>
      <div class="page-header-actions"><ng-content select="[headerActions]"/>@if (actionLabel()) { @if (actionLink()) { <a mat-flat-button class="primary-button" [routerLink]="actionLink()"><mat-icon>{{ actionIcon() }}</mat-icon>{{ actionLabel() }}</a> } @else { <button mat-flat-button class="primary-button" type="button" (click)="action.emit()"><mat-icon>{{ actionIcon() }}</mat-icon>{{ actionLabel() }}</button> } }</div>
    </header>`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PageHeaderComponent {
  readonly title = input.required<string>(); readonly subtitle = input(''); readonly eyebrow = input('DaFabi Frangos');
  readonly actionLabel = input(''); readonly actionIcon = input('add');
  /** Quando informado, a ação principal é um link para esta rota (em vez de emitir `action`). */
  readonly actionLink = input<string | null>(null); readonly action = output<void>();
}
