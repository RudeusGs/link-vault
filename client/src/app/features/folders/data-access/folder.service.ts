import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Folder, FolderRequest } from '../models/folder.model';
import { ApiService } from '../../../core/http/api.service';
import { WorkspaceService } from '../../settings/data-access/workspace.service';

@Injectable({ providedIn: 'root' })
export class FolderService {
  private readonly api = inject(ApiService);
  private readonly workspaceService = inject(WorkspaceService);

  listByVault(vaultId: string): Observable<Folder[]> {
    return this.api.get<Folder[]>(`${this.workspaceService.pathPrefix()}/vaults/${vaultId}/folders`);
  }

  listChildren(folderId: string): Observable<Folder[]> {
    return this.api.get<Folder[]>(`${this.workspaceService.pathPrefix()}/folders/${folderId}/children`);
  }

  get(id: string): Observable<Folder> {
    return this.api.get<Folder>(`${this.workspaceService.pathPrefix()}/folders/${id}`);
  }

  createInVault(vaultId: string, request: FolderRequest): Observable<Folder> {
    return this.api.post<Folder>(`${this.workspaceService.pathPrefix()}/vaults/${vaultId}/folders`, request);
  }

  createChild(parentId: string, request: FolderRequest): Observable<Folder> {
    return this.api.post<Folder>(`${this.workspaceService.pathPrefix()}/folders/${parentId}/children`, request);
  }

  update(id: string, request: FolderRequest): Observable<Folder> {
    return this.api.put<Folder>(`${this.workspaceService.pathPrefix()}/folders/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.api.delete<void>(`${this.workspaceService.pathPrefix()}/folders/${id}`);
  }
}
