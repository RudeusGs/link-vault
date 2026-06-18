import { Injectable, inject, signal } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/http/api.service';
import {
  Workspace,
  WorkspaceInvitation,
  WorkspaceMember,
  WorkspaceRequest,
  WorkspaceUsage
} from '../models/workspace.model';

const ACTIVE_WORKSPACE_KEY = 'linkvault.active_workspace_id';

@Injectable({ providedIn: 'root' })
export class WorkspaceService {
  private readonly api = inject(ApiService);
  private readonly activeWorkspaceIdState = signal<string | null>(this.readActiveWorkspaceId());

  readonly activeWorkspaceId = this.activeWorkspaceIdState.asReadonly();

  list(): Observable<Workspace[]> {
    return this.api.get<Workspace[]>('/workspaces');
  }

  get(id: string): Observable<Workspace> {
    return this.api.get<Workspace>(`/workspaces/${id}`);
  }

  create(request: WorkspaceRequest): Observable<Workspace> {
    return this.api.post<Workspace>('/workspaces', request);
  }

  update(id: string, request: WorkspaceRequest): Observable<Workspace> {
    return this.api.put<Workspace>(`/workspaces/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.api.delete<void>(`/workspaces/${id}`);
  }

  updatePlan(id: string, plan: string): Observable<Workspace> {
    return this.api.patch<Workspace>(`/workspaces/${id}/plan`, { plan });
  }

  members(id: string): Observable<WorkspaceMember[]> {
    return this.api.get<WorkspaceMember[]>(`/workspaces/${id}/members`);
  }

  updateMemberRole(id: string, memberId: string, role: string): Observable<WorkspaceMember> {
    return this.api.patch<WorkspaceMember>(`/workspaces/${id}/members/${memberId}/role`, { role });
  }

  removeMember(id: string, memberId: string): Observable<void> {
    return this.api.delete<void>(`/workspaces/${id}/members/${memberId}`);
  }

  usage(id: string): Observable<WorkspaceUsage> {
    return this.api.get<WorkspaceUsage>(`/workspaces/${id}/usage`);
  }

  listInvitations(id: string): Observable<WorkspaceInvitation[]> {
    return this.api.get<WorkspaceInvitation[]>(`/workspaces/${id}/invitations`);
  }

  pendingInvitations(): Observable<WorkspaceInvitation[]> {
    return this.api.get<WorkspaceInvitation[]>('/workspace-invitations/pending');
  }

  inviteMember(id: string, request: { invitedIdentifier: string; role: string }): Observable<WorkspaceInvitation> {
    return this.api.post<WorkspaceInvitation>(`/workspaces/${id}/invitations`, request);
  }

  cancelInvitation(id: string, invitationId: string): Observable<void> {
    return this.api.delete<void>(`/workspaces/${id}/invitations/${invitationId}`);
  }

  acceptInvitation(token: string): Observable<WorkspaceInvitation> {
    return this.api.post<WorkspaceInvitation>(`/workspace-invitations/${token}/accept`, {});
  }

  declineInvitation(token: string): Observable<WorkspaceInvitation> {
    return this.api.post<WorkspaceInvitation>(`/workspace-invitations/${token}/decline`, {});
  }

  setActiveWorkspace(id: string | null): void {
    this.activeWorkspaceIdState.set(id);
    try {
      if (id) {
        localStorage.setItem(ACTIVE_WORKSPACE_KEY, id);
      } else {
        localStorage.removeItem(ACTIVE_WORKSPACE_KEY);
      }
    } catch {
      // ignore storage failures
    }
  }

  ensureActiveWorkspace(workspaces: Workspace[]): string | null {
    const currentId = this.activeWorkspaceIdState();
    const existing = workspaces.find((workspace) => workspace.id === currentId);
    if (existing) {
      return existing.id;
    }

    const first = workspaces[0]?.id ?? null;
    this.setActiveWorkspace(first);
    return first;
  }

  pathPrefix(): string {
    const id = this.activeWorkspaceIdState();
    return id ? `/workspaces/${id}` : '';
  }

  currentId(): string | null {
    return this.activeWorkspaceIdState();
  }

  private readActiveWorkspaceId(): string | null {
    try {
      return localStorage.getItem(ACTIVE_WORKSPACE_KEY);
    } catch {
      return null;
    }
  }
}
