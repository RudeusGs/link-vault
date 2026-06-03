import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Folder, FolderRequest } from '../models/folder.model';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class FolderService {
  private readonly api = inject(ApiService);

  listByVault(vaultId: string): Observable<Folder[]> {
    return this.api.get<Folder[]>(`/vaults/${vaultId}/folders`);
  }

  listChildren(folderId: string): Observable<Folder[]> {
    return this.api.get<Folder[]>(`/folders/${folderId}/children`);
  }

  get(id: string): Observable<Folder> {
    return this.api.get<Folder>(`/folders/${id}`);
  }

  createInVault(vaultId: string, request: FolderRequest): Observable<Folder> {
    return this.api.post<Folder>(`/vaults/${vaultId}/folders`, request);
  }

  createChild(parentId: string, request: FolderRequest): Observable<Folder> {
    return this.api.post<Folder>(`/folders/${parentId}/children`, request);
  }

  update(id: string, request: FolderRequest): Observable<Folder> {
    return this.api.put<Folder>(`/folders/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.api.delete<void>(`/folders/${id}`);
  }
}
