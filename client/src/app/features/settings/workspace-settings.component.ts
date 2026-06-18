import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';
import { WorkspaceService } from './data-access/workspace.service';
import {
  Workspace,
  WorkspaceInvitation,
  WorkspaceMember,
  WorkspaceRequest,
  WorkspaceRole
} from './models/workspace.model';

@Component({
  selector: 'app-workspace-settings',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="lv-workspace-page">
      <header class="lv-workspace-hero">
        <div class="lv-workspace-hero-copy">
          <div class="lv-page-kicker">Workspace management</div>
          <h1 class="lv-page-title">Workspaces</h1>
          <p>
            Manage the spaces your vaults, resources, tags, members, and invitations belong to.
          </p>
        </div>

        <div class="lv-workspace-hero-actions">
          <button class="lv-ui-button secondary" type="button" (click)="load()">
            <span class="material-symbols-outlined">refresh</span>
            Refresh
          </button>
          <button class="lv-ui-button primary" type="button" (click)="openCreate()">
            <span class="material-symbols-outlined">add</span>
            New workspace
          </button>
        </div>
      </header>

      <div *ngIf="error" class="lv-notice error">{{ error }}</div>
      <div *ngIf="success" class="lv-notice success">{{ success }}</div>

      <section class="lv-invite-inbox" *ngIf="pendingInvitations.length > 0">
        <div class="lv-section-heading">
          <div>
            <h2>Invitations waiting for you</h2>
            <p>These workspace invites match your current username or email.</p>
          </div>
          <span class="lv-pill warning">{{ pendingInvitations.length }} pending</span>
        </div>

        <div class="lv-invite-grid">
          <article class="lv-invite-card" *ngFor="let invitation of pendingInvitations">
            <div class="lv-invite-icon">
              <span class="material-symbols-outlined">mail</span>
            </div>
            <div class="lv-invite-body">
              <h3>{{ invitation.workspaceName || 'Workspace invitation' }}</h3>
              <p>
                Invited as {{ invitation.role }} by
                {{ invitation.invitedByDisplayName || invitation.invitedByUsername || 'workspace admin' }}.
              </p>
              <small>Expires {{ invitation.expiresAt | date:'mediumDate' }}</small>
            </div>
            <div class="lv-invite-actions">
              <button class="lv-ui-button secondary small" type="button" (click)="declineInvite(invitation)">
                Decline
              </button>
              <button class="lv-ui-button primary small" type="button" (click)="acceptInvite(invitation)">
                Accept
              </button>
            </div>
          </article>
        </div>
      </section>

      <div *ngIf="loading" class="lv-loading-card">
        <span class="spinner-border spinner-border-sm"></span>
        Loading workspace data...
      </div>

      <div class="lv-workspace-layout" *ngIf="!loading">
        <aside class="lv-workspace-list-panel">
          <div class="lv-panel-header">
            <div>
              <h2>Workspace list</h2>
              <p>{{ workspaces.length }} total workspace{{ workspaces.length === 1 ? '' : 's' }}</p>
            </div>
            <span class="material-symbols-outlined">corporate_fare</span>
          </div>

          <div class="lv-workspace-list" *ngIf="workspaces.length > 0; else noWorkspaceTpl">
            <button
              class="lv-workspace-list-item"
              type="button"
              *ngFor="let workspace of workspaces"
              [class.active]="selectedWorkspace?.id === workspace.id"
              (click)="selectWorkspace(workspace)"
            >
              <span class="lv-workspace-avatar">{{ workspaceInitial(workspace) }}</span>
              <span class="lv-workspace-list-main">
                <span class="lv-workspace-list-title">{{ workspace.name }}</span>
                <span class="lv-workspace-list-meta">{{ workspace.slug }} · {{ workspace.plan }} plan</span>
              </span>
              <span class="lv-workspace-list-side">
                <span class="lv-pill" [ngClass]="badgeClassForRole(workspace.role)">{{ workspace.role }}</span>
                <span *ngIf="isActiveWorkspace(workspace)" class="lv-active-dot">Active</span>
              </span>
            </button>
          </div>

          <ng-template #noWorkspaceTpl>
            <div class="lv-workspace-empty compact">
              <span class="material-symbols-outlined">corporate_fare</span>
              <strong>No workspace yet</strong>
              <p>Create one to start organizing resources with a team.</p>
            </div>
          </ng-template>
        </aside>

        <main class="lv-workspace-detail-panel">
          <ng-container *ngIf="selectedWorkspace as workspace; else selectWorkspaceTpl">
            <section class="lv-workspace-detail-card">
              <div class="lv-workspace-detail-top">
                <div class="lv-workspace-title-block">
                  <span class="lv-workspace-detail-avatar">{{ workspaceInitial(workspace) }}</span>
                  <div>
                    <h2>{{ workspace.name }}</h2>
                    <p>{{ workspace.slug }} · {{ workspace.plan }} plan</p>
                  </div>
                </div>

                <div class="lv-workspace-actions">
                  <button
                    class="lv-ui-button primary"
                    type="button"
                    [disabled]="isActiveWorkspace(workspace)"
                    (click)="setActive(workspace)"
                  >
                    {{ isActiveWorkspace(workspace) ? 'Active workspace' : 'Set active' }}
                  </button>
                  <button class="lv-ui-button secondary" type="button" (click)="openEdit(workspace)">
                    Rename
                  </button>
                  <a class="lv-ui-button secondary" routerLink="/vaults">Open vaults</a>
                  <button
                    class="lv-ui-button danger"
                    type="button"
                    *ngIf="canManage(workspace)"
                    (click)="deleteWorkspace(workspace)"
                  >
                    Delete
                  </button>
                </div>
              </div>

              <p class="lv-workspace-description">
                This workspace controls the active context for Dashboard, Vaults, Resources, Tags, members, and invitations.
              </p>

              <div class="lv-workspace-stat-grid">
                <div class="lv-workspace-stat">
                  <span>Your role</span>
                  <strong>{{ workspace.role }}</strong>
                </div>
                <div class="lv-workspace-stat">
                  <span>Plan</span>
                  <strong>{{ workspace.plan }}</strong>
                </div>
                <div class="lv-workspace-stat">
                  <span>Created</span>
                  <strong>{{ workspace.createdAt | date:'mediumDate' }}</strong>
                </div>
              </div>
            </section>

            <section class="lv-workspace-team-card">
              <div class="lv-team-header">
                <div>
                  <h2>Members & invitations</h2>
                  <p>Invite teammates and manage access for this workspace.</p>
                </div>
                <div class="lv-tab-switcher" role="tablist" aria-label="Workspace team tabs">
                  <button
                    type="button"
                    [class.active]="activeTab === 'members'"
                    (click)="activeTab = 'members'"
                  >
                    Members
                  </button>
                  <button
                    type="button"
                    [class.active]="activeTab === 'invitations'"
                    (click)="openInvitationsTab()"
                  >
                    Invitations
                  </button>
                </div>
              </div>

              <div *ngIf="membersError" class="lv-notice error">{{ membersError }}</div>

              <ng-container *ngIf="activeTab === 'members'">
                <div class="lv-member-list" *ngIf="members.length > 0; else noMemberTpl">
                  <article class="lv-member-row" *ngFor="let member of members">
                    <div class="lv-member-person">
                      <span class="lv-member-avatar">{{ memberInitial(member) }}</span>
                      <span>
                        <strong>{{ member.displayName || member.username }}</strong>
                        <small>{{ member.email }}</small>
                      </span>
                    </div>

                    <div class="lv-member-role">
                      <label>Role</label>
                      <select
                        class="lv-field-select compact"
                        [ngModel]="member.role"
                        (ngModelChange)="updateRole(member, $event)"
                        [disabled]="!canManage(workspace) || member.userId === auth.currentUser()?.id"
                      >
                        <option value="OWNER">Owner</option>
                        <option value="ADMIN">Admin</option>
                        <option value="MEMBER">Member</option>
                        <option value="VIEWER">Viewer</option>
                      </select>
                    </div>

                    <div class="lv-member-date">
                      <span>Joined</span>
                      <strong>{{ member.joinedAt | date:'mediumDate' }}</strong>
                    </div>

                    <div class="lv-member-actions">
                      <button
                        class="lv-ui-button danger small"
                        type="button"
                        [disabled]="!canManage(workspace) || member.userId === auth.currentUser()?.id"
                        (click)="removeMember(member)"
                      >
                        Remove
                      </button>
                    </div>
                  </article>
                </div>

                <ng-template #noMemberTpl>
                  <div class="lv-workspace-empty">
                    <span class="material-symbols-outlined">group</span>
                    <strong>No members found</strong>
                    <p>Invite teammates to collaborate in this workspace.</p>
                  </div>
                </ng-template>
              </ng-container>

              <ng-container *ngIf="activeTab === 'invitations'">
                <div *ngIf="!canManage(workspace)" class="lv-workspace-empty compact">
                  <span class="material-symbols-outlined">lock</span>
                  <strong>Invite access is restricted</strong>
                  <p>Only owners and admins can invite new members.</p>
                </div>

                <div *ngIf="canManage(workspace)" class="lv-invite-form-card">
                  <div>
                    <h3>Invite a teammate</h3>
                    <p>Invite by username or email. Existing users will see it in their notification center.</p>
                  </div>

                  <div class="lv-invite-form-grid">
                    <input
                      class="lv-field-input"
                      name="invitedIdentifier"
                      placeholder="username or email"
                      [(ngModel)]="inviteForm.invitedIdentifier"
                    />
                    <select class="lv-field-select" name="inviteRole" [(ngModel)]="inviteForm.role">
                      <option value="ADMIN">Admin</option>
                      <option value="MEMBER">Member</option>
                      <option value="VIEWER">Viewer</option>
                    </select>
                    <button
                      class="lv-ui-button primary"
                      type="button"
                      [disabled]="!inviteForm.invitedIdentifier || inviting"
                      (click)="sendInvite()"
                    >
                      {{ inviting ? 'Sending...' : 'Invite' }}
                    </button>
                  </div>

                  <div *ngIf="inviteSuccessMsg" class="lv-form-message success">{{ inviteSuccessMsg }}</div>

                  <div *ngIf="lastInviteLink" class="lv-copy-line">
                    <span>Invite link</span>
                    <input [value]="lastInviteLink" readonly />
                    <button type="button" (click)="copyText(lastInviteLink)">Copy</button>
                  </div>
                </div>

                <div class="lv-invitation-list" *ngIf="invitations.length > 0; else noInvitationTpl">
                  <article class="lv-invitation-row" *ngFor="let invitation of invitations">
                    <div class="lv-invitation-main">
                      <strong>{{ invitation.invitedIdentifier }}</strong>
                      <small>Role: {{ invitation.role }} · expires {{ invitation.expiresAt | date:'mediumDate' }}</small>
                    </div>
                    <span class="lv-pill" [ngClass]="badgeClassForInvitation(invitation.status)">
                      {{ invitation.status }}
                    </span>
                    <div class="lv-invitation-actions">
                      <button
                        class="lv-ui-button secondary small"
                        type="button"
                        *ngIf="invitation.status === 'PENDING'"
                        (click)="copyInviteLink(invitation)"
                      >
                        Copy link
                      </button>
                      <button
                        class="lv-ui-button danger small"
                        type="button"
                        *ngIf="invitation.status === 'PENDING' && canManage(workspace)"
                        (click)="cancelInvite(invitation)"
                      >
                        Cancel
                      </button>
                    </div>
                  </article>
                </div>

                <ng-template #noInvitationTpl>
                  <div class="lv-workspace-empty compact">
                    <span class="material-symbols-outlined">outgoing_mail</span>
                    <strong>No invitations</strong>
                    <p>No pending invitations for this workspace.</p>
                  </div>
                </ng-template>
              </ng-container>
            </section>
          </ng-container>

          <ng-template #selectWorkspaceTpl>
            <div class="lv-workspace-empty large">
              <span class="material-symbols-outlined">touch_app</span>
              <strong>Select a workspace</strong>
              <p>Pick a workspace from the list to manage members, invitations, and context.</p>
            </div>
          </ng-template>
        </main>
      </div>
    </section>

    <div class="lv-modal-backdrop" *ngIf="showForm" (click)="closeForm()">
      <section class="lv-workspace-modal" (click)="$event.stopPropagation()">
        <div class="lv-modal-header-clean">
          <div>
            <h2>{{ editingId ? 'Rename workspace' : 'Create workspace' }}</h2>
            <p>Use a short name that is easy to recognize in the workspace switcher.</p>
          </div>
          <button class="lv-icon-button" type="button" (click)="closeForm()">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>

        <form class="lv-modal-form" (ngSubmit)="save()">
          <label>Workspace name</label>
          <input class="lv-field-input large" name="name" [(ngModel)]="form.name" required maxlength="150" />

          <div class="lv-modal-actions-clean">
            <button type="button" class="lv-ui-button secondary" (click)="closeForm()">Cancel</button>
            <button type="submit" class="lv-ui-button primary" [disabled]="saving || !form.name.trim()">
              <span *ngIf="saving" class="spinner-border spinner-border-sm"></span>
              Save workspace
            </button>
          </div>
        </form>
      </section>
    </div>
  `
})
export class WorkspaceSettingsComponent implements OnInit {
  protected workspaces: Workspace[] = [];
  protected selectedWorkspace?: Workspace;
  protected pendingInvitations: WorkspaceInvitation[] = [];
  protected loading = false;
  protected error = '';
  protected success = '';

  protected showForm = false;
  protected editingId: string | null = null;
  protected form: WorkspaceRequest = { name: '' };
  protected saving = false;

  protected members: WorkspaceMember[] = [];
  protected invitations: WorkspaceInvitation[] = [];
  protected membersError = '';
  protected activeTab: 'members' | 'invitations' = 'members';
  protected inviteForm: { invitedIdentifier: string; role: WorkspaceRole } = { invitedIdentifier: '', role: 'MEMBER' };
  protected inviting = false;
  protected inviteSuccessMsg = '';
  protected lastInviteLink = '';

  private readonly wsService = inject(WorkspaceService);
  protected readonly auth = inject(AuthService);

  ngOnInit(): void {
    this.load();
  }

  protected activeWorkspaceId(): string | null {
    return this.wsService.activeWorkspaceId();
  }

  protected isActiveWorkspace(workspace: Workspace): boolean {
    return this.activeWorkspaceId() === workspace.id;
  }

  protected workspaceInitial(workspace: Workspace): string {
    return (workspace.name || workspace.slug || 'W').trim().charAt(0).toUpperCase();
  }

  protected memberInitial(member: WorkspaceMember): string {
    return (member.displayName || member.username || member.email || 'U').trim().charAt(0).toUpperCase();
  }

  protected load(): void {
    this.loading = true;
    this.error = '';
    this.success = '';
    this.wsService.list().subscribe({
      next: (data) => {
        this.workspaces = data;
        const activeId = this.wsService.ensureActiveWorkspace(data);
        this.selectedWorkspace = data.find((workspace) => workspace.id === (this.selectedWorkspace?.id ?? activeId)) ?? data[0];
        this.loading = false;
        if (this.selectedWorkspace) {
          this.loadMembers();
        }
      },
      error: (err) => {
        this.error = err instanceof Error ? err.message : 'Failed to load workspaces';
        this.loading = false;
      }
    });

    this.loadPendingInvitations();
  }

  protected loadPendingInvitations(): void {
    this.wsService.pendingInvitations().subscribe({
      next: (data) => (this.pendingInvitations = data),
      error: () => (this.pendingInvitations = [])
    });
  }

  protected selectWorkspace(workspace: Workspace): void {
    this.selectedWorkspace = workspace;
    this.membersError = '';
    this.activeTab = 'members';
    this.loadMembers();
  }

  protected setActive(workspace: Workspace): void {
    this.wsService.setActiveWorkspace(workspace.id);
    this.success = `${workspace.name} is now the active workspace.`;
  }

  protected openCreate(): void {
    this.editingId = null;
    this.form = { name: '' };
    this.showForm = true;
  }

  protected openEdit(workspace: Workspace): void {
    this.editingId = workspace.id;
    this.form = { name: workspace.name };
    this.showForm = true;
  }

  protected closeForm(): void {
    this.showForm = false;
  }

  protected save(): void {
    const name = this.form.name.trim();
    if (!name) return;

    this.saving = true;
    const request = { name };
    const action = this.editingId ? this.wsService.update(this.editingId, request) : this.wsService.create(request);

    action.subscribe({
      next: (workspace) => {
        this.saving = false;
        this.closeForm();
        if (!this.editingId) {
          this.wsService.setActiveWorkspace(workspace.id);
          this.selectedWorkspace = workspace;
        }
        this.load();
      },
      error: (err) => {
        this.error = err instanceof Error ? err.message : 'Could not save workspace';
        this.saving = false;
      }
    });
  }

  protected deleteWorkspace(workspace: Workspace): void {
    if (!confirm(`Delete workspace "${workspace.name}"?`)) return;
    this.wsService.delete(workspace.id).subscribe({
      next: () => this.load(),
      error: (err) => (this.error = err instanceof Error ? err.message : 'Could not delete workspace')
    });
  }

  protected loadMembers(): void {
    if (!this.selectedWorkspace) return;
    this.membersError = '';
    this.wsService.members(this.selectedWorkspace.id).subscribe({
      next: (data) => (this.members = data),
      error: (err) => (this.membersError = err instanceof Error ? err.message : 'Failed to load members')
    });
  }

  protected openInvitationsTab(): void {
    this.activeTab = 'invitations';
    this.loadInvitations();
  }

  protected loadInvitations(): void {
    if (!this.selectedWorkspace || !this.canManage(this.selectedWorkspace)) {
      this.invitations = [];
      return;
    }

    this.wsService.listInvitations(this.selectedWorkspace.id).subscribe({
      next: (data) => (this.invitations = data),
      error: (err) => (this.membersError = err instanceof Error ? err.message : 'Failed to load invitations')
    });
  }

  protected sendInvite(): void {
    if (!this.selectedWorkspace) return;
    const invitedIdentifier = this.inviteForm.invitedIdentifier.trim();
    if (!invitedIdentifier) return;

    this.inviting = true;
    this.membersError = '';
    this.inviteSuccessMsg = '';
    this.lastInviteLink = '';

    this.wsService.inviteMember(this.selectedWorkspace.id, { invitedIdentifier, role: this.inviteForm.role }).subscribe({
      next: (invitation) => {
        this.inviting = false;
        this.inviteSuccessMsg = 'Invitation created. Existing users will see it in the notification center.';
        this.inviteForm.invitedIdentifier = '';
        this.lastInviteLink = this.inviteUrl(invitation);
        this.loadInvitations();
      },
      error: (err) => {
        this.inviting = false;
        this.membersError = err instanceof Error ? err.message : 'Could not send invitation';
      }
    });
  }

  protected cancelInvite(invitation: WorkspaceInvitation): void {
    if (!this.selectedWorkspace) return;
    if (!confirm('Cancel this invitation?')) return;
    this.wsService.cancelInvitation(this.selectedWorkspace.id, invitation.id).subscribe({
      next: () => this.loadInvitations(),
      error: (err) => (this.membersError = err instanceof Error ? err.message : 'Could not cancel invitation')
    });
  }

  protected copyInviteLink(invitation: WorkspaceInvitation): void {
    this.copyText(this.inviteUrl(invitation));
  }

  protected copyText(text: string): void {
    navigator.clipboard?.writeText(text).then(
      () => (this.success = 'Copied to clipboard.'),
      () => (this.success = text)
    );
  }

  protected acceptInvite(invitation: WorkspaceInvitation): void {
    this.wsService.acceptInvitation(invitation.token).subscribe({
      next: (accepted) => {
        this.wsService.setActiveWorkspace(accepted.workspaceId);
        this.success = `Joined ${accepted.workspaceName || 'workspace'} successfully.`;
        this.load();
      },
      error: (err) => (this.error = err instanceof Error ? err.message : 'Could not accept invitation')
    });
  }

  protected declineInvite(invitation: WorkspaceInvitation): void {
    this.wsService.declineInvitation(invitation.token).subscribe({
      next: () => this.loadPendingInvitations(),
      error: (err) => (this.error = err instanceof Error ? err.message : 'Could not decline invitation')
    });
  }

  protected updateRole(member: WorkspaceMember, role: WorkspaceRole): void {
    if (!this.selectedWorkspace) return;
    this.wsService.updateMemberRole(this.selectedWorkspace.id, member.userId, role).subscribe({
      next: () => this.loadMembers(),
      error: (err) => {
        this.membersError = err instanceof Error ? err.message : 'Could not update role';
        this.loadMembers();
      }
    });
  }

  protected removeMember(member: WorkspaceMember): void {
    if (!this.selectedWorkspace) return;
    if (!confirm(`Remove ${member.displayName || member.username} from this workspace?`)) return;
    this.wsService.removeMember(this.selectedWorkspace.id, member.userId).subscribe({
      next: () => this.loadMembers(),
      error: (err) => (this.membersError = err instanceof Error ? err.message : 'Could not remove member')
    });
  }

  protected canManage(workspace: Workspace | undefined): boolean {
    return workspace?.role === 'OWNER' || workspace?.role === 'ADMIN';
  }

  protected badgeClassForRole(role: WorkspaceRole): string {
    switch (role) {
      case 'OWNER': return 'owner';
      case 'ADMIN': return 'success';
      case 'VIEWER': return 'neutral';
      default: return 'warning';
    }
  }

  protected badgeClassForInvitation(status: string): string {
    switch (status) {
      case 'PENDING': return 'warning';
      case 'ACCEPTED': return 'success';
      case 'DECLINED': return 'neutral';
      case 'CANCELLED': return 'neutral';
      default: return 'error';
    }
  }

  private inviteUrl(invitation: WorkspaceInvitation): string {
    return `${window.location.origin}/invitations/${invitation.token}`;
  }
}
