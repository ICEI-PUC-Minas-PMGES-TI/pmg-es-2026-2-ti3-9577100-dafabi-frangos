import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-page-header', standalone: true, imports: [MatIconModule, MatButtonModule],
  template: `
    <header class="page-header">
      <div><p class="eyebrow">{{ eyebrow() }}</p><h1>{{ title() }}</h1>@if (subtitle()) { <p class="subtitle">{{ subtitle() }}</p> }</div>
      @if (actionLabel()) { <button mat-flat-button class="primary-button" type="button" (click)="action.emit()"><mat-icon>{{ actionIcon() }}</mat-icon>{{ actionLabel() }}</button> }
    </header>`,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PageHeaderComponent {
  readonly title = input.required<string>(); readonly subtitle = input(''); readonly eyebrow = input('DaFabi Frangos');
  readonly actionLabel = input(''); readonly actionIcon = input('add'); readonly action = output<void>();
}
