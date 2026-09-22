import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface ConfirmationDialogData { title: string; message: string; confirmLabel?: string; destructive?: boolean; }
@Component({ selector: 'app-confirmation-dialog', standalone: true, imports: [MatDialogModule, MatButtonModule, MatIconModule], template: `<div class="dialog-icon" [class.destructive]="data.destructive"><mat-icon>{{ data.destructive ? 'warning' : 'help' }}</mat-icon></div><h2 mat-dialog-title>{{ data.title }}</h2><mat-dialog-content>{{ data.message }}</mat-dialog-content><mat-dialog-actions align="end"><button mat-button (click)="ref.close(false)">Voltar</button><button mat-flat-button [class]="data.destructive ? 'danger-button' : 'primary-button'" (click)="ref.close(true)">{{ data.confirmLabel ?? 'Confirmar' }}</button></mat-dialog-actions>`, changeDetection: ChangeDetectionStrategy.OnPush })
export class ConfirmationDialogComponent { readonly data = inject<ConfirmationDialogData>(MAT_DIALOG_DATA); readonly ref = inject(MatDialogRef<ConfirmationDialogComponent>); }
