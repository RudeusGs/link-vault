import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { WorkspaceService } from '../settings/data-access/workspace.service';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-invitation-accept',
  standalone: true,
  imports: [CommonModule, RouterModule],
  template: `
    <div class="lv-auth-bg d-flex flex-column align-items-center justify-content-center py-5">
      <div class="lv-auth-card p-4 p-md-5">
        <div class="text-center mb-4">
          <a class="lv-brand text-decoration-none d-inline-flex align-items-center gap-2" routerLink="/">
            <span class="material-symbols-outlined fs-2">lock</span>
            LinkVault
          </a>
        </div>

        <div *ngIf="loading" class="text-center py-4">
          <div class="spinner-border text-primary" role="status"></div>
          <p class="mt-3 text-muted">Processing invitation...</p>
        </div>

        <div *ngIf="!loading && error" class="text-center">
          <span class="material-symbols-outlined text-danger mb-3" style="font-size: 48px;">error</span>
          <h4 class="mb-3">Invalid or Expired Invitation</h4>
          <p class="text-muted mb-4">{{ error }}</p>
          <a routerLink="/dashboard" class="btn btn-primary w-100">Go to Dashboard</a>
        </div>

        <div *ngIf="!loading && success" class="text-center">
          <span class="material-symbols-outlined text-success mb-3" style="font-size: 48px;">check_circle</span>
          <h4 class="mb-3">Invitation Accepted!</h4>
          <p class="text-muted mb-4">You have successfully joined the workspace.</p>
          <a routerLink="/dashboard" class="btn btn-primary w-100">Go to Dashboard</a>
        </div>

        <div *ngIf="!loading && !error && !success" class="text-center">
          <span class="material-symbols-outlined text-primary mb-3" style="font-size: 48px;">group_add</span>
          <h4 class="mb-3">You've been invited!</h4>
          <p class="text-muted mb-4">You have been invited to join a workspace.</p>
          
          <div class="d-flex gap-3 mt-4">
            <button class="btn btn-outline-danger w-50" [disabled]="processing" (click)="decline()">Decline</button>
            <button class="btn btn-primary w-50" [disabled]="processing" (click)="accept()">Accept Invitation</button>
          </div>
        </div>
      </div>
    </div>
  `
})
export class InvitationAcceptComponent implements OnInit {
  protected loading = false;
  protected processing = false;
  protected error = '';
  protected success = false;
  protected token = '';

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly wsService = inject(WorkspaceService);
  private readonly authService = inject(AuthService);

  ngOnInit() {
    this.token = this.route.snapshot.paramMap.get('token') || '';
    if (!this.token) {
      this.error = 'No invitation token provided.';
    }

    if (!this.authService.isAuthenticated()) {
      // User must be logged in to accept an invitation
      this.router.navigate(['/login'], { queryParams: { returnUrl: `/invitations/${this.token}` } });
    }
  }

  accept() {
    this.processing = true;
    this.error = '';
    this.wsService.acceptInvitation(this.token).subscribe({
      next: () => {
        this.success = true;
        this.processing = false;
      },
      error: (err) => {
        this.error = err.message;
        this.processing = false;
      }
    });
  }

  decline() {
    this.processing = true;
    this.error = '';
    this.wsService.declineInvitation(this.token).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err) => {
        this.error = err.message;
        this.processing = false;
      }
    });
  }
}
