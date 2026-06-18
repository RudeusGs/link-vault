import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { WorkspaceService } from '../settings/data-access/workspace.service';

@Component({
  selector: 'app-invitation-accept',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <main class="lv-auth-bg d-flex flex-column align-items-center justify-content-center py-5 px-3">
      <section class="lv-auth-card p-4 p-md-5">
        <div class="text-center mb-4">
          <a class="lv-brand justify-content-center text-decoration-none" routerLink="/">
            <span class="lv-brand-mark"><span class="material-symbols-outlined" style="font-size:18px">lock</span></span>
            <span>LinkVault</span>
          </a>
        </div>

        <div *ngIf="error" class="alert alert-danger">{{ error }}</div>

        <div class="text-center" *ngIf="!success">
          <span class="lv-icon-box lv-icon-box-lg mx-auto mb-3">
            <span class="material-symbols-outlined" style="font-size:28px">group_add</span>
          </span>
          <h1 class="lv-section-title mb-2">Workspace invitation</h1>
          <p class="lv-muted mb-4">
            Accepting this invite will add your current account to the workspace if the invite matches your username or email.
          </p>
          <div class="d-flex gap-2">
            <button class="btn lv-button-quiet flex-fill" [disabled]="processing" type="button" (click)="decline()">Decline</button>
            <button class="btn btn-primary flex-fill" [disabled]="processing" type="button" (click)="accept()">
              <span *ngIf="processing" class="spinner-border spinner-border-sm me-2"></span>
              Accept
            </button>
          </div>
        </div>

        <div class="text-center" *ngIf="success">
          <span class="lv-icon-box lv-icon-box-lg mx-auto mb-3">
            <span class="material-symbols-outlined" style="font-size:28px">check_circle</span>
          </span>
          <h1 class="lv-section-title mb-2">You joined the workspace</h1>
          <p class="lv-muted mb-4">The workspace is now active. You can open its vaults and resources immediately.</p>
          <a routerLink="/workspaces" class="btn btn-primary w-100">Open workspace</a>
        </div>
      </section>
    </main>
  `
})
export class InvitationAcceptComponent implements OnInit {
  protected processing = false;
  protected error = '';
  protected success = false;
  protected token = '';

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly wsService = inject(WorkspaceService);
  private readonly authService = inject(AuthService);

  ngOnInit(): void {
    this.token = this.route.snapshot.paramMap.get('token') || '';
    if (!this.token) {
      this.error = 'No invitation token provided.';
      return;
    }

    if (!this.authService.isAuthenticated()) {
      this.router.navigate(['/login'], { queryParams: { returnUrl: `/invitations/${this.token}` } });
    }
  }

  protected accept(): void {
    if (!this.token) return;
    this.processing = true;
    this.error = '';
    this.wsService.acceptInvitation(this.token).subscribe({
      next: (invitation) => {
        this.wsService.setActiveWorkspace(invitation.workspaceId);
        this.success = true;
        this.processing = false;
      },
      error: (err) => {
        this.error = err instanceof Error ? err.message : 'Could not accept invitation';
        this.processing = false;
      }
    });
  }

  protected decline(): void {
    if (!this.token) return;
    this.processing = true;
    this.error = '';
    this.wsService.declineInvitation(this.token).subscribe({
      next: () => this.router.navigate(['/dashboard']),
      error: (err) => {
        this.error = err instanceof Error ? err.message : 'Could not decline invitation';
        this.processing = false;
      }
    });
  }
}
