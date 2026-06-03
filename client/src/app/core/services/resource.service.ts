import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  Resource,
  ResourcePreview,
  ResourceRequest,
  ResourceSearchParams
} from '../models/resource.model';
import { ApiService } from './api.service';

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

  create(request: ResourceRequest): Observable<Resource> {
    return this.api.post<Resource>('/resources', request);
  }

  update(id: string, request: ResourceRequest): Observable<Resource> {
    return this.api.put<Resource>(`/resources/${id}`, request);
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

  uploadFile(formData: FormData): Observable<Resource> {
    return this.api.upload<Resource>('/resources/upload', formData);
  }

  preview(id: string): Observable<ResourcePreview> {
    return this.api.get<ResourcePreview>(`/resources/${id}/preview`);
  }

  attachTag(resourceId: string, tagId: string): Observable<Resource> {
    return this.api.post<Resource>(`/resources/${resourceId}/tags/${tagId}`, {});
  }

  detachTag(resourceId: string, tagId: string): Observable<Resource> {
    return this.api.delete<Resource>(`/resources/${resourceId}/tags/${tagId}`);
  }
}
