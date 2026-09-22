import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({ selector: 'app-error-state', standalone: true, imports: [MatButtonModule, MatIconModule, RouterLink], template: `<div class="state-card error-state"><mat-icon>{{ denied ? 'lock' : 'cloud_off' }}</mat-icon><h2>{{ denied ? 'Acesso não permitido' : title() }}</h2><p>{{ denied ? 'Seu perfil não tem permissão para acessar esta área.' : message() }}</p><a mat-flat-button class="primary-button" routerLink="/app/dashboard">Voltar ao início</a></div>`, changeDetection: ChangeDetectionStrategy.OnPush })
export class ErrorStateComponent { readonly route = inject(ActivatedRoute); readonly denied = !!this.route.snapshot.data['denied']; readonly title = input('Não foi possível carregar'); readonly message = input('Tente novamente em alguns instantes.'); }
