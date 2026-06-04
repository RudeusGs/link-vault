import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { DashboardSummary } from '../../core/models/dashboard-summary.model';
import { DashboardService } from '../../core/services/dashboard.service';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <section>
      <div class="lv-page-header mb-4">
        <div class="lv-page-header-copy">
          <h1 class="lv-page-title">Dashboard</h1>
          <p class="lv-muted fs-6 mb-0">Welcome back, manage your personal resource hub.</p>
        </div>
        <a class="btn btn-primary d-inline-flex align-items-center gap-2" routerLink="/vaults">
          <span class="material-symbols-outlined" style="font-size:20px">add</span>
          New Vault
        </a>
      </div>

      <div *ngIf="error" class="alert alert-danger">{{ error }}</div>

      <ng-container *ngIf="!loading && summary">
        <section class="row g-3 mb-4">
          <div class="col-6 col-md-4 col-xl-3" *ngFor="let item of metrics">
            <article class="lv-card lv-card-hover p-4 h-100">
              <div class="d-flex align-items-center justify-content-between mb-3">
                <span class="lv-icon-box" [class.lv-badge-secondary]="item.variant === 'secondary'" [class.lv-badge-tertiary]="item.variant === 'tertiary'">
                  <span class="material-symbols-outlined">{{ item.icon }}</span>
                </span>
                <small class="lv-primary fw-semibold">{{ item.hint }}</small>
              </div>
              <p class="lv-muted fw-semibold mb-1">{{ item.label }}</p>
              <h2 class="lv-page-title fs-2">{{ item.value }}</h2>
            </article>
          </div>
        </section>

        <section class="row g-4">
          <div class="col-xl-8">
            <article class="lv-card p-4 h-100">
              <div class="lv-section-toolbar mb-3">
                <div class="min-w-0">
                  <h2 class="lv-section-title mb-1">Recent resources</h2>
                  <p class="lv-muted mb-0">Latest links, notes, files and snippets.</p>
                </div>
                <a routerLink="/resources" class="btn btn-sm btn-outline-primary">View all</a>
              </div>

              <div *ngIf="summary.recentResources.length === 0" class="lv-empty-state">
                <span class="material-symbols-outlined d-block mb-2" style="font-size:36px">inventory_2</span>
                No resources yet. Create a vault and start saving your first item.
              </div>

              <div class="list-group list-group-flush">
                <a *ngFor="let resource of summary.recentResources" class="list-group-item list-group-item-action px-0 py-3" [routerLink]="['/resources', resource.id]">
                  <div class="d-flex align-items-center gap-3">
                    <span class="lv-icon-box">
                      <span class="material-symbols-outlined">{{ iconFor(resource.resourceType) }}</span>
                    </span>
                    <div class="min-w-0 flex-grow-1">
                      <div class="d-flex flex-wrap align-items-center gap-2 mb-1">
                        <strong class="text-truncate min-w-0">{{ resource.title }}</strong>
                        <span class="badge rounded-pill lv-badge-soft lv-chip flex-shrink-0">{{ resource.resourceType }}</span>
                      </div>
                      <small class="lv-muted text-truncate d-block">
                        {{ resource.vaultName || 'Vault' }} <span *ngIf="resource.folderName">/ {{ resource.folderName }}</span>
                      </small>
                    </div>
                    <span class="material-symbols-outlined lv-muted">chevron_right</span>
                  </div>
                </a>
              </div>
            </article>
          </div>

          <div class="col-xl-4">
            <article class="lv-card p-4 h-100">
              <div class="lv-section-toolbar mb-3">
                <div class="min-w-0">
                  <h2 class="lv-section-title mb-1">Top tags</h2>
                  <p class="lv-muted mb-0">Most used labels.</p>
                </div>
                <a routerLink="/tags" class="btn btn-sm btn-outline-primary">Manage</a>
              </div>

              <div *ngIf="summary.topTags.length === 0" class="lv-empty-state py-4">
                <span class="material-symbols-outlined d-block mb-2">sell</span>
                No tags yet.
              </div>

              <div class="d-flex flex-wrap gap-2">
                <span *ngFor="let tag of summary.topTags" class="badge rounded-pill text-bg-light border px-3 py-2 lv-chip">
                  <span class="material-symbols-outlined me-1" style="font-size:14px">sell</span>
                  <span class="lv-chip-label">{{ tag.name }} · {{ tag.usageCount }}</span>
                </span>
              </div>
            </article>
          </div>
        </section>
      </ng-container>

      <div *ngIf="loading" class="lv-card p-4">
        <span class="spinner-border spinner-border-sm me-2"></span>
        Loading dashboard...
      </div>
    </section>
  `
})
export class DashboardComponent implements OnInit {
  protected summary?: DashboardSummary;
  protected loading = true;
  protected error = '';

  private readonly dashboardService = inject(DashboardService);

  get metrics() {
    if (!this.summary) {
      return [];
    }

    return [
      { label: 'Vaults', value: this.summary.totalVaults, icon: 'account_balance_wallet', hint: '+ collections', variant: 'primary' },
      { label: 'Folders', value: this.summary.totalFolders, icon: 'folder', hint: 'organized', variant: 'secondary' },
      { label: 'Resources', value: this.summary.totalResources, icon: 'inventory_2', hint: 'total items', variant: 'tertiary' },
      { label: 'Favorites', value: this.summary.totalFavorites, icon: 'star', hint: 'saved', variant: 'primary' },
      { label: 'Links', value: this.summary.totalLinks, icon: 'link', hint: 'urls', variant: 'primary' },
      { label: 'Files', value: this.summary.totalFiles, icon: 'draft', hint: 'uploads', variant: 'secondary' },
      { label: 'Notes', value: this.summary.totalNotes, icon: 'notes', hint: 'ideas', variant: 'tertiary' },
      { label: 'Snippets', value: this.summary.totalSnippets, icon: 'code', hint: 'code', variant: 'primary' }
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
      case 'LINK':
        return 'link';
      case 'FILE':
        return 'draft';
      case 'NOTE':
        return 'notes';
      case 'SNIPPET':
        return 'code';
      default:
        return 'inventory_2';
    }
  }
}
