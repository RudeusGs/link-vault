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
    <section class="page">
      <header class="page-header">
        <div>
          <p class="eyebrow">Personal Resource Hub</p>
          <h1>Dashboard</h1>
        </div>
        <a class="btn primary" routerLink="/vaults">Open vaults</a>
      </header>

      <div *ngIf="error" class="error">{{ error }}</div>

      <section class="grid cols-4" *ngIf="summary">
        <article class="card metric" *ngFor="let item of metrics">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </article>
      </section>

      <section class="grid cols-2" *ngIf="summary">
        <article class="panel stack">
          <h2>Recent resources</h2>
          <p *ngIf="summary.recentResources.length === 0" class="muted">No resources yet.</p>
          <a
            *ngFor="let resource of summary.recentResources"
            class="recent-row"
            [routerLink]="['/resources', resource.id]"
          >
            <span class="pill">{{ resource.resourceType }}</span>
            <strong>{{ resource.title }}</strong>
          </a>
        </article>

        <article class="panel stack">
          <h2>Top tags</h2>
          <p *ngIf="summary.topTags.length === 0" class="muted">No tags yet.</p>
          <div class="row wrap">
            <span *ngFor="let tag of summary.topTags" class="pill">
              {{ tag.name }} · {{ tag.usageCount }}
            </span>
          </div>
        </article>
      </section>
    </section>
  `,
  styles: [
    `
      .metric {
        display: grid;
        gap: 10px;
      }

      .metric span {
        color: #60736c;
        font-weight: 700;
      }

      .metric strong {
        font-size: 1.9rem;
      }

      .recent-row {
        display: flex;
        align-items: center;
        gap: 10px;
        padding: 10px;
        border-radius: 8px;
        color: inherit;
        text-decoration: none;
        background: #f5faf8;
      }
    `
  ]
})
export class DashboardComponent implements OnInit {
  protected summary?: DashboardSummary;
  protected error = '';

  private readonly dashboardService = inject(DashboardService);

  get metrics() {
    if (!this.summary) {
      return [];
    }

    return [
      { label: 'Resources', value: this.summary.totalResources },
      { label: 'Links', value: this.summary.totalLinks },
      { label: 'Files', value: this.summary.totalFiles },
      { label: 'Notes', value: this.summary.totalNotes },
      { label: 'Snippets', value: this.summary.totalSnippets },
      { label: 'Favorites', value: this.summary.totalFavorites },
      { label: 'Vaults', value: this.summary.totalVaults },
      { label: 'Folders', value: this.summary.totalFolders }
    ];
  }

  ngOnInit(): void {
    this.dashboardService.getSummary().subscribe({
      next: (summary) => (this.summary = summary),
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not load dashboard')
    });
  }
}
