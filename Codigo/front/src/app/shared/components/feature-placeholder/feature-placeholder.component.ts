import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { PageHeaderComponent } from '../page-header/page-header.component';

@Component({ selector: 'app-feature-placeholder', standalone: true, imports: [MatIconModule, PageHeaderComponent], template: `<app-page-header [eyebrow]="data['eyebrow'] ?? 'DaFabi Frangos'" [title]="data['title'] ?? 'Área do sistema'" [subtitle]="data['subtitle'] ?? 'Conteúdo em preparação.'"/><section class="surface state-card"><mat-icon>{{data['icon']??'construction'}}</mat-icon><h2>Primeira versão navegável</h2><p>Este fluxo está disponível no menu e receberá seus dados simulados na próxima etapa.</p></section>`, changeDetection: ChangeDetectionStrategy.OnPush })
export class FeaturePlaceholderComponent { readonly data = inject(ActivatedRoute).snapshot.data; }
