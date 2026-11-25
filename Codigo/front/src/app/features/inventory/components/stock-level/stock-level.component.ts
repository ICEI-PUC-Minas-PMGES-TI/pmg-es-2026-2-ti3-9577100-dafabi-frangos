import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { QuantityPipe } from '../../../../shared/pipes/quantity.pipe';

/** Saldo do produto com alerta de saldo que não pode ser vendido (lote vencido ou sem lote). */
@Component({
  selector: 'app-stock-level',
  standalone: true,
  imports: [QuantityPipe],
  template: `
    <div class="stock-level"><strong>{{ quantity() | qty }}</strong><span>{{ unit() }}</span></div>
    @if (expired() > 0 || untracked() > 0) {
      <div class="stock-alert" role="note">
        @if (expired() > 0) { <span>{{ expired() | qty }} vencido</span> }
        @if (untracked() > 0) { <span>{{ untracked() | qty }} sem lote</span> }
        <span>· vendável {{ sellable() | qty }}</span>
      </div>
    }`,
  styles: [`
    .stock-level{display:flex;align-items:baseline;gap:4px}
    .stock-level strong{font-size:1.05rem}
    .stock-level span{color:var(--muted);font-size:.72rem}
    .stock-alert{display:flex;flex-wrap:wrap;gap:4px;margin-top:3px;font-size:.68rem;font-weight:700;color:var(--red-dark)}
  `],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class StockLevelComponent {
  readonly quantity = input.required<number>();
  readonly unit = input.required<string>();
  readonly sellable = input<number>(0);
  readonly expired = input<number>(0);
  readonly untracked = input<number>(0);
}
