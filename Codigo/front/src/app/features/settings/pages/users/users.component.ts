import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { AuthService } from '../../../../core/auth/auth.service';
import { User } from '../../../../core/models/domain.models';
import { EmptyStateComponent } from '../../../../shared/components/empty-state/empty-state.component';
import { ErrorStateComponent } from '../../../../shared/components/error-state/error-state.component';
import { LoadingStateComponent } from '../../../../shared/components/loading-state/loading-state.component';
import { PageHeaderComponent } from '../../../../shared/components/page-header/page-header.component';
import { StatusChipComponent } from '../../../../shared/components/status-chip/status-chip.component';
import { UserCreateDialogComponent } from '../../components/user-create-dialog/user-create-dialog.component';
import { SettingsNavComponent } from '../../components/settings-nav/settings-nav.component';
import { SettingsService } from '../../services/settings.service';

@Component({
  selector: 'app-users', standalone: true,
  imports: [MatButtonModule, MatIconModule, MatSlideToggleModule, PageHeaderComponent, StatusChipComponent,
    SettingsNavComponent, EmptyStateComponent, ErrorStateComponent, LoadingStateComponent],
  template: `<app-page-header eyebrow="Configurações" title="Usuários" subtitle="Controle quem pode acessar o sistema e o perfil de cada pessoa."/><app-settings-nav/>
    @if(loading()){<section class="surface"><app-loading-state label="Carregando usuários…"/></section>}
    @else if(error()){<app-error-state title="Usuários indisponíveis" message="Não foi possível carregar ou atualizar os usuários."/><div class="retry"><button mat-stroked-button (click)="load()">Tentar novamente</button></div>}
    @else if(!users().length){<section class="surface"><app-empty-state icon="group" title="Nenhum usuário cadastrado" message="Cadastre os acessos da equipe da loja."/></section>}
    @else{<section class="surface"><div class="surface-header"><div><h2>Usuários cadastrados</h2><span class="cell-meta">{{users().length}} pessoas</span></div><button mat-flat-button class="primary-button" (click)="addUser()"><mat-icon>person_add</mat-icon>Novo usuário</button></div><div class="table-wrap"><table class="data-table"><thead><tr><th>Usuário</th><th>E-mail</th><th>Perfil</th><th>Status</th><th>Acesso</th></tr></thead><tbody>@for(u of users();track u.id){<tr><td><div class="user-cell"><span>{{initials(u.name)}}</span><strong>{{u.name}}</strong></div></td><td>{{u.email}}</td><td>{{u.role==='ADMIN'?'Administradora':'Operador de caixa'}}</td><td><app-status-chip [status]="u.active?'ACTIVE':'INACTIVE'"/></td><td><mat-slide-toggle [checked]="u.active" [disabled]="savingId()===u.id||(u.id===auth.currentUser()?.id&&u.active)" (change)="toggle(u)" [attr.aria-label]="'Alterar acesso de '+u.name">{{u.active?'Liberado':'Bloqueado'}}</mat-slide-toggle></td></tr>}</tbody></table></div></section>}`,
  styles: [`.user-cell{display:flex;align-items:center;gap:10px}.user-cell span{width:34px;height:34px;border-radius:50%;background:var(--yellow-light);color:var(--red-dark);display:grid;place-items:center;font-size:.68rem;font-weight:800}.retry{text-align:center;margin:12px}`],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class UsersComponent {
  readonly service = inject(SettingsService);
  readonly auth = inject(AuthService);
  private readonly dialog = inject(MatDialog);
  private readonly snack = inject(MatSnackBar);
  readonly users = signal<User[]>([]);
  readonly loading = signal(true);
  readonly error = signal(false);
  readonly savingId = signal<string | null>(null);

  constructor() { void this.load(); }

  async load(): Promise<void> {
    this.loading.set(true);
    this.error.set(false);
    try { this.users.set(await this.service.listUsers()); }
    catch { this.error.set(true); }
    finally { this.loading.set(false); }
  }

  initials(name: string): string { return name.split(' ').slice(0, 2).map(part => part[0]).join(''); }

  async toggle(user: User): Promise<void> {
    this.savingId.set(user.id);
    try {
      const updated = await this.service.setUserActive(user.id, !user.active);
      this.users.update(items => items.map(item => item.id === updated.id ? updated : item));
      this.snack.open(updated.active ? 'Acesso do usuário liberado.' : 'Acesso do usuário bloqueado.', 'Fechar', { duration: 3000 });
    } catch {
      this.snack.open('Não foi possível atualizar o acesso. Tente novamente.', 'Fechar', { duration: 4000 });
    } finally { this.savingId.set(null); }
  }

  addUser(): void {
    this.dialog.open(UserCreateDialogComponent).afterClosed().subscribe(async value => {
      if (!value) return;
      this.loading.set(true);
      try {
        const created = await this.service.createUser(value);
        this.users.update(items => [...items, created].sort((a, b) => a.name.localeCompare(b.name, 'pt-BR')));
        this.snack.open('Usuário cadastrado com sucesso.', 'Fechar', { duration: 3500 });
      } catch {
        this.snack.open('Não foi possível cadastrar. Verifique se o e-mail já está em uso.', 'Fechar', { duration: 4500 });
      } finally { this.loading.set(false); }
    });
  }
}
