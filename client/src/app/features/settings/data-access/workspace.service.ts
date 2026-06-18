import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/http/api.service';
import { Workspace, WorkspaceMember, WorkspaceRequest, WorkspaceUsage } from '../models/workspace.model';

@Injectable({ providedIn: 'root' })
export class WorkspaceService {
  private readonly api = inject(ApiService);

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

  listInvitations(id: string): Observable<any[]> {
    return this.api.get<any[]>(`/workspaces/${id}/invitations`);
  }

  inviteMember(id: string, request: { invitedIdentifier: string; role: string }): Observable<any> {
    return this.api.post<any>(`/workspaces/${id}/invitations`, request);
  }

  cancelInvitation(id: string, invitationId: string): Observable<void> {
    return this.api.delete<void>(`/workspaces/${id}/invitations/${invitationId}`);
  }

  acceptInvitation(token: string): Observable<any> {
    return this.api.post<any>(`/workspace-invitations/${token}/accept`, {});
  }

  declineInvitation(token: string): Observable<any> {
    return this.api.post<any>(`/workspace-invitations/${token}/decline`, {});
  }
}
