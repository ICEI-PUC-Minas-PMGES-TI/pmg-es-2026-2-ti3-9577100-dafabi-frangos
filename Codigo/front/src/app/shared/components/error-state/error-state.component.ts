import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthService } from '../../../core/auth/auth.service';

@Component({ selector: 'app-error-state', standalone: true, imports: [MatButtonModule, MatIconModule, RouterLink], template: `<div class="state-card error-state"><mat-icon>{{ denied ? 'lock' : notFound ? 'travel_explore' : 'cloud_off' }}</mat-icon><h2>{{ denied ? 'Acesso não permitido' : notFound ? 'Página não encontrada' : title() }}</h2><p>{{ denied ? 'Seu perfil não tem permissão para acessar esta área.' : notFound ? 'O endereço acessado não existe. Confira o link ou volte ao início.' : message() }}</p><a mat-flat-button class="primary-button" [routerLink]="auth.homeRoute()">Voltar ao início</a></div>`, changeDetection: ChangeDetectionStrategy.OnPush })
export class ErrorStateComponent { readonly route = inject(ActivatedRoute); readonly auth = inject(AuthService); readonly denied = !!this.route.snapshot.data['denied']; readonly notFound = !!this.route.snapshot.data['notFound']; readonly title = input('Não foi possível carregar'); readonly message = input('Tente novamente em alguns instantes.'); }
