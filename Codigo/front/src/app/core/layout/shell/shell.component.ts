import { ChangeDetectionStrategy, Component, HostListener, signal } from '@angular/core';
import { NavigationEnd, Router, RouterOutlet } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { filter } from 'rxjs';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { TopbarComponent } from '../topbar/topbar.component';
import { BreadcrumbsComponent } from '../../../shared/components/breadcrumbs/breadcrumbs.component';

@Component({ selector: 'app-shell', standalone: true, imports: [RouterOutlet, MatSidenavModule, SidebarComponent, TopbarComponent, BreadcrumbsComponent], template: `
  <mat-sidenav-container class="app-shell" autosize>
    <mat-sidenav #drawer [mode]="isTablet() ? 'over' : 'side'" [opened]="!isTablet()" [disableClose]="!isTablet()" [style.width.px]="collapsed() ? 88 : 264"><app-sidebar [collapsed]="collapsed() && !isTablet()" (navigate)="isTablet() && drawer.close()" /></mat-sidenav>
    <mat-sidenav-content>
      <app-topbar (menuClick)="isTablet() ? drawer.toggle() : collapsed.set(!collapsed())" />
      <main id="conteudo-principal" tabindex="-1"><app-breadcrumbs/><router-outlet /></main>
    </mat-sidenav-content>
  </mat-sidenav-container>`, changeDetection: ChangeDetectionStrategy.OnPush })
export class ShellComponent {
  readonly collapsed = signal(false); readonly isTablet = signal(window.innerWidth < 1100);
  constructor(router: Router) { router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe(() => document.getElementById('conteudo-principal')?.focus({ preventScroll: true })); }
  @HostListener('window:resize') onResize(): void { this.isTablet.set(window.innerWidth < 1100); }
}
