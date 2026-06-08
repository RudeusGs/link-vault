import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, OnInit, SimpleChanges, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ResourceService } from './data-access/resource.service';
import { Resource, ResourceSearchParams, ResourceType } from './models/resource.model';
import { LinkPreviewCardComponent } from './link-preview-card.component';

@Component({
  selector: 'app-resource-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, LinkPreviewCardComponent],
  template: `
    <section>
      <div class="lv-section-toolbar mb-3">
        <div class="min-w-0">
          <h2 class="lv-section-title mb-1">{{ title }}</h2>
          <p class="lv-muted mb-0">Browse, filter, favorite, archive and open resources.</p>
        </div>

        <button class="btn lv-blue-action" type="button" (click)="load()">
          <span class="material-symbols-outlined me-1" style="font-size:18px">refresh</span>
          Refresh
        </button>
      </div>

      <div class="row g-2 align-items-center mb-3">
        <div class="col-lg">
          <div class="position-relative">
            <span class="material-symbols-outlined lv-input-icon">search</span>
            <input
              class="form-control lv-input-with-icon"
              name="keyword"
              placeholder="Search title, URL, content..."
              [(ngModel)]="keyword"
              (keyup.enter)="load()"
            />
          </div>
        </div>

        <div class="col-sm-6 col-lg-3 col-xl-2">
          <select class="form-select" name="type" [(ngModel)]="type" (change)="load()">
            <option value="">All types</option>
            <option *ngFor="let item of types" [value]="item">{{ item }}</option>
          </select>
        </div>

        <div class="col-sm-6 col-lg-auto">
          <label class="btn lv-blue-action w-100 d-flex align-items-center justify-content-center gap-2">
            <input
              class="form-check-input m-0"
              name="favoriteOnly"
              type="checkbox"
              [(ngModel)]="favoriteOnly"
              (change)="load()"
            />
            Favorites
          </label>
        </div>

        <div class="col-lg-auto">
          <button class="btn lv-blue-action w-100" type="button" (click)="load()">
            Search
          </button>
        </div>
      </div>

      <div *ngIf="error" class="alert alert-danger">{{ error }}</div>

      <div *ngIf="loading" class="lv-card p-4">
        <span class="spinner-border spinner-border-sm me-2"></span>
        Loading resources...
      </div>

      <div *ngIf="!error && !loading && resources.length === 0" class="lv-empty-state">
        <span class="material-symbols-outlined d-block mb-2" style="font-size:36px">add_circle</span>
        <strong>No resources here yet.</strong>
        <p class="mb-0">Use the toolbar above to create a link, note, snippet or upload a file.</p>
      </div>

      <div class="row g-4" *ngIf="!loading && resources.length > 0">
        <div class="col-md-6 col-xxl-4" *ngFor="let resource of resources; trackBy: trackById">
          <article class="lv-card lv-card-hover lv-resource-card p-4 h-100">
            <div class="d-flex justify-content-between align-items-start mb-3">
              <span class="lv-icon-box" [ngClass]="accentFor(resource.resourceType)">
                <span class="material-symbols-outlined">{{ iconFor(resource.resourceType) }}</span>
              </span>

              <div class="dropdown lv-resource-actions">
                <button class="lv-icon-button" type="button" data-bs-toggle="dropdown">
                  <span class="material-symbols-outlined">more_vert</span>
                </button>

                <ul class="dropdown-menu dropdown-menu-end border-0 shadow p-2">
                  <li>
                    <a class="dropdown-item rounded-2" [routerLink]="['/resources', resource.id]">
                      Open
                    </a>
                  </li>

                  <li *ngIf="resource.resourceType === 'LINK'">
                    <button
                      class="dropdown-item rounded-2"
                      type="button"
                      (click)="refreshPreview(resource)"
                      [disabled]="refreshingPreviewId === resource.id"
                    >
                      {{ refreshingPreviewId === resource.id ? 'Refreshing preview...' : 'Refresh preview' }}
                    </button>
                  </li>

                  <li>
                    <button class="dropdown-item rounded-2" type="button" (click)="favorite(resource)">
                      {{ resource.isFavorite ? 'Unfavorite' : 'Favorite' }}
                    </button>
                  </li>

                  <li>
                    <button class="dropdown-item rounded-2" type="button" (click)="archive(resource)">
                      {{ resource.isArchived ? 'Unarchive' : 'Archive' }}
                    </button>
                  </li>

                  <li>
                    <button class="dropdown-item rounded-2 text-danger" type="button" (click)="delete(resource)">
                      Delete
                    </button>
                  </li>
                </ul>
              </div>
            </div>

            <a [routerLink]="['/resources', resource.id]" class="text-dark d-block min-w-0">
              <h3 class="lv-section-title fs-5 lv-resource-card-title mb-2">
                {{ resource.title }}
              </h3>
            </a>

            <ng-container *ngIf="resource.resourceType === 'LINK'; else standardSummaryTpl">
              <app-link-preview-card
                class="d-block mb-4"
                [resource]="resource"
                [compact]="true"
                [showActions]="false"
              ></app-link-preview-card>
            </ng-container>

            <ng-template #standardSummaryTpl>
              <p class="lv-muted lv-line-clamp-2 lv-resource-summary mb-4">
                {{ resource.description || resource.url || resource.fileName || 'No description' }}
              </p>
            </ng-template>

            <div class="d-flex flex-wrap gap-2 mb-4 min-w-0">
              <span class="badge rounded-pill lv-badge-soft lv-chip">
                {{ resource.resourceType }}
              </span>

              <span *ngIf="resource.isFavorite" class="badge rounded-pill text-bg-warning lv-chip">
                Favorite
              </span>

              <span *ngIf="resource.isArchived" class="badge rounded-pill text-bg-secondary lv-chip">
                Archived
              </span>

              <span *ngFor="let tag of resource.tags" class="badge rounded-pill text-bg-light border lv-chip">
                <span class="lv-chip-label">{{ tag.name }}</span>
              </span>
            </div>

            <div class="lv-card-footer mt-auto pt-3 border-top">
              <small class="lv-muted text-truncate">
                Updated {{ resource.updatedAt | date:'mediumDate' }}
              </small>

              <a class="btn btn-sm lv-blue-action" [routerLink]="['/resources', resource.id]">
                Open
              </a>
            </div>
          </article>
        </div>
      </div>
    </section>
  `
})
export class ResourceListComponent implements OnInit, OnChanges {
  @Input() title = 'Resources';
  @Input() vaultId?: string | null;
  @Input() folderId?: string | null;
  @Input() rootOnly = false;
  @Input() initialKeyword = '';

  protected resources: Resource[] = [];
  protected loading = false;
  protected error = '';
  protected keyword = '';
  protected type: ResourceType | '' = '';
  protected favoriteOnly = false;
  protected refreshingPreviewId = '';
  protected readonly types: ResourceType[] = ['LINK', 'FILE', 'NOTE', 'SNIPPET'];

  private readonly resourceService = inject(ResourceService);
  private loadRequestId = 0;

  ngOnInit(): void {
    this.keyword = this.initialKeyword.trim();
    this.load();
  }

  ngOnChanges(changes: SimpleChanges): void {
    const keywordChanged = changes['initialKeyword'] && !changes['initialKeyword'].firstChange;

    if (keywordChanged) {
      this.keyword = this.initialKeyword.trim();
      this.load();
      return;
    }

    const vaultChanged = changes['vaultId'] && !changes['vaultId'].firstChange;
    const folderChanged = changes['folderId'] && !changes['folderId'].firstChange;
    const rootOnlyChanged = changes['rootOnly'] && !changes['rootOnly'].firstChange;

    if (vaultChanged || folderChanged || rootOnlyChanged) {
      this.load();
    }
  }

  setKeyword(keyword: string): void {
    this.keyword = keyword.trim();
    this.load();
  }

  load(): void {
    const requestId = ++this.loadRequestId;

    this.loading = true;
    this.error = '';

    const params: ResourceSearchParams = {
      keyword: this.keyword.trim(),
      type: this.type,
      favorite: this.favoriteOnly || undefined,
      vaultId: this.vaultId ?? undefined,
      folderId: this.folderId ?? undefined,
      rootOnly: this.rootOnly || undefined
    };

    const hasSearch = Boolean(
      params.keyword ||
      params.type ||
      params.favorite ||
      params.rootOnly ||
      params.tagId
    );

    const action = hasSearch
      ? this.resourceService.search(params)
      : this.folderId
        ? this.resourceService.listByFolder(this.folderId)
        : this.vaultId
          ? this.resourceService.listByVault(this.vaultId)
          : this.resourceService.list();

    action.subscribe({
      next: (resources) => {
        if (requestId !== this.loadRequestId) {
          return;
        }

        this.loading = false;
        this.resources = resources;
      },
      error: (error) => {
        if (requestId !== this.loadRequestId) {
          return;
        }

        this.loading = false;
        this.error = error instanceof Error ? error.message : 'Could not load resources';
      }
    });
  }

  protected favorite(resource: Resource): void {
    this.resourceService.toggleFavorite(resource.id).subscribe({
      next: (updated) => {
        if (this.favoriteOnly && !updated.isFavorite) {
          this.resources = this.resources.filter((item) => item.id !== updated.id);
          return;
        }

        this.replace(updated);
      },
      error: (error) => {
        this.error = error instanceof Error ? error.message : 'Could not update favorite';
      }
    });
  }

  protected archive(resource: Resource): void {
    this.resourceService.toggleArchive(resource.id).subscribe({
      next: (updated) => this.replace(updated),
      error: (error) => {
        this.error = error instanceof Error ? error.message : 'Could not update archive';
      }
    });
  }

  protected delete(resource: Resource): void {
    if (!confirm(`Delete resource "${resource.title}"?`)) {
      return;
    }

    this.resourceService.delete(resource.id).subscribe({
      next: () => {
        this.resources = this.resources.filter((item) => item.id !== resource.id);
      },
      error: (error) => {
        this.error = error instanceof Error ? error.message : 'Could not delete resource';
      }
    });
  }

  protected refreshPreview(resource: Resource): void {
    if (resource.resourceType !== 'LINK' || this.refreshingPreviewId) {
      return;
    }

    this.refreshingPreviewId = resource.id;
    this.error = '';

    this.resourceService.refreshLinkPreview(resource.id).subscribe({
      next: (updated) => {
        this.refreshingPreviewId = '';
        this.replace(updated);
      },
      error: (error) => {
        this.refreshingPreviewId = '';
        this.error = error instanceof Error ? error.message : 'Could not refresh link preview';
      }
    });
  }

  protected iconFor(type: ResourceType): string {
    switch (type) {
      case 'LINK':
        return 'link';
      case 'FILE':
        return 'draft';
      case 'NOTE':
        return 'notes';
      case 'SNIPPET':
        return 'code';
    }
  }

  protected accentFor(type: ResourceType): string {
    switch (type) {
      case 'LINK':
        return 'lv-badge-soft';
      case 'FILE':
        return 'lv-badge-error';
      case 'NOTE':
        return 'lv-badge-tertiary';
      case 'SNIPPET':
        return 'lv-badge-secondary';
    }
  }

  protected trackById(_index: number, resource: Resource): string {
    return resource.id;
  }

  private replace(resource: Resource): void {
    this.resources = this.resources.map((item) => {
      return item.id === resource.id ? resource : item;
    });
  }
}
