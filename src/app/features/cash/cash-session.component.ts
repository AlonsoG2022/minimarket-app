import { CommonModule } from '@angular/common';
import { ChangeDetectorRef, Component, OnInit, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../../core/services/auth.service';
import { CashSessionsService } from '../../core/services/cash-sessions.service';
import { CashSession } from '../../core/models/minimarket.models';
import { SolesPricePipe } from '../../shared/pipes/soles-price.pipe';

@Component({
  selector: 'app-cash-session',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, SolesPricePipe],
  templateUrl: './cash-session.component.html',
  styleUrl: './cash-session.component.css'
})
export class CashSessionComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly cdr = inject(ChangeDetectorRef);
  readonly authService = inject(AuthService);
  private readonly cashSessionsService = inject(CashSessionsService);

  readonly openForm = this.fb.nonNullable.group({
    openingAmount: [0, [Validators.required, Validators.min(0)]],
    notes: ['']
  });

  readonly movementForm = this.fb.nonNullable.group({
    type: ['ingreso', Validators.required],
    amount: [0, [Validators.required, Validators.min(0.01)]],
    description: ['', Validators.required]
  });

  readonly closeForm = this.fb.nonNullable.group({
    countedAmount: [0, [Validators.required, Validators.min(0)]],
    notes: ['']
  });

  currentSession?: CashSession | null;
  recentSessions: CashSession[] = [];
  loading = true;
  loadingHistory = true;
  message = '';
  error = '';

  ngOnInit(): void {
    this.loadCurrentSession();
    this.loadRecentSessions();
  }

  get hasOpenSession(): boolean {
    return !!this.currentSession && this.currentSession.status.toLowerCase() === 'abierta';
  }

  openCash(): void {
    const currentUser = this.authService.session();
    if (!currentUser || this.openForm.invalid) {
      this.openForm.markAllAsTouched();
      return;
    }

    const value = this.openForm.getRawValue();
    this.cashSessionsService.open({
      userId: currentUser.id,
      openingAmount: Number(value.openingAmount),
      notes: value.notes || null
    }).subscribe({
      next: (session) => {
        this.currentSession = session;
        this.closeForm.patchValue({ countedAmount: session.currentAmount });
        this.message = 'Caja abierta correctamente.';
        this.error = '';
        this.openForm.reset({ openingAmount: 0, notes: '' });
        this.loadRecentSessions();
        this.cdr.detectChanges();
      },
      error: (response) => {
        this.error = response.error?.message ?? 'No se pudo abrir la caja.';
        this.message = '';
        this.cdr.detectChanges();
      }
    });
  }

  registerMovement(): void {
    const currentUser = this.authService.session();
    if (!currentUser || !this.currentSession || this.movementForm.invalid) {
      this.movementForm.markAllAsTouched();
      return;
    }

    const value = this.movementForm.getRawValue();
    this.cashSessionsService.addMovement(this.currentSession.id, {
      userId: currentUser.id,
      type: value.type,
      amount: Number(value.amount),
      description: value.description
    }).subscribe({
      next: (session) => {
        this.currentSession = session;
        this.closeForm.patchValue({ countedAmount: session.currentAmount });
        this.message = 'Movimiento registrado correctamente.';
        this.error = '';
        this.movementForm.reset({ type: 'ingreso', amount: 0, description: '' });
        this.loadRecentSessions();
        this.cdr.detectChanges();
      },
      error: (response) => {
        this.error = response.error?.message ?? 'No se pudo registrar el movimiento.';
        this.message = '';
        this.cdr.detectChanges();
      }
    });
  }

  closeCash(): void {
    const currentUser = this.authService.session();
    if (!currentUser || !this.currentSession || this.closeForm.invalid) {
      this.closeForm.markAllAsTouched();
      return;
    }

    const value = this.closeForm.getRawValue();
    this.cashSessionsService.close(this.currentSession.id, {
      userId: currentUser.id,
      countedAmount: Number(value.countedAmount),
      notes: value.notes || null
    }).subscribe({
      next: (session) => {
        this.currentSession = session;
        this.message = 'Caja cerrada correctamente.';
        this.error = '';
        this.loadRecentSessions();
        this.cdr.detectChanges();
      },
      error: (response) => {
        this.error = response.error?.message ?? 'No se pudo cerrar la caja.';
        this.message = '';
        this.cdr.detectChanges();
      }
    });
  }

  private loadCurrentSession(): void {
    const currentUser = this.authService.session();
    if (!currentUser) {
      this.loading = false;
      return;
    }

    this.loading = true;
    this.cashSessionsService.getCurrent(currentUser.id, true).subscribe({
      next: (session) => {
        this.currentSession = session;
        if (session) {
          this.closeForm.patchValue({ countedAmount: session.currentAmount, notes: '' });
        }
        this.loading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.error = 'No se pudo cargar el estado actual de la caja.';
        this.loading = false;
        this.cdr.detectChanges();
      }
    });
  }

  private loadRecentSessions(): void {
    const currentUser = this.authService.session();
    if (!currentUser) {
      this.loadingHistory = false;
      return;
    }

    this.loadingHistory = true;
    this.cashSessionsService.getRecent(currentUser.id).subscribe({
      next: (sessions) => {
        this.recentSessions = sessions;
        this.loadingHistory = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.loadingHistory = false;
        this.cdr.detectChanges();
      }
    });
  }
}
