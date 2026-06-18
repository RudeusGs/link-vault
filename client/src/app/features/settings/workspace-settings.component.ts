import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { WorkspaceService } from './data-access/workspace.service';
import { Workspace, WorkspaceMember, WorkspaceRequest } from './models/workspace.model';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-workspace-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="lv-card p-4 mb-4">
      <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 class="lv-section-title mb-1">Workspaces</h2>
          <p class="lv-muted mb-0">Manage your workspaces and collaboration.</p>
        </div>
        <button class="btn btn-primary" (click)="openCreate()">New Workspace</button>
      </div>

      <div *ngIf="error" class="alert alert-danger">{{ error }}</div>

      <div class="table-responsive">
        <table class="table table-hover align-middle">
          <thead>
            <tr>
              <th>Name</th>
              <th>Created</th>
              <th class="text-end">Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let ws of workspaces">
              <td>
                <strong>{{ ws.name }}</strong>
                <div class="small lv-muted">{{ ws.description }}</div>
              </td>
              <td>{{ ws.createdAt | date }}</td>
              <td class="text-end">
                <button class="btn btn-sm btn-outline-secondary me-2" (click)="manageMembers(ws)">Members</button>
                <button class="btn btn-sm btn-outline-primary" (click)="openEdit(ws)">Edit</button>
              </td>
            </tr>
            <tr *ngIf="workspaces.length === 0 && !loading">
              <td colspan="3" class="text-center py-4 lv-muted">No workspaces found.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Create/Edit Modal -->
    <div class="lv-modal-backdrop" *ngIf="showForm" (click)="closeForm()">
      <div class="lv-modal-card p-4" (click)="$event.stopPropagation()">
        <h3 class="mb-3">{{ editingId ? 'Edit Workspace' : 'New Workspace' }}</h3>
        <form (ngSubmit)="save()">
          <div class="mb-3">
            <label class="form-label">Name</label>
            <input class="form-control" name="name" [(ngModel)]="form.name" required />
          </div>
          <div class="mb-3">
            <label class="form-label">Description</label>
            <textarea class="form-control" name="description" [(ngModel)]="form.description"></textarea>
          </div>
          <div class="text-end">
            <button type="button" class="btn btn-outline-secondary me-2" (click)="closeForm()">Cancel</button>
            <button type="submit" class="btn btn-primary" [disabled]="saving">Save</button>
          </div>
        </form>
      </div>
    </div>

    <!-- Members Modal -->
    <div class="lv-modal-backdrop" *ngIf="showMembers" (click)="closeMembers()">
      <div class="lv-modal-card p-4" (click)="$event.stopPropagation()">
        <h3 class="mb-3">Workspace Collaboration</h3>
        
        <ul class="nav nav-tabs mb-4">
          <li class="nav-item">
            <button class="nav-link" [class.active]="activeTab === 'members'" (click)="activeTab = 'members'">Members</button>
          </li>
          <li class="nav-item">
            <button class="nav-link" [class.active]="activeTab === 'invitations'" (click)="activeTab = 'invitations'; loadInvitations()">Invitations</button>
          </li>
        </ul>

        <div *ngIf="membersError" class="alert alert-danger">{{ membersError }}</div>
        
        <!-- Members Tab -->
        <div *ngIf="activeTab === 'members'">
          <table class="table align-middle">
            <thead>
              <tr>
                <th>User</th>
                <th>Role</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let member of members">
                <td>
                  <div>{{ member.user.displayName || member.user.username }}</div>
                  <div class="small lv-muted">{{ member.user.email }}</div>
                </td>
                <td>
                  <select class="form-select form-select-sm" 
                          [ngModel]="member.role" 
                          (ngModelChange)="updateRole(member, $event)"
                          [disabled]="member.user.id === auth.currentUser()?.id">
                    <option value="OWNER">Owner</option>
                    <option value="ADMIN">Admin</option>
                    <option value="MEMBER">Member</option>
                  </select>
                </td>
                <td>
                  <button 
                    class="btn btn-sm btn-outline-danger" 
                    [disabled]="member.user.id === auth.currentUser()?.id"
                    (click)="removeMember(member)">
                    Remove
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- Invitations Tab -->
        <div *ngIf="activeTab === 'invitations'">
          <div class="lv-soft-panel p-3 mb-4">
            <h5 class="mb-3">Invite new member</h5>
            <div class="d-flex gap-2">
              <input type="text" class="form-control" placeholder="Email address" [(ngModel)]="inviteForm.invitedIdentifier">
              <select class="form-select" style="width: 130px;" [(ngModel)]="inviteForm.role">
                <option value="ADMIN">Admin</option>
                <option value="MEMBER">Member</option>
                <option value="VIEWER">Viewer</option>
              </select>
              <button class="btn btn-primary" [disabled]="!inviteForm.invitedIdentifier || inviting" (click)="sendInvite()">
                {{ inviting ? 'Sending...' : 'Invite' }}
              </button>
            </div>
            <div *ngIf="inviteSuccessMsg" class="form-text text-success mt-2">{{ inviteSuccessMsg }}</div>
          </div>

          <h5 class="mb-3">Pending Invitations</h5>
          <table class="table align-middle">
            <thead>
              <tr>
                <th>Email</th>
                <th>Role</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let inv of invitations">
                <td>{{ inv.invitedIdentifier }}</td>
                <td>{{ inv.role }}</td>
                <td><span class="badge bg-secondary">{{ inv.status }}</span></td>
                <td>
                  <button class="btn btn-sm btn-outline-danger" *ngIf="inv.status === 'PENDING'" (click)="cancelInvite(inv)">Cancel</button>
                  <button class="btn btn-sm btn-outline-primary ms-2" *ngIf="inv.status === 'PENDING'" (click)="copyInviteLink(inv)">Copy Link</button>
                </td>
              </tr>
              <tr *ngIf="invitations.length === 0">
                <td colspan="4" class="text-center py-3 lv-muted">No pending invitations.</td>
              </tr>
            </tbody>
          </table>
        </div>
        
        <div class="text-end mt-3">
          <button class="btn btn-secondary" (click)="closeMembers()">Close</button>
        </div>
      </div>
    </div>
  `
})
export class WorkspaceSettingsComponent implements OnInit {
  protected workspaces: Workspace[] = [];
  protected loading = false;
  protected error = '';

  protected showForm = false;
  protected editingId: string | null = null;
  protected form: WorkspaceRequest = { name: '', description: '' };
  protected saving = false;

  protected showMembers = false;
  protected activeWorkspaceId: string | null = null;
  protected members: WorkspaceMember[] = [];
  protected membersError = '';

  protected activeTab: 'members' | 'invitations' = 'members';
  protected invitations: any[] = [];
  protected inviteForm = { invitedIdentifier: '', role: 'MEMBER' };
  protected inviting = false;
  protected inviteSuccessMsg = '';

  private readonly wsService = inject(WorkspaceService);
  protected readonly auth = inject(AuthService);

  ngOnInit() {
    this.load();
  }

  load() {
    this.loading = true;
    this.wsService.list().subscribe({
      next: (data) => {
        this.workspaces = data;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.message;
        this.loading = false;
      }
    });
  }

  openCreate() {
    this.editingId = null;
    this.form = { name: '', description: '' };
    this.showForm = true;
  }

  openEdit(ws: Workspace) {
    this.editingId = ws.id;
    this.form = { name: ws.name, description: ws.description };
    this.showForm = true;
  }

  closeForm() {
    this.showForm = false;
  }

  save() {
    this.saving = true;
    const req = this.editingId 
      ? this.wsService.update(this.editingId, this.form)
      : this.wsService.create(this.form);
      
    req.subscribe({
      next: () => {
        this.saving = false;
        this.closeForm();
        this.load();
      },
      error: (err) => {
        this.error = err.message;
        this.saving = false;
        this.closeForm();
      }
    });
  }

  manageMembers(ws: Workspace) {
    this.activeWorkspaceId = ws.id;
    this.membersError = '';
    this.activeTab = 'members';
    this.inviteSuccessMsg = '';
    this.wsService.members(ws.id).subscribe({
      next: (data) => {
        this.members = data;
        this.showMembers = true;
      },
      error: (err) => {
        this.error = 'Failed to load members';
      }
    });
  }

  loadInvitations() {
    if (!this.activeWorkspaceId) return;
    this.wsService.listInvitations(this.activeWorkspaceId).subscribe({
      next: (data) => this.invitations = data,
      error: (err) => this.membersError = 'Failed to load invitations'
    });
  }

  sendInvite() {
    if (!this.activeWorkspaceId) return;
    this.inviting = true;
    this.membersError = '';
    this.inviteSuccessMsg = '';
    this.wsService.inviteMember(this.activeWorkspaceId, this.inviteForm).subscribe({
      next: (res) => {
        this.inviting = false;
        this.inviteSuccessMsg = 'Invitation sent successfully!';
        this.inviteForm.invitedIdentifier = '';
        this.loadInvitations();
        
        // Mock sending email by logging token
        console.log('Mock Email: Invite link is: ' + window.location.origin + '/invitations/' + res.token);
      },
      error: (err) => {
        this.inviting = false;
        this.membersError = err.message;
      }
    });
  }

  cancelInvite(inv: any) {
    if (!this.activeWorkspaceId) return;
    if (!confirm('Cancel this invitation?')) return;
    this.wsService.cancelInvitation(this.activeWorkspaceId, inv.id).subscribe({
      next: () => this.loadInvitations(),
      error: (err) => this.membersError = err.message
    });
  }

  copyInviteLink(inv: any) {
    const link = window.location.origin + '/invitations/' + inv.token;
    navigator.clipboard.writeText(link).then(() => {
      alert('Invitation link copied to clipboard!');
    });
  }

  closeMembers() {
    this.showMembers = false;
    this.activeWorkspaceId = null;
  }

  updateRole(member: WorkspaceMember, role: string) {
    if (!this.activeWorkspaceId) return;
    this.wsService.updateMemberRole(this.activeWorkspaceId, member.user.id, role).subscribe({
      next: () => this.manageMembers({ id: this.activeWorkspaceId!, plan: 'FREE', role: 'OWNER', slug: '' } as unknown as Workspace),
      error: (err) => this.membersError = err.message
    });
  }

  removeMember(member: WorkspaceMember) {
    if (!this.activeWorkspaceId) return;
    if (!confirm('Remove member?')) return;
    this.wsService.removeMember(this.activeWorkspaceId, member.user.id).subscribe({
      next: () => this.manageMembers({ id: this.activeWorkspaceId!, plan: 'FREE', role: 'OWNER', slug: '' } as unknown as Workspace),
      error: (err) => this.membersError = err.message
    });
  }
}
