import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ApiError } from '../../../core/http/api-error';

export interface ConfirmationDialogData { title: string; message: string; confirmLabel?: string; destructive?: boolean; confirmAction?: () => Promise<unknown>; }
@Component({ selector: 'app-confirmation-dialog', standalone: true, imports: [MatDialogModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule], template: `<div class="dialog-icon" [class.destructive]="data.destructive"><mat-icon>{{ data.destructive ? 'warning' : 'help' }}</mat-icon></div><h2 mat-dialog-title>{{ data.title }}</h2><mat-dialog-content><p>{{ data.message }}</p>@if(error()){<div class="dialog-error" role="alert"><mat-icon>error</mat-icon><span>{{error()}}</span></div>}</mat-dialog-content><mat-dialog-actions align="end"><button mat-button [disabled]="processing()" (click)="ref.close(false)">Voltar</button><button mat-flat-button [disabled]="processing()" [class]="data.destructive ? 'danger-button' : 'primary-button'" (click)="confirm()">@if(processing()){<mat-progress-spinner diameter="18" mode="indeterminate"/>}<span>{{ processing() ? 'Processando…' : (data.confirmLabel ?? 'Confirmar') }}</span></button></mat-dialog-actions>`, styles: [`.dialog-error{display:flex;align-items:flex-start;gap:8px;margin-top:14px;padding:10px 12px;border-radius:8px;background:#fff0ee;color:var(--red-dark);font-size:.78rem}.dialog-error mat-icon{font-size:18px;width:18px;height:18px}.primary-button,.danger-button{display:inline-flex;align-items:center;gap:8px}.primary-button mat-progress-spinner,.danger-button mat-progress-spinner{--mdc-circular-progress-active-indicator-color:white}`], changeDetection: ChangeDetectionStrategy.OnPush })
export class ConfirmationDialogComponent {
  readonly data = inject<ConfirmationDialogData>(MAT_DIALOG_DATA);
  readonly ref = inject(MatDialogRef<ConfirmationDialogComponent>);
  readonly processing = signal(false);
  readonly error = signal<string | null>(null);

  async confirm(): Promise<void> {
    if (!this.data.confirmAction) { this.ref.close(true); return; }
    this.processing.set(true);
    this.error.set(null);
    try {
      await this.data.confirmAction();
      this.ref.close(true);
    } catch (error) {
      this.error.set(error instanceof ApiError ? error.message : 'Não foi possível concluir a operação.');
      this.processing.set(false);
    }
  }
}
