import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

import { LinkPreview, Resource } from './models/resource.model';

@Component({
  selector: 'app-link-preview-card',
  standalone: true,
  imports: [CommonModule],
  template: `
    <article class="lv-link-preview" [class.compact]="compact">
      <div class="lv-link-preview-media" *ngIf="imageUrl(); else noImageTpl">
        <img [src]="imageUrl()" [alt]="previewTitle()" loading="lazy" />
      </div>
      <ng-template #noImageTpl>
        <div class="lv-link-preview-placeholder">
          <span class="material-symbols-outlined">link</span>
        </div>
      </ng-template>

      <div class="lv-link-preview-body">
        <div class="d-flex align-items-center gap-2 mb-2 min-w-0">
          <img *ngIf="faviconUrl()" class="lv-link-favicon" [src]="faviconUrl()" [alt]="siteLabel()" loading="lazy" />
          <span class="lv-muted small text-truncate">{{ siteLabel() }}</span>
          <span *ngIf="statusLabel()" class="badge rounded-pill lv-chip flex-shrink-0" [class.lv-badge-soft]="isOk()" [class.lv-badge-error]="!isOk()">{{ statusLabel() }}</span>
        </div>

        <h3 class="lv-link-preview-title">{{ previewTitle() }}</h3>
        <p class="lv-muted lv-line-clamp-2 mb-3">{{ previewDescription() }}</p>

        <small *ngIf="linkUrl()" class="lv-muted d-block text-truncate mb-3">{{ linkUrl() }}</small>

        <div *ngIf="showActions" class="lv-link-preview-actions">
          <a *ngIf="linkUrl()" class="btn btn-sm btn-outline-primary" [href]="linkUrl()" target="_blank" rel="noopener">
            Open link
            <span class="material-symbols-outlined ms-1" style="font-size:15px">open_in_new</span>
          </a>
          <button *ngIf="showRefresh" class="btn btn-sm btn-outline-secondary" type="button" (click)="refresh.emit()" [disabled]="refreshing">
            <span *ngIf="refreshing" class="spinner-border spinner-border-sm me-1"></span>
            <span *ngIf="!refreshing" class="material-symbols-outlined me-1" style="font-size:15px">refresh</span>
            Refresh preview
          </button>
        </div>

        <small *ngIf="previewError()" class="text-warning d-block mt-2">{{ previewError() }}</small>
      </div>
    </article>
  `
})
export class LinkPreviewCardComponent {
  @Input() resource?: Resource | null;
  @Input() preview?: LinkPreview | null;
  @Input() compact = false;
  @Input() showActions = true;
  @Input() showRefresh = false;
  @Input() refreshing = false;
  @Output() refresh = new EventEmitter<void>();

  protected imageUrl(): string {
    return this.preview?.thumbnailUrl || this.resource?.thumbnailUrl || '';
  }

  protected faviconUrl(): string {
    return this.preview?.faviconUrl || this.resource?.faviconUrl || '';
  }

  protected previewTitle(): string {
    return this.preview?.previewTitle ||
      this.resource?.previewTitle ||
      this.resource?.title ||
      this.linkUrl() ||
      'Untitled link';
  }

  protected previewDescription(): string {
    return this.preview?.previewDescription ||
      this.resource?.previewDescription ||
      this.resource?.description ||
      this.linkUrl() ||
      'No preview description available.';
  }

  protected siteLabel(): string {
    return this.preview?.siteName ||
      this.preview?.sourceName ||
      this.preview?.domain ||
      this.resource?.siteName ||
      this.resource?.sourceName ||
      this.domainFromUrl(this.linkUrl()) ||
      'Link';
  }

  protected linkUrl(): string {
    return this.preview?.canonicalUrl ||
      this.preview?.url ||
      this.resource?.canonicalUrl ||
      this.resource?.url ||
      '';
  }

  protected previewError(): string {
    return this.preview?.previewError || this.resource?.previewError || '';
  }

  protected statusLabel(): string {
    const status = this.preview?.previewStatus || this.resource?.previewStatus || '';
    return status && status !== 'OK' ? status : '';
  }

  protected isOk(): boolean {
    return (this.preview?.previewStatus || this.resource?.previewStatus || 'OK') === 'OK';
  }

  private domainFromUrl(value: string): string {
    if (!value) {
      return '';
    }

    try {
      const host = new URL(value).hostname.toLowerCase();
      return host.startsWith('www.') ? host.slice(4) : host;
    } catch {
      return '';
    }
  }
}
