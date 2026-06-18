import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Tag, TagRequest } from '../models/tag.model';
import { ApiService } from '../../../core/http/api.service';
import { WorkspaceService } from '../../settings/data-access/workspace.service';

@Injectable({ providedIn: 'root' })
export class TagService {
  private readonly api = inject(ApiService);
  private readonly workspaceService = inject(WorkspaceService);

  list(): Observable<Tag[]> {
    return this.api.get<Tag[]>(`${this.workspaceService.pathPrefix()}/tags`);
  }

  create(request: TagRequest): Observable<Tag> {
    return this.api.post<Tag>(`${this.workspaceService.pathPrefix()}/tags`, request);
  }

  update(id: string, request: TagRequest): Observable<Tag> {
    return this.api.put<Tag>(`${this.workspaceService.pathPrefix()}/tags/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.api.delete<void>(`${this.workspaceService.pathPrefix()}/tags/${id}`);
  }
}
