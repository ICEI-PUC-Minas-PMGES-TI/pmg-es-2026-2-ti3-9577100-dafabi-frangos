import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

@Component({ selector: 'app-status-chip', standalone: true, template: `<span class="status-chip" [class]="tone()"><span aria-hidden="true">{{ icon() }}</span>{{ label() }}</span>`, changeDetection: ChangeDetectionStrategy.OnPush })
export class StatusChipComponent {
  readonly status = input.required<string>();
  readonly label = computed(() => ({ ACTIVE: 'Ativo', INACTIVE: 'Inativo', COMPLETED: 'Concluída', CANCELED: 'Cancelada', REFUNDED: 'Estornada', OPEN: 'Aberto', CLOSED: 'Fechado', CONNECTED: 'Conectado', UNAVAILABLE: 'Indisponível', PENDING: 'Pendente', CONSOLIDATED: 'Consolidada', REGISTERED: 'Registrada', VALID: 'Válido', EXPIRED: 'Vencido', USED: 'Utilizado' }[this.status()] ?? this.status()));
  readonly tone = computed(() => ['ACTIVE','COMPLETED','CONNECTED','REGISTERED','VALID','OPEN'].includes(this.status()) ? 'success' : ['PENDING','EXPIRED'].includes(this.status()) ? 'warning' : 'neutral');
  readonly icon = computed(() => this.tone() === 'success' ? '●' : this.tone() === 'warning' ? '◆' : '○');
}
