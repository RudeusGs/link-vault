import { CommonModule } from '@angular/common';
import { Component, DoCheck, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from './core/auth/auth.service';
import { ApiService } from './core/http/api.service';
import { WorkspaceService } from './features/settings/data-access/workspace.service';
import { Workspace, WorkspaceInvitation } from './features/settings/models/workspace.model';

type HealthState = 'checking' | 'online' | 'offline';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit, DoCheck {
  protected readonly auth = inject(AuthService);
  protected readonly healthState = signal<HealthState>('checking');
  protected readonly healthMessage = signal('Checking API');
  protected readonly workspaces = signal<Workspace[]>([]);
  protected readonly pendingInvitations = signal<WorkspaceInvitation[]>([]);
  protected readonly workspaceLoading = signal(false);
  protected readonly workspaceError = signal('');
  protected globalKeyword = '';

  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly workspaceService = inject(WorkspaceService);
  private lastAuthState = false;

  ngOnInit(): void {
    this.checkHealth();
    this.lastAuthState = this.auth.isAuthenticated();
    if (this.lastAuthState) {
      this.auth.loadMe().subscribe({ error: () => undefined });
      this.loadWorkspaceContext();
    }
  }

  ngDoCheck(): void {
    const isAuthenticated = this.auth.isAuthenticated();
    if (isAuthenticated && !this.lastAuthState) {
      this.auth.loadMe().subscribe({ error: () => undefined });
      this.loadWorkspaceContext();
    }

    if (!isAuthenticated && this.lastAuthState) {
      this.workspaces.set([]);
      this.pendingInvitations.set([]);
      this.workspaceService.setActiveWorkspace(null);
    }

    this.lastAuthState = isAuthenticated;
  }

  protected logout(): void {
    this.auth.logout();
    this.workspaces.set([]);
    this.pendingInvitations.set([]);
    this.workspaceService.setActiveWorkspace(null);
    this.router.navigate(['/login']);
  }

  protected runGlobalSearch(): void {
    const keyword = this.globalKeyword.trim();
    this.router.navigate(['/resources'], {
      queryParams: keyword ? { q: keyword } : undefined
    });
  }

  protected loadWorkspaceContext(): void {
    if (!this.auth.isAuthenticated()) {
      return;
    }

    this.workspaceLoading.set(true);
    this.workspaceError.set('');

    this.workspaceService.list().subscribe({
      next: (workspaces) => {
        this.workspaces.set(workspaces);
        this.workspaceService.ensureActiveWorkspace(workspaces);
        this.workspaceLoading.set(false);
      },
      error: (error) => {
        this.workspaceLoading.set(false);
        this.workspaceError.set(error instanceof Error ? error.message : 'Could not load workspaces');
      }
    });

    this.refreshInvitations();
  }

  protected refreshInvitations(): void {
    if (!this.auth.isAuthenticated()) {
      return;
    }

    this.workspaceService.pendingInvitations().subscribe({
      next: (invitations) => this.pendingInvitations.set(invitations),
      error: () => this.pendingInvitations.set([])
    });
  }

  protected selectWorkspace(workspace: Workspace): void {
    this.workspaceService.setActiveWorkspace(workspace.id);
    this.router.navigate(['/dashboard']);
  }

  protected activeWorkspace(): Workspace | undefined {
    const activeId = this.workspaceService.activeWorkspaceId();
    return this.workspaces().find((workspace) => workspace.id === activeId) ?? this.workspaces()[0];
  }

  protected acceptInvite(invitation: WorkspaceInvitation): void {
    this.workspaceService.acceptInvitation(invitation.token).subscribe({
      next: (accepted) => {
        this.workspaceService.setActiveWorkspace(accepted.workspaceId);
        this.loadWorkspaceContext();
        this.router.navigate(['/workspaces']);
      },
      error: (error) => this.workspaceError.set(error instanceof Error ? error.message : 'Could not accept invitation')
    });
  }

  protected declineInvite(invitation: WorkspaceInvitation): void {
    this.workspaceService.declineInvitation(invitation.token).subscribe({
      next: () => this.refreshInvitations(),
      error: (error) => this.workspaceError.set(error instanceof Error ? error.message : 'Could not decline invitation')
    });
  }

  get userInitial(): string {
    const user = this.auth.currentUser();
    return (user?.displayName?.[0] || user?.username?.[0] || 'U').toUpperCase();
  }

  protected checkHealth(): void {
    this.healthState.set('checking');
    this.api.getHealth().subscribe({
      next: (response) => {
        this.healthState.set(response.success ? 'online' : 'offline');
        this.healthMessage.set(response.message || 'Backend is online');
      },
      error: (error) => {
        this.healthState.set('offline');
        this.healthMessage.set(error instanceof Error ? error.message : 'Backend is not reachable');
      }
    });
  }
}
