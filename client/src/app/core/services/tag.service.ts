import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Tag, TagRequest } from '../models/tag.model';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class TagService {
  private readonly api = inject(ApiService);

  list(): Observable<Tag[]> { return this.api.get<Tag[]>('/tags'); }
  create(request: TagRequest): Observable<Tag> { return this.api.post<Tag>('/tags', request); }
  update(id: string, request: TagRequest): Observable<Tag> { return this.api.put<Tag>(`/tags/${id}`, request); }
  delete(id: string): Observable<void> { return this.api.delete<void>(`/tags/${id}`); }
}
