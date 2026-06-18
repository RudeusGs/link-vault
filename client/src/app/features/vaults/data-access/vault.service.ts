import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Vault, VaultRequest } from '../models/vault.model';
import { ApiService } from '../../../core/http/api.service';
import { WorkspaceService } from '../../settings/data-access/workspace.service';

@Injectable({ providedIn: 'root' })
export class VaultService {
  private readonly api = inject(ApiService);
  private readonly workspaceService = inject(WorkspaceService);

  list(): Observable<Vault[]> {
    return this.api.get<Vault[]>(`${this.workspaceService.pathPrefix()}/vaults`);
  }

  get(id: string): Observable<Vault> {
    return this.api.get<Vault>(`${this.workspaceService.pathPrefix()}/vaults/${id}`);
  }

  create(request: VaultRequest): Observable<Vault> {
    return this.api.post<Vault>(`${this.workspaceService.pathPrefix()}/vaults`, request);
  }

  update(id: string, request: VaultRequest): Observable<Vault> {
    return this.api.put<Vault>(`${this.workspaceService.pathPrefix()}/vaults/${id}`, request);
  }

  delete(id: string): Observable<void> {
    return this.api.delete<void>(`${this.workspaceService.pathPrefix()}/vaults/${id}`);
  }
}
