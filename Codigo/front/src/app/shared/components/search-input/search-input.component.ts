import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';

@Component({ selector: 'app-search-input', standalone: true, imports: [MatIconModule], template: `<label class="search-box"><span class="sr-only">{{ label() }}</span><mat-icon>search</mat-icon><input type="search" [placeholder]="placeholder()" [value]="value()" (input)="changed.emit($any($event.target).value)" /></label>`, changeDetection: ChangeDetectionStrategy.OnPush })
export class SearchInputComponent { readonly label = input('Pesquisar'); readonly placeholder = input('Pesquisar…'); readonly value = input(''); readonly changed = output<string>(); }
