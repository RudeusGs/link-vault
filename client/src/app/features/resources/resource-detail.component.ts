import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

import { Resource, ResourcePreview } from '../../core/models/resource.model';
import { Tag } from '../../core/models/tag.model';
import { ResourceService } from '../../core/services/resource.service';
import { TagService } from '../../core/services/tag.service';

@Component({
  selector: 'app-resource-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="page" *ngIf="resource">
      <header class="page-header">
        <div>
          <p class="eyebrow">{{ resource.resourceType }}</p>
          <h1>{{ resource.title }}</h1>
          <p class="muted">{{ resource.description }}</p>
        </div>
        <a class="btn" [routerLink]="resource.folderId ? ['/folders', resource.folderId] : ['/vaults', resource.vaultId]">
          Back
        </a>
      </header>

      <div *ngIf="error" class="error">{{ error }}</div>

      <section class="grid cols-2">
        <article class="panel stack">
          <div class="row wrap">
            <span class="pill">{{ resource.vaultName }}</span>
            <span *ngIf="resource.folderName" class="pill">{{ resource.folderName }}</span>
            <span *ngIf="resource.isFavorite" class="pill">Favorite</span>
            <span *ngIf="resource.isArchived" class="pill">Archived</span>
          </div>

          <div class="row wrap">
            <button class="btn" type="button" (click)="favorite()">
              {{ resource.isFavorite ? 'Unfavorite' : 'Favorite' }}
            </button>
            <button class="btn" type="button" (click)="archive()">
              {{ resource.isArchived ? 'Unarchive' : 'Archive' }}
            </button>
          </div>

          <div class="stack">
            <h2>Tags</h2>
            <div class="row wrap">
              <span *ngFor="let tag of resource.tags" class="pill">
                {{ tag.name }}
                <button class="tag-remove" type="button" (click)="detachTag(tag)">x</button>
              </span>
            </div>
            <div class="row">
              <select name="tagId" [(ngModel)]="selectedTagId">
                <option value="">Choose tag</option>
                <option *ngFor="let tag of tags" [value]="tag.id">{{ tag.name }}</option>
              </select>
              <button class="btn" type="button" (click)="attachTag()">Attach</button>
            </div>
          </div>
        </article>

        <article class="panel stack">
          <h2>Details</h2>
          <p><strong>Type:</strong> {{ resource.resourceType }}</p>
          <p *ngIf="resource.url"><strong>URL:</strong> <a [href]="resource.url" target="_blank">{{ resource.url }}</a></p>
          <p *ngIf="resource.fileName"><strong>File:</strong> {{ resource.fileName }}</p>
          <p *ngIf="resource.mimeType"><strong>MIME:</strong> {{ resource.mimeType }}</p>
          <p *ngIf="resource.fileSize"><strong>Size:</strong> {{ resource.fileSize | number }} bytes</p>
        </article>
      </section>

      <section class="panel stack">
        <div class="page-header">
          <div>
            <p class="eyebrow">Preview</p>
            <h2>{{ preview?.supported === false ? 'Preview unavailable' : 'Resource preview' }}</h2>
          </div>
          <a *ngIf="openUrl" class="btn primary" [href]="openUrl" target="_blank">Open</a>
        </div>

        <ng-container [ngSwitch]="resource.resourceType">
          <p *ngSwitchCase="'LINK'" class="muted">
            Open the link in a new tab: <a [href]="resource.url" target="_blank">{{ resource.url }}</a>
          </p>

          <div *ngSwitchCase="'NOTE'" class="note-content">{{ resource.content }}</div>

          <pre *ngSwitchCase="'SNIPPET'"><code>{{ resource.content }}</code></pre>

          <div *ngSwitchCase="'FILE'" class="file-preview">
            <img *ngIf="isImage()" [src]="preview?.previewUrl" [alt]="resource.title" />
            <iframe *ngIf="isPdf() && safePreviewUrl" [src]="safePreviewUrl" title="PDF preview"></iframe>
            <pre *ngIf="isTextPreview()"><code>{{ textPreview }}</code></pre>
            <p *ngIf="preview && !preview.supported" class="muted">{{ preview.reason }}</p>
            <p *ngIf="textError" class="muted">{{ textError }}</p>
          </div>
        </ng-container>
      </section>
    </section>
  `,
  styles: [
    `
      .tag-remove {
        border: 0;
        background: transparent;
        color: inherit;
        font-weight: 800;
      }

      .note-content {
        white-space: pre-wrap;
        line-height: 1.6;
      }

      .file-preview img {
        display: block;
        max-width: 100%;
        max-height: 70dvh;
        border-radius: 8px;
      }

      .file-preview iframe {
        width: 100%;
        min-height: 70dvh;
        border: 1px solid #d8e4df;
        border-radius: 8px;
      }
    `
  ]
})
export class ResourceDetailComponent implements OnInit {
  protected resource?: Resource;
  protected preview?: ResourcePreview;
  protected tags: Tag[] = [];
  protected selectedTagId = '';
  protected safePreviewUrl?: SafeResourceUrl;
  protected textPreview = '';
  protected textError = '';
  protected error = '';

  private readonly route = inject(ActivatedRoute);
  private readonly resourceService = inject(ResourceService);
  private readonly tagService = inject(TagService);
  private readonly http = inject(HttpClient);
  private readonly sanitizer = inject(DomSanitizer);

  get openUrl(): string | undefined {
    return this.resource?.resourceType === 'FILE' ? this.resource.fileUrl : this.resource?.url;
  }

  ngOnInit(): void {
    this.loadTags();
    this.loadResource();
  }

  protected favorite(): void {
    if (!this.resource) {
      return;
    }

    this.resourceService.toggleFavorite(this.resource.id).subscribe({
      next: (resource) => (this.resource = resource),
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not update favorite')
    });
  }

  protected archive(): void {
    if (!this.resource) {
      return;
    }

    this.resourceService.toggleArchive(this.resource.id).subscribe({
      next: (resource) => (this.resource = resource),
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not update archive')
    });
  }

  protected attachTag(): void {
    if (!this.resource || !this.selectedTagId) {
      return;
    }

    this.resourceService.attachTag(this.resource.id, this.selectedTagId).subscribe({
      next: (resource) => {
        this.resource = resource;
        this.selectedTagId = '';
      },
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not attach tag')
    });
  }

  protected detachTag(tag: Tag): void {
    if (!this.resource) {
      return;
    }

    this.resourceService.detachTag(this.resource.id, tag.id).subscribe({
      next: (resource) => (this.resource = resource),
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not remove tag')
    });
  }

  protected isImage(): boolean {
    return this.preview?.mimeType?.startsWith('image/') ?? false;
  }

  protected isPdf(): boolean {
    return this.preview?.mimeType === 'application/pdf';
  }

  protected isTextPreview(): boolean {
    if (!this.preview?.supported || !this.preview.previewUrl) {
      return false;
    }

    const mime = this.preview.mimeType ?? '';
    const name = this.preview.fileName ?? '';
    return (
      mime.startsWith('text/') ||
      mime === 'application/json' ||
      ['.txt', '.md', '.json', '.java', '.ts', '.js', '.html', '.css'].some((ext) =>
        name.toLowerCase().endsWith(ext)
      )
    );
  }

  private loadResource(): void {
    const resourceId = this.route.snapshot.paramMap.get('resourceId');
    if (!resourceId) {
      return;
    }

    this.resourceService.get(resourceId).subscribe({
      next: (resource) => {
        this.resource = resource;
        this.resourceService.recordView(resource.id).subscribe();
        this.loadPreview(resource.id);
      },
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not load resource')
    });
  }

  private loadTags(): void {
    this.tagService.list().subscribe({
      next: (tags) => (this.tags = tags),
      error: () => (this.tags = [])
    });
  }

  private loadPreview(resourceId: string): void {
    this.resourceService.preview(resourceId).subscribe({
      next: (preview) => {
        this.preview = preview;
        this.safePreviewUrl = preview.previewUrl
          ? this.sanitizer.bypassSecurityTrustResourceUrl(preview.previewUrl)
          : undefined;
        this.loadTextPreview();
      },
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not load preview')
    });
  }

  private loadTextPreview(): void {
    this.textPreview = '';
    this.textError = '';

    if (!this.isTextPreview() || !this.preview?.previewUrl) {
      return;
    }

    this.http.get(this.preview.previewUrl, { responseType: 'text' }).subscribe({
      next: (text) => (this.textPreview = text),
      error: () => (this.textError = 'Text preview could not be loaded. Use Open instead.')
    });
  }
}
