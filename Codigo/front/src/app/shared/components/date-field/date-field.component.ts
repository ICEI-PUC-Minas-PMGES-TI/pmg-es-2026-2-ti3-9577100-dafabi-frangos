import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { fromDateInputValue, toDateInputValue } from '../../utils/date.utils';

/**
 * Campo de data no padrão Material (contorno + calendário) para filtros.
 * Recebe e devolve a data como texto `yyyy-MM-dd`, igual ao antigo `<input type="date">`.
 */
@Component({
  selector: 'app-date-field', standalone: true,
  imports: [MatDatepickerModule, MatFormFieldModule, MatInputModule],
  template: `<mat-form-field appearance="outline" class="filter-field" subscriptSizing="dynamic">
    <mat-label>{{ label() }}</mat-label>
    <input matInput [matDatepicker]="picker" [value]="date()" [min]="minDate()" [max]="maxDate()" placeholder="dd/mm/aaaa" (dateChange)="emit($event.value)">
    <mat-datepicker-toggle matIconSuffix [for]="picker" [attr.aria-label]="'Abrir calendário: ' + label()"/>
    <mat-datepicker #picker/>
  </mat-form-field>`,
  styles: [`:host{display:block;width:170px}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DateFieldComponent {
  readonly label = input.required<string>();
  readonly value = input('');
  readonly min = input('');
  readonly max = input('');
  readonly changed = output<string>();
  readonly date = computed(() => fromDateInputValue(this.value()));
  readonly minDate = computed(() => fromDateInputValue(this.min()));
  readonly maxDate = computed(() => fromDateInputValue(this.max()));

  emit(value: Date | null): void {
    this.changed.emit(value ? toDateInputValue(value) : '');
  }
}
