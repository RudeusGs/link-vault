import { CommonModule } from '@angular/common';
import { Component, Input, OnChanges, SimpleChanges, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Resource, ResourceSearchParams, ResourceType } from '../../core/models/resource.model';
import { ResourceService } from '../../core/services/resource.service';

@Component({
  selector: 'app-resource-list',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="panel stack">
      <div class="page-header">
        <div>
          <p class="eyebrow">Resources</p>
          <h2>{{ title }}</h2>
        </div>
        <button class="btn subtle" type="button" (click)="load()">Refresh</button>
      </div>

      <div class="row wrap">
        <input
          class="search"
          name="keyword"
          placeholder="Search title, URL, content..."
          [(ngModel)]="keyword"
          (keyup.enter)="load()"
        />
        <select name="type" [(ngModel)]="type" (change)="load()">
          <option value="">ALL</option>
          <option *ngFor="let item of types" [value]="item">{{ item }}</option>
        </select>
        <label class="inline-check">
          <input name="favoriteOnly" type="checkbox" [(ngModel)]="favoriteOnly" (change)="load()" />
          Favorites
        </label>
        <button class="btn" type="button" (click)="load()">Search</button>
      </div>

      <div *ngIf="error" class="error">{{ error }}</div>
      <p *ngIf="!error && !loading && resources.length === 0" class="muted">No resources yet.</p>

      <div class="grid">
        <article *ngFor="let resource of resources" class="card resource-card">
          <div class="row">
            <span class="pill">{{ resource.resourceType }}</span>
            <span *ngIf="resource.isArchived" class="pill archived">Archived</span>
            <span class="spacer"></span>
            <button class="btn" type="button" (click)="favorite(resource)">
              {{ resource.isFavorite ? 'Unfavorite' : 'Favorite' }}
            </button>
          </div>

          <h3>
            <a [routerLink]="['/resources', resource.id]">{{ resource.title }}</a>
          </h3>
          <p class="muted">{{ resource.description || resource.url || resource.fileName || 'No description' }}</p>

          <div class="row wrap" *ngIf="resource.tags.length">
            <span *ngFor="let tag of resource.tags" class="pill" [style.background]="tag.color || null">
              {{ tag.name }}
            </span>
          </div>

          <div class="row wrap">
            <button class="btn" type="button" (click)="archive(resource)">
              {{ resource.isArchived ? 'Unarchive' : 'Archive' }}
            </button>
            <button class="btn danger" type="button" (click)="remove(resource)">Delete</button>
          </div>
        </article>
      </div>
    </section>
  `,
  styles: [
    `
      .search {
        max-width: 360px;
      }

      .inline-check {
        display: flex;
        grid-template-columns: auto auto;
        align-items: center;
        gap: 8px;
        font-weight: 700;
      }

      .inline-check input {
        width: auto;
        min-height: auto;
      }

      .resource-card {
        display: grid;
        gap: 12px;
      }

      .archived {
        background: #f7e6df;
        color: #8a4436;
      }
    `
  ]
})
export class ResourceListComponent implements OnChanges {
  @Input() title = 'Resources';
  @Input() vaultId?: string;
  @Input() folderId?: string | null;

  protected readonly types: ResourceType[] = ['LINK', 'FILE', 'NOTE', 'SNIPPET'];
  protected resources: Resource[] = [];
  protected keyword = '';
  protected type: ResourceType | '' = '';
  protected favoriteOnly = false;
  protected loading = false;
  protected error = '';

  private readonly resourceService = inject(ResourceService);

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['vaultId'] || changes['folderId']) {
      this.load();
    }
  }

  load(): void {
    this.loading = true;
    this.error = '';

    const params: ResourceSearchParams = {
      keyword: this.keyword,
      type: this.type,
      vaultId: this.vaultId,
      folderId: this.folderId ?? undefined,
      favorite: this.favoriteOnly ? true : undefined
    };

    this.resourceService.search(params).subscribe({
      next: (resources) => {
        this.resources = resources;
        this.loading = false;
      },
      error: (error) => {
        this.error = error instanceof Error ? error.message : 'Could not load resources';
        this.loading = false;
      }
    });
  }

  protected favorite(resource: Resource): void {
    this.resourceService.toggleFavorite(resource.id).subscribe({
      next: () => this.load(),
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not update favorite')
    });
  }

  protected archive(resource: Resource): void {
    this.resourceService.toggleArchive(resource.id).subscribe({
      next: () => this.load(),
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not update archive')
    });
  }

  protected remove(resource: Resource): void {
    if (!confirm(`Delete "${resource.title}"?`)) {
      return;
    }

    this.resourceService.delete(resource.id).subscribe({
      next: () => this.load(),
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not delete resource')
    });
  }
}
