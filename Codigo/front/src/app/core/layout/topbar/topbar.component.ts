import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PasswordDialogComponent } from '../../../features/settings/components/password-dialog/password-dialog.component';
import { SettingsService } from '../../../features/settings/services/settings.service';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { AuthService } from '../../auth/auth.service';

@Component({ selector: 'app-topbar', standalone: true, imports: [MatButtonModule, MatIconModule, MatMenuModule], template: `
  <header class="topbar">
    <button mat-icon-button (click)="menuClick.emit()" aria-label="Recolher ou abrir menu"><mat-icon>menu</mat-icon></button>
    <div class="topbar-context"><span>Operação de hoje</span><strong>{{ today }}</strong></div>
    <span class="topbar-spacer"></span>
    <button mat-button [matMenuTriggerFor]="userMenu" class="user-menu" aria-label="Abrir menu do usuário"><span class="avatar">{{ initials }}</span><span class="user-copy"><strong>{{ auth.currentUser()?.name }}</strong><small>{{ auth.isAdmin() ? 'Administradora' : 'Operador de caixa' }}</small></span><mat-icon>expand_more</mat-icon></button>
    <mat-menu #userMenu="matMenu"><button mat-menu-item (click)="changePassword()"><mat-icon>key</mat-icon><span>Trocar minha senha</span></button><button mat-menu-item (click)="auth.logout()"><mat-icon>logout</mat-icon><span>Sair do sistema</span></button></mat-menu>
  </header>`, changeDetection: ChangeDetectionStrategy.OnPush })
export class TopbarComponent {
  readonly menuClick = output<void>(); readonly compact = input(false);
  readonly today = new Intl.DateTimeFormat('pt-BR', { weekday: 'short', day: '2-digit', month: 'long' }).format(new Date()).replace('.', '');
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  private readonly settings = inject(SettingsService);
  constructor(readonly auth: AuthService) {}

  changePassword(): void {
    this.dialog.open(PasswordDialogComponent, { data: {
      mode: 'own',
      submit: ({ currentPassword, newPassword }: { currentPassword: string; newPassword: string }) =>
        this.settings.changeOwnPassword(currentPassword, newPassword)
    } }).afterClosed().subscribe(done => { if (done) this.snack.open('Senha alterada.', 'Fechar', { duration: 3000 }); });
  }
  get initials(): string { return this.auth.currentUser()?.name.split(' ').slice(0, 2).map(word => word[0]).join('') ?? 'DF'; }
}
