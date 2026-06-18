import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { PageResponse } from '../models/page-response.model';
import { Vault, VaultRequest } from '../../features/vaults/models/vault.model';
import { Resource, ResourceRequest } from '../../features/resources/models/resource.model';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PublicApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/public`;

  // --- Vaults ---

  getVault(id: string): Observable<{ data: Vault }> {
    return this.http.get<{ data: Vault }>(`${this.baseUrl}/vaults/${id}`);
  }

  getVaultResources(id: string, params?: any): Observable<{ data: PageResponse<Resource> }> {
    return this.http.get<{ data: PageResponse<Resource> }>(`${this.baseUrl}/vaults/${id}/resources`, { params });
  }

  updateVault(id: string, request: VaultRequest): Observable<{ data: Vault }> {
    return this.http.put<{ data: Vault }>(`${this.baseUrl}/vaults/${id}`, request);
  }

  createResourceInVault(id: string, request: ResourceRequest): Observable<{ data: Resource }> {
    return this.http.post<{ data: Resource }>(`${this.baseUrl}/vaults/${id}/resources`, request);
  }

  // --- Resources ---

  getResource(id: string): Observable<{ data: Resource }> {
    return this.http.get<{ data: Resource }>(`${this.baseUrl}/resources/${id}`);
  }

  updateResource(id: string, request: ResourceRequest): Observable<{ data: Resource }> {
    return this.http.put<{ data: Resource }>(`${this.baseUrl}/resources/${id}`, request);
  }
}
