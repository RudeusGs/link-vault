import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { DashboardService } from './data-access/dashboard.service';
import { DashboardSummary } from './models/dashboard-summary.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section>
      <div class="lv-page-header">
        <div class="lv-page-header-copy">
          <div class="lv-page-kicker">Overview</div>
          <h1 class="lv-page-title">Dashboard</h1>
          <p class="lv-muted fs-6 mb-0">A clean summary of the active workspace.</p>
        </div>
        <div class="lv-action-toolbar">
          <a class="btn lv-button-quiet" routerLink="/workspaces">
            <span class="material-symbols-outlined me-1" style="font-size:18px">corporate_fare</span>
            Workspace
          </a>
          <a class="btn btn-primary" routerLink="/vaults">
            <span class="material-symbols-outlined me-1" style="font-size:18px">add</span>
            New vault
          </a>
        </div>
      </div>

      <div *ngIf="error" class="alert alert-danger">{{ error }}</div>

      <div *ngIf="loading" class="lv-card p-4">
        <span class="spinner-border spinner-border-sm me-2"></span>
        Loading dashboard...
      </div>

      <ng-container *ngIf="!loading && summary">
        <section class="lv-card p-4 mb-4">
          <div class="row g-3 align-items-stretch">
            <div class="col-md-6 col-xl-3" *ngFor="let item of primaryMetrics">
              <div class="lv-soft-panel p-3 h-100">
                <div class="d-flex align-items-center justify-content-between mb-3">
                  <span class="lv-icon-box"><span class="material-symbols-outlined">{{ item.icon }}</span></span>
                  <span class="badge rounded-pill lv-badge-secondary">{{ item.hint }}</span>
                </div>
                <div class="lv-muted fw-semibold mb-1">{{ item.label }}</div>
                <div class="display-6 fw-bold lh-1">{{ item.value }}</div>
              </div>
            </div>
          </div>
        </section>

        <section class="row g-4 mb-4">
          <div class="col-lg-8">
            <article class="lv-card p-4 h-100">
              <div class="lv-section-toolbar mb-3">
                <div>
                  <h2 class="lv-section-title mb-1">Recent resources</h2>
                  <p class="lv-muted mb-0">Newest items saved in the active workspace.</p>
                </div>
                <a routerLink="/resources" class="btn btn-sm lv-button-quiet">View all</a>
              </div>

              <div *ngIf="summary.recentResources.length === 0" class="lv-empty-state">
                <span class="material-symbols-outlined d-block mb-2" style="font-size:36px">inventory_2</span>
                <h3 class="lv-section-title mb-2">No resources yet</h3>
                <p class="mb-3">Create a vault, then add links, files, notes, or snippets.</p>
                <a class="btn btn-primary" routerLink="/vaults">Create first vault</a>
              </div>

              <div class="list-group list-group-flush" *ngIf="summary.recentResources.length > 0">
                <a *ngFor="let resource of summary.recentResources" class="list-group-item list-group-item-action px-0 py-3 border-bottom" [routerLink]="['/resources', resource.id]">
                  <div class="d-flex align-items-center gap-3">
                    <span class="lv-icon-box"><span class="material-symbols-outlined">{{ iconFor(resource.resourceType) }}</span></span>
                    <div class="min-w-0 flex-grow-1">
                      <div class="d-flex align-items-center gap-2 mb-1 min-w-0">
                        <strong class="text-truncate min-w-0">{{ resource.title }}</strong>
                        <span class="badge rounded-pill lv-badge-soft flex-shrink-0">{{ resource.resourceType }}</span>
                      </div>
                      <small class="lv-muted text-truncate d-block">
                        {{ resource.vaultName || 'Vault' }}<span *ngIf="resource.folderName"> / {{ resource.folderName }}</span>
                      </small>
                    </div>
                    <span class="material-symbols-outlined lv-muted">chevron_right</span>
                  </div>
                </a>
              </div>
            </article>
          </div>

          <div class="col-lg-4">
            <article class="lv-card p-4 h-100">
              <div class="lv-section-toolbar mb-3">
                <div>
                  <h2 class="lv-section-title mb-1">Top tags</h2>
                  <p class="lv-muted mb-0">Useful labels in this workspace.</p>
                </div>
                <a routerLink="/tags" class="btn btn-sm lv-button-quiet">Manage</a>
              </div>

              <div *ngIf="summary.topTags.length === 0" class="lv-empty-state py-4">
                <span class="material-symbols-outlined d-block mb-2">sell</span>
                No tags yet.
              </div>

              <div class="d-grid gap-2" *ngIf="summary.topTags.length > 0">
                <a routerLink="/tags" class="lv-soft-panel p-3 d-flex align-items-center justify-content-between gap-2" *ngFor="let tag of summary.topTags">
                  <span class="d-flex align-items-center gap-2 min-w-0">
                    <span class="material-symbols-outlined lv-primary" style="font-size:18px">sell</span>
                    <strong class="text-truncate">{{ tag.name }}</strong>
                  </span>
                  <span class="badge rounded-pill lv-badge-secondary">{{ tag.usageCount }}</span>
                </a>
              </div>
            </article>
          </div>
        </section>

        <section class="row g-3">
          <div class="col-6 col-md-3" *ngFor="let item of secondaryMetrics">
            <article class="lv-card p-3 h-100">
              <div class="lv-muted fw-semibold mb-1">{{ item.label }}</div>
              <div class="fs-4 fw-bold">{{ item.value }}</div>
            </article>
          </div>
        </section>
      </ng-container>
    </section>
  `
})
export class DashboardComponent implements OnInit {
  protected summary?: DashboardSummary;
  protected loading = true;
  protected error = '';

  private readonly dashboardService = inject(DashboardService);

  protected get primaryMetrics() {
    if (!this.summary) return [];
    return [
      { label: 'Vaults', value: this.summary.totalVaults, icon: 'folder_special', hint: 'collections' },
      { label: 'Resources', value: this.summary.totalResources, icon: 'inventory_2', hint: 'items' },
      { label: 'Favorites', value: this.summary.totalFavorites, icon: 'star', hint: 'saved' },
      { label: 'Folders', value: this.summary.totalFolders, icon: 'folder', hint: 'structure' }
    ];
  }

  protected get secondaryMetrics() {
    if (!this.summary) return [];
    return [
      { label: 'Links', value: this.summary.totalLinks },
      { label: 'Files', value: this.summary.totalFiles },
      { label: 'Notes', value: this.summary.totalNotes },
      { label: 'Snippets', value: this.summary.totalSnippets }
    ];
  }

  ngOnInit(): void {
    this.loading = true;
    this.error = '';
    this.dashboardService.getSummary().subscribe({
      next: (summary) => {
        this.summary = summary;
        this.loading = false;
      },
      error: (error) => {
        this.error = error instanceof Error ? error.message : 'Could not load dashboard';
        this.loading = false;
      }
    });
  }

  protected iconFor(type: string): string {
    switch (type) {
      case 'LINK': return 'link';
      case 'FILE': return 'draft';
      case 'NOTE': return 'notes';
      case 'SNIPPET': return 'code';
      default: return 'inventory_2';
    }
  }
}
