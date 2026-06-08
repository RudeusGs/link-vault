import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  DocumentPreview,
  LinkPreview,
  LinkPreviewRequest,
  Resource,
  ResourcePreview,
  ResourceRequest,
  ResourceSearchParams
} from '../models/resource.model';
import { ApiService } from '../../../core/http/api.service';

@Injectable({
  providedIn: 'root'
})
export class ResourceService {
  private readonly api = inject(ApiService);

  list(): Observable<Resource[]> {
    return this.api.get<Resource[]>('/resources');
  }

  listByVault(vaultId: string): Observable<Resource[]> {
    return this.api.get<Resource[]>(`/vaults/${vaultId}/resources`);
  }

  listByFolder(folderId: string): Observable<Resource[]> {
    return this.api.get<Resource[]>(`/folders/${folderId}/resources`);
  }

  search(params: ResourceSearchParams): Observable<Resource[]> {
    return this.api.get<Resource[]>('/resources/search', params);
  }

  get(id: string): Observable<Resource> {
    return this.api.get<Resource>(`/resources/${id}`);
  }

  createInVault(vaultId: string, request: ResourceRequest): Observable<Resource> {
    return this.api.post<Resource>(`/vaults/${vaultId}/resources`, request);
  }

  createInFolder(folderId: string, request: ResourceRequest): Observable<Resource> {
    return this.api.post<Resource>(`/folders/${folderId}/resources`, request);
  }

  update(id: string, request: ResourceRequest): Observable<Resource> {
    return this.api.put<Resource>(`/resources/${id}`, request);
  }

  fetchLinkPreview(url: string): Observable<LinkPreview> {
    const request: LinkPreviewRequest = { url };
    return this.api.post<LinkPreview>('/link-preview', request);
  }

  refreshLinkPreview(id: string): Observable<Resource> {
    return this.api.patch<Resource>(`/resources/${id}/refresh-preview`);
  }

  delete(id: string): Observable<void> {
    return this.api.delete<void>(`/resources/${id}`);
  }

  toggleFavorite(id: string): Observable<Resource> {
    return this.api.patch<Resource>(`/resources/${id}/favorite`);
  }

  toggleArchive(id: string): Observable<Resource> {
    return this.api.patch<Resource>(`/resources/${id}/archive`);
  }

  recordView(id: string): Observable<Resource> {
    return this.api.post<Resource>(`/resources/${id}/view`, {});
  }

  uploadToVault(vaultId: string, formData: FormData): Observable<Resource> {
    return this.api.upload<Resource>(`/vaults/${vaultId}/resources/upload`, formData);
  }

  uploadToFolder(folderId: string, formData: FormData): Observable<Resource> {
    return this.api.upload<Resource>(`/folders/${folderId}/resources/upload`, formData);
  }

  preview(id: string): Observable<ResourcePreview> {
    return this.api.get<ResourcePreview>(`/resources/${id}/preview`);
  }

  downloadFile(id: string): Observable<Blob> {
    return this.api.download(`/resources/${id}/file`);
  }

  documentPreview(id: string): Observable<DocumentPreview> {
    return this.api.get<DocumentPreview>(`/resources/${id}/document-preview`);
  }

  attachTag(resourceId: string, tagId: string): Observable<Resource> {
    return this.api.post<Resource>(`/resources/${resourceId}/tags/${tagId}`, {});
  }

  detachTag(resourceId: string, tagId: string): Observable<Resource> {
    return this.api.delete<Resource>(`/resources/${resourceId}/tags/${tagId}`);
  }
}
