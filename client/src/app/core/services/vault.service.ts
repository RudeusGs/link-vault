import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { Vault, VaultRequest } from '../models/vault.model';
import { ApiService } from './api.service';

@Injectable({ providedIn: 'root' })
export class VaultService {
  private readonly api = inject(ApiService);

  list(): Observable<Vault[]> { return this.api.get<Vault[]>('/vaults'); }
  get(id: string): Observable<Vault> { return this.api.get<Vault>(`/vaults/${id}`); }
  create(request: VaultRequest): Observable<Vault> { return this.api.post<Vault>('/vaults', request); }
  update(id: string, request: VaultRequest): Observable<Vault> { return this.api.put<Vault>(`/vaults/${id}`, request); }
  delete(id: string): Observable<void> { return this.api.delete<void>(`/vaults/${id}`); }
}
