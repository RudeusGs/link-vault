import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { AuthService } from '../../core/auth/auth.service';
import { UserSession } from '../auth/models/auth.model';

@Component({
  selector: 'app-profile-settings',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="lv-card p-4 mb-4">
      <h2 class="lv-section-title mb-3">Profile Information</h2>
      <div class="d-flex align-items-center gap-4">
        <div class="lv-primary-bg text-white rounded-circle d-flex align-items-center justify-content-center fw-bold fs-3" style="width: 80px; height: 80px;">
          {{ getInitial() }}
        </div>
        <div>
          <h3 class="fs-4 mb-1">{{ auth.currentUser()?.displayName || auth.currentUser()?.username }}</h3>
          <p class="lv-muted mb-0">{{ auth.currentUser()?.email }}</p>
          <p class="lv-muted mb-0"><small>Member since {{ auth.currentUser()?.createdAt | date:'longDate' }}</small></p>
        </div>
      </div>
    </div>

    <div class="lv-card p-4">
      <h2 class="lv-section-title mb-3">Active Sessions</h2>
      <p class="lv-muted mb-4">Manage the devices and browsers that are currently signed in to your account.</p>

      <div *ngIf="loading" class="spinner-border spinner-border-sm text-primary"></div>
      <div *ngIf="error" class="alert alert-danger">{{ error }}</div>

      <div *ngIf="!loading && sessions.length > 0" class="list-group list-group-flush">
        <div *ngFor="let session of sessions" class="list-group-item px-0 py-3 d-flex align-items-center justify-content-between">
          <div>
            <strong>{{ parseUserAgent(session.userAgent || '') }}</strong>
            <div class="lv-muted small">
              IP: {{ session.ipAddress || 'Unknown' }} · Last used: {{ session.lastUsedAt | date:'medium' }}
            </div>
            <div *ngIf="session.revokedAt" class="badge bg-danger mt-1">Revoked</div>
          </div>
          <button 
            *ngIf="!session.revokedAt"
            class="btn btn-sm btn-outline-danger" 
            [disabled]="revokingId === session.id"
            (click)="revoke(session.id)">
            <span *ngIf="revokingId === session.id" class="spinner-border spinner-border-sm me-1"></span>
            Revoke
          </button>
        </div>
      </div>
    </div>
  `
})
export class ProfileSettingsComponent implements OnInit {
  protected auth = inject(AuthService);
  protected sessions: UserSession[] = [];
  protected loading = false;
  protected error = '';
  protected revokingId: string | null = null;

  ngOnInit() {
    this.loadSessions();
  }

  loadSessions() {
    this.loading = true;
    this.auth.sessions().subscribe({
      next: (data) => {
        this.sessions = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.message;
        this.loading = false;
      }
    });
  }

  revoke(id: string) {
    if (!confirm('Are you sure you want to revoke this session?')) return;
    this.revokingId = id;
    this.auth.revokeSession(id).subscribe({
      next: () => {
        this.revokingId = null;
        this.loadSessions();
      },
      error: (err) => {
        this.error = err.message;
        this.revokingId = null;
      }
    });
  }

  getInitial() {
    const name = this.auth.currentUser()?.displayName || this.auth.currentUser()?.username || 'U';
    return name.charAt(0).toUpperCase();
  }

  parseUserAgent(ua: string) {
    if (!ua) return 'Unknown Device';
    if (ua.includes('Windows')) return 'Windows PC';
    if (ua.includes('Mac OS')) return 'Mac';
    if (ua.includes('Linux')) return 'Linux';
    if (ua.includes('Android')) return 'Android Device';
    if (ua.includes('iPhone') || ua.includes('iPad')) return 'iOS Device';
    return ua.substring(0, 30) + (ua.length > 30 ? '...' : '');
  }
}
