import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PublicApiService } from '../../../core/http/public-api.service';
import { Vault } from '../../vaults/models/vault.model';
import { Resource } from '../../resources/models/resource.model';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-public-vault',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="container py-5 max-w-4xl">
      <div *ngIf="loading" class="text-center py-5">
        <span class="spinner-border text-primary"></span>
        <p class="mt-3 text-muted">Loading vault...</p>
      </div>

      <div *ngIf="error" class="alert alert-danger shadow-sm">
        <h4 class="alert-heading">Access Denied</h4>
        <p class="mb-0">{{ error }}</p>
      </div>

      <ng-container *ngIf="!loading && vault">
        <div class="lv-card p-4 mb-4 text-center">
          <span class="lv-icon-box lv-icon-box-lg mb-3 mx-auto" [style.color]="vault.color || null">
            <span class="material-symbols-outlined" style="font-size:32px">{{ vault.icon || 'work' }}</span>
          </span>
          <h1 class="lv-page-title display-6 mb-2">{{ vault.name }}</h1>
          <p class="lv-muted lead mb-0">{{ vault.description || 'Public Resource Vault' }}</p>
          <div class="mt-3">
            <span class="badge rounded-pill lv-badge-soft px-3 py-2">
              <span class="material-symbols-outlined align-middle me-1" style="font-size:14px">public</span>
              Public {{ vault.publicAccess === 'EDIT' ? 'Editor' : 'Viewer' }}
            </span>
          </div>
        </div>

        <div class="lv-card p-4">
          <h2 class="lv-section-title mb-4">Resources</h2>
          
          <div *ngIf="loadingResources" class="text-center py-4">
            <span class="spinner-border spinner-border-sm me-2"></span> Loading resources...
          </div>

          <div *ngIf="!loadingResources && resources.length === 0" class="lv-empty-state py-5">
            <span class="material-symbols-outlined mb-2" style="font-size:48px; color:var(--lv-gray-400)">folder_open</span>
            <p class="lv-muted mb-0">This vault is currently empty.</p>
          </div>

          <div class="row g-3" *ngIf="!loadingResources && resources.length > 0">
            <div class="col-md-6 col-lg-4" *ngFor="let res of resources">
              <a [routerLink]="['/public/resources', res.id]" class="lv-card h-100 text-decoration-none text-dark hover-shadow transition-all border-hover-primary p-3 d-flex flex-column">
                <div class="d-flex align-items-start gap-3 mb-2">
                  <span class="lv-icon-box lv-badge-soft">
                    <span class="material-symbols-outlined">{{ getIcon(res) }}</span>
                  </span>
                  <div class="min-w-0 flex-grow-1">
                    <h3 class="h6 mb-1 text-truncate fw-semibold">{{ res.title || 'Untitled' }}</h3>
                    <small class="text-muted">{{ res.resourceType }}</small>
                  </div>
                </div>
                <p class="small text-muted mb-0 lv-line-clamp-2 mt-auto">{{ res.description || 'No description' }}</p>
              </a>
            </div>
          </div>
        </div>
      </ng-container>
    </div>
  `
})
export class PublicVaultComponent implements OnInit {
  vault?: Vault;
  resources: Resource[] = [];
  loading = true;
  loadingResources = false;
  error = '';

  private readonly route = inject(ActivatedRoute);
  private readonly publicApi = inject(PublicApiService);
  private readonly destroyRef = inject(DestroyRef);

  ngOnInit() {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.loadVault(id);
      }
    });
  }

  loadVault(id: string) {
    this.loading = true;
    this.error = '';
    
    this.publicApi.getVault(id).subscribe({
      next: (res) => {
        this.vault = res.data;
        this.loading = false;
        this.loadResources(id);
      },
      error: (err) => {
        this.error = err.error?.message || 'Failed to load vault.';
        this.loading = false;
      }
    });
  }

  loadResources(id: string) {
    this.loadingResources = true;
    this.publicApi.getVaultResources(id, { size: 100 }).subscribe({
      next: (res) => {
        this.resources = res.data.content;
        this.loadingResources = false;
      },
      error: () => {
        this.loadingResources = false;
      }
    });
  }

  getIcon(res: Resource): string {
    if (res.resourceType === 'LINK') return 'link';
    if (res.resourceType === 'FILE') return 'description';
    if (res.resourceType === 'NOTE') return 'notes';
    return 'code';
  }
}
