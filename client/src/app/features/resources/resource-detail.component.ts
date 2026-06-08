import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnDestroy, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';

import { TagService } from '../tags/data-access/tag.service';
import { Tag } from '../tags/models/tag.model';
import { ResourceService } from './data-access/resource.service';
import { DocumentPreview, Resource, ResourcePreview, ResourceRequest } from './models/resource.model';
import { LinkPreviewCardComponent } from './link-preview-card.component';

const IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'webp', 'gif', 'svg'];
const TEXT_PREVIEW_EXTENSIONS = [
  'txt', 'md', 'csv', 'json', 'xml', 'yaml', 'yml', 'log',
  'java', 'kt', 'py', 'ts', 'tsx', 'js', 'jsx', 'html', 'css', 'scss',
  'sql', 'sh', 'ps1', 'c', 'cpp', 'h', 'hpp', 'cs', 'go', 'rs', 'php',
  'rb', 'swift', 'dart'
];

type FileTab = 'preview' | 'insights';

@Component({
  selector: 'app-resource-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, LinkPreviewCardComponent],
  template: `
    <section *ngIf="resource; else loadingTpl">
      <div class="lv-page-header mb-4">
        <div class="lv-page-header-copy">
          <button class="btn btn-link px-0 lv-primary fw-semibold" type="button" (click)="goBack()">
            <span class="material-symbols-outlined me-1" style="font-size:18px">arrow_back</span>
            Back
          </button>
          <div class="d-flex align-items-center gap-3 flex-wrap">
            <h1 class="lv-page-title lv-break-title">{{ resource.title }}</h1>
            <span class="badge rounded-pill lv-badge-soft px-3 py-2">{{ resource.resourceType }}</span>
            <span *ngIf="resource.isFavorite" class="badge rounded-pill text-bg-warning px-3 py-2">Favorite</span>
            <span *ngIf="resource.isArchived" class="badge rounded-pill text-bg-secondary px-3 py-2">Archived</span>
          </div>
          <p class="lv-muted mt-2 mb-0 lv-line-clamp-3 text-break">{{ resource.description || 'No description' }}</p>
        </div>

        <div class="lv-action-toolbar">
          <button class="btn btn-outline-warning" type="button" (click)="favorite()">
            <span class="material-symbols-outlined me-1" style="font-size:18px">{{ resource.isFavorite ? 'star' : 'star_border' }}</span>
            {{ resource.isFavorite ? 'Unfavorite' : 'Favorite' }}
          </button>
          <button class="btn btn-outline-secondary" type="button" (click)="archive()">
            <span class="material-symbols-outlined me-1" style="font-size:18px">archive</span>
            {{ resource.isArchived ? 'Unarchive' : 'Archive' }}
          </button>
          <button class="btn btn-outline-primary" type="button" (click)="openEdit()">
            <span class="material-symbols-outlined me-1" style="font-size:18px">edit</span>
            Edit
          </button>
          <button class="btn btn-outline-danger" type="button" (click)="delete()">
            <span class="material-symbols-outlined me-1" style="font-size:18px">delete</span>
            Delete
          </button>
        </div>
      </div>

      <div *ngIf="error" class="alert alert-danger">{{ error }}</div>

      <div class="row g-4">
        <div class="col-xl-4">
          <article class="lv-card p-4 mb-4">
            <h2 class="lv-section-title mb-3">Details</h2>
            <div class="d-grid gap-3">
              <div class="lv-detail-row"><span class="lv-muted">Vault</span><strong>{{ resource.vaultName }}</strong></div>
              <div class="lv-detail-row" *ngIf="resource.folderName"><span class="lv-muted">Folder</span><strong>{{ resource.folderName }}</strong></div>
              <div class="lv-detail-row"><span class="lv-muted">Type</span><strong>{{ resource.resourceType }}</strong></div>
              <div class="lv-detail-row" *ngIf="resource.fileName"><span class="lv-muted">File</span><strong>{{ resource.fileName }}</strong></div>
              <div class="lv-detail-row" *ngIf="resource.mimeType"><span class="lv-muted">MIME</span><strong>{{ resource.mimeType }}</strong></div>
              <div class="lv-detail-row" *ngIf="resource.fileSize"><span class="lv-muted">Size</span><strong>{{ formatFileSize(resource.fileSize) }}</strong></div>
              <div class="lv-detail-row"><span class="lv-muted">Updated</span><strong>{{ resource.updatedAt | date:'mediumDate' }}</strong></div>
            </div>
          </article>

          <article class="lv-card p-4">
            <div class="d-flex justify-content-between align-items-center mb-3">
              <h2 class="lv-section-title mb-0">Tags</h2>
            </div>
            <div class="d-flex flex-wrap gap-2 mb-3">
              <span *ngFor="let tag of resource.tags" class="badge rounded-pill text-bg-light border px-3 py-2 lv-chip">
                <span class="lv-chip-label">{{ tag.name }}</span>
                <button class="btn btn-sm p-0 ms-1 border-0" type="button" (click)="detachTag(tag)">×</button>
              </span>
              <span *ngIf="resource.tags.length === 0" class="lv-muted">No tags attached.</span>
            </div>
            <div class="input-group">
              <select class="form-select" name="tagId" [(ngModel)]="selectedTagId">
                <option value="">Choose tag</option>
                <option *ngFor="let tag of tags" [value]="tag.id">{{ tag.name }}</option>
              </select>
              <button class="btn btn-outline-primary" type="button" (click)="attachTag()">Attach</button>
            </div>
          </article>
        </div>

        <div class="col-xl-8">
          <article class="lv-card p-4">
            <div class="lv-section-toolbar mb-3">
              <div class="min-w-0">
                <h2 class="lv-section-title mb-1">Resource preview</h2>
                <p class="lv-muted mb-0 lv-line-clamp-2">{{ resource.resourceType === 'FILE' ? 'Secure in-app preview for PDF, DOCX, images and code files.' : 'Preview the saved content or open it externally.' }}</p>
              </div>
              <div class="lv-action-toolbar compact" *ngIf="resource.resourceType === 'FILE'; else nonFileActionsTpl">
                <button class="btn btn-outline-secondary" type="button" (click)="copyOriginalFileLink()" [disabled]="!resource.fileUrl">
                  <span class="material-symbols-outlined me-1" style="font-size:16px">content_copy</span>
                  {{ copyState || 'Copy link' }}
                </button>
                <button class="btn btn-outline-primary" type="button" (click)="downloadFile()" [disabled]="fileLoading">
                  <span class="material-symbols-outlined me-1" style="font-size:16px">download</span>
                  Download
                </button>
                <button class="btn btn-primary" type="button" (click)="openFile()" [disabled]="fileLoading || !canOpenFile()">
                  Open
                  <span class="material-symbols-outlined ms-1" style="font-size:16px">open_in_new</span>
                </button>
              </div>
              <ng-template #nonFileActionsTpl>
                <div *ngIf="resource.resourceType === 'LINK'; else openLinkTpl" class="lv-action-toolbar compact">
                  <button class="btn btn-outline-secondary" type="button" (click)="copyLink()" [disabled]="!resource.url">
                    <span class="material-symbols-outlined me-1" style="font-size:16px">content_copy</span>
                    {{ linkCopyState || 'Copy link' }}
                  </button>
                  <button class="btn btn-outline-primary" type="button" (click)="refreshLinkPreview()" [disabled]="linkPreviewRefreshing">
                    <span *ngIf="linkPreviewRefreshing" class="spinner-border spinner-border-sm me-1"></span>
                    <span *ngIf="!linkPreviewRefreshing" class="material-symbols-outlined me-1" style="font-size:16px">refresh</span>
                    Refresh preview
                  </button>
                  <a *ngIf="openUrl" class="btn btn-primary" [href]="openUrl" target="_blank" rel="noopener">
                    Open link
                    <span class="material-symbols-outlined ms-1" style="font-size:16px">open_in_new</span>
                  </a>
                </div>
              </ng-template>
              <ng-template #openLinkTpl>
                <a *ngIf="openUrl" class="btn btn-primary" [href]="openUrl" target="_blank">
                  Open
                  <span class="material-symbols-outlined ms-1" style="font-size:16px">open_in_new</span>
                </a>
              </ng-template>
            </div>

            <ng-container [ngSwitch]="resource.resourceType">
              <div *ngSwitchCase="'LINK'" class="d-grid gap-3">
                <app-link-preview-card
                  [resource]="resource"
                  [showActions]="true"
                  [showRefresh]="true"
                  [refreshing]="linkPreviewRefreshing"
                  (refresh)="refreshLinkPreview()"
                ></app-link-preview-card>
                <div *ngIf="resource.previewStatus && resource.previewStatus !== 'OK'" class="alert alert-warning mb-0">
                  {{ resource.previewError || 'Preview metadata is not available yet.' }} The original link is still saved and can be opened.
                </div>
              </div>

              <div *ngSwitchCase="'NOTE'" class="lv-soft-panel p-4" style="white-space:pre-wrap;line-height:1.7">{{ resource.content || 'No content' }}</div>

              <pre *ngSwitchCase="'SNIPPET'" class="lv-code-preview"><code>{{ resource.content || '// No code' }}</code></pre>

              <div *ngSwitchCase="'FILE'" class="lv-soft-panel p-3 p-md-4">
                <div class="lv-segmented mb-3" role="tablist">
                  <button class="lv-segmented-item" [class.active]="activeFileTab === 'preview'" type="button" (click)="activeFileTab = 'preview'">
                    <span class="material-symbols-outlined" style="font-size:18px">visibility</span>
                    Preview
                  </button>
                  <button class="lv-segmented-item" [class.active]="activeFileTab === 'insights'" type="button" (click)="activeFileTab = 'insights'">
                    Insights
                  </button>
                </div>

                <div *ngIf="fileLoading || !preview" class="lv-empty-state py-4">
                  <span class="spinner-border spinner-border-sm me-2"></span>
                  Preparing secure file preview...
                </div>

                <ng-container *ngIf="preview && !fileLoading">
                  <ng-container *ngIf="activeFileTab === 'preview'; else fileInsightsTpl">
                    <img *ngIf="isImage()" class="img-fluid rounded-3 bg-white" [src]="previewBlobUrl || ''" [alt]="resource.title" />
                    <iframe *ngIf="isPdf() && safePreviewUrl" class="w-100 rounded-3 border bg-white lv-pdf-frame" [src]="safePreviewUrl" title="PDF preview"></iframe>

                    <article *ngIf="isDocxPreview()" class="lv-doc-page">
                      <div class="d-flex align-items-center justify-content-between gap-3 mb-3 pb-3 border-bottom">
                        <div class="min-w-0">
                          <strong class="d-block text-truncate">{{ documentPreview?.title || resource.title }}</strong>
                          <small class="lv-muted">{{ documentPreview?.paragraphCount || 0 }} paragraphs extracted from DOCX</small>
                        </div>
                        <span class="badge rounded-pill lv-badge-soft lv-chip flex-shrink-0">DOCX Live Preview</span>
                      </div>
                      <div class="lv-doc-content">{{ documentPreview?.plainText || 'No readable text found in this DOCX file.' }}</div>
                    </article>

                    <div *ngIf="isDocxLoading()" class="lv-empty-state py-4">
                      <span class="spinner-border spinner-border-sm me-2"></span>
                      Rendering DOCX preview...
                    </div>

                    <pre *ngIf="isTextPreview() && !textError" class="lv-code-preview"><code>{{ textPreview || 'Loading text preview...' }}</code></pre>

                    <div *ngIf="documentError || textError" class="alert alert-warning mb-3">{{ documentError || textError }}</div>

                    <div *ngIf="(!hasInlinePreview() && !isDocxLoading()) || documentError || textError" class="lv-file-fallback d-flex flex-column flex-sm-row align-items-sm-center justify-content-between gap-3">
                      <div class="d-flex align-items-center gap-3 min-w-0">
                        <span class="lv-icon-box">
                          <span class="material-symbols-outlined">{{ fileIcon() }}</span>
                        </span>
                        <div class="min-w-0">
                          <strong class="d-block text-truncate">{{ preview.fileName || resource.fileName || resource.title }}</strong>
                          <small class="lv-muted d-block text-truncate">{{ preview.mimeType || 'Unknown file type' }}</small>
                          <small class="lv-muted d-block text-break">{{ preview.reason || documentError || textError || 'Inline preview is not available for this file yet.' }}</small>
                        </div>
                      </div>
                      <button class="btn btn-outline-primary flex-shrink-0" type="button" (click)="downloadFile()">
                        Download file
                        <span class="material-symbols-outlined ms-1" style="font-size:16px">download</span>
                      </button>
                    </div>
                  </ng-container>

                  <ng-template #fileInsightsTpl>
                    <div class="row g-3">
                      <div class="col-sm-6 col-xl-4" *ngFor="let item of fileInsights()">
                        <div class="lv-insight-tile h-100">
                          <span class="material-symbols-outlined lv-primary">{{ item.icon }}</span>
                          <small class="lv-muted d-block">{{ item.label }}</small>
                          <strong class="d-block text-break">{{ item.value }}</strong>
                        </div>
                      </div>
                    </div>

                    <div class="lv-demo-strip mt-3">
                      <span class="material-symbols-outlined">verified_user</span>
                      <span>Owner-only preview via backend proxy. No public token is exposed in the iframe.</span>
                    </div>
                  </ng-template>
                </ng-container>
              </div>
            </ng-container>
          </article>
        </div>
      </div>
    </section>

    <div class="lv-modal-backdrop" *ngIf="editOpen" (click)="editOpen = false">
      <section class="lv-modal-card p-4" (click)="$event.stopPropagation()">
        <div class="d-flex justify-content-between align-items-start gap-3 mb-3">
          <h2 class="lv-section-title">Edit resource</h2>
          <button class="lv-icon-button" type="button" (click)="editOpen = false"><span class="material-symbols-outlined">close</span></button>
        </div>
        <form class="row g-3" (ngSubmit)="saveEdit()">
          <div class="col-md-7"><label class="form-label fw-semibold">Title</label><input class="form-control" name="title" required [(ngModel)]="editForm.title" /></div>
          <div class="col-md-5" *ngIf="resource?.resourceType === 'LINK'"><label class="form-label fw-semibold">Source</label><input class="form-control" name="sourceName" [(ngModel)]="editForm.sourceName" /></div>
          <div class="col-12"><label class="form-label fw-semibold">Description</label><textarea class="form-control" name="description" rows="2" [(ngModel)]="editForm.description"></textarea></div>
          <div class="col-12" *ngIf="resource?.resourceType === 'LINK'"><label class="form-label fw-semibold">URL</label><input class="form-control" name="url" type="url" [(ngModel)]="editForm.url" /></div>
          <div class="col-md-5" *ngIf="resource?.resourceType === 'SNIPPET'"><label class="form-label fw-semibold">Language</label><input class="form-control" name="codeLanguage" [(ngModel)]="editForm.codeLanguage" /></div>
          <div class="col-12" *ngIf="resource?.resourceType === 'NOTE' || resource?.resourceType === 'SNIPPET'"><label class="form-label fw-semibold">Content</label><textarea class="form-control" name="content" rows="8" [(ngModel)]="editForm.content"></textarea></div>
          <div class="col-12 lv-form-actions"><button class="btn btn-outline-secondary" type="button" (click)="editOpen = false">Cancel</button><button class="btn btn-primary" type="submit">Save changes</button></div>
        </form>
      </section>
    </div>

    <ng-template #loadingTpl>
      <div class="lv-card p-4">
        <span class="spinner-border spinner-border-sm me-2"></span>
        Loading resource...
      </div>
    </ng-template>
  `
})
export class ResourceDetailComponent implements OnInit, OnDestroy {
  protected resource?: Resource;
  protected preview?: ResourcePreview;
  protected documentPreview?: DocumentPreview;
  protected tags: Tag[] = [];
  protected selectedTagId = '';
  protected safePreviewUrl?: SafeResourceUrl;
  protected previewBlobUrl = '';
  protected textPreview = '';
  protected textError = '';
  protected documentError = '';
  protected documentLoading = false;
  protected fileLoading = false;
  protected copyState = '';
  protected linkCopyState = '';
  protected linkPreviewRefreshing = false;
  protected activeFileTab: FileTab = 'preview';
  protected error = '';
  protected editOpen = false;
  protected editForm: ResourceRequest = this.emptyEditForm();

  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly resourceService = inject(ResourceService);
  private readonly tagService = inject(TagService);
  private readonly sanitizer = inject(DomSanitizer);
  private loadRequestId = 0;
  private fileBlob?: Blob;

  get openUrl(): string | undefined {
    return this.resource?.resourceType === 'FILE'
      ? this.previewBlobUrl || this.resource.fileUrl || undefined
      : this.resource?.canonicalUrl || this.resource?.url || undefined;
  }

  ngOnInit(): void {
    this.loadTags();
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.loadResource()
    });
  }

  ngOnDestroy(): void {
    this.clearBlobUrl();
  }

  protected goBack(): void {
    if (this.resource?.folderId) {
      this.router.navigate(['/folders', this.resource.folderId]);
      return;
    }
    if (this.resource?.vaultId) {
      this.router.navigate(['/vaults', this.resource.vaultId]);
      return;
    }
    this.router.navigate(['/dashboard']);
  }

  protected favorite(): void {
    if (!this.resource) return;
    this.resourceService.toggleFavorite(this.resource.id).subscribe({ next: (resource) => (this.resource = resource), error: (error) => (this.error = error instanceof Error ? error.message : 'Could not update favorite') });
  }

  protected archive(): void {
    if (!this.resource) return;
    this.resourceService.toggleArchive(this.resource.id).subscribe({ next: (resource) => (this.resource = resource), error: (error) => (this.error = error instanceof Error ? error.message : 'Could not update archive') });
  }

  protected delete(): void {
    if (!this.resource || !confirm(`Delete resource "${this.resource.title}"?`)) return;
    const vaultId = this.resource.vaultId;
    const folderId = this.resource.folderId;
    this.resourceService.delete(this.resource.id).subscribe({
      next: () => this.router.navigate(folderId ? ['/folders', folderId] : ['/vaults', vaultId]),
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not delete resource')
    });
  }

  protected openEdit(): void {
    if (!this.resource) return;
    this.editForm = {
      title: this.resource.title,
      description: this.resource.description ?? '',
      resourceType: this.resource.resourceType,
      url: this.resource.url ?? '',
      content: this.resource.content ?? '',
      codeLanguage: this.resource.codeLanguage ?? '',
      sourceName: this.resource.sourceName ?? '',
      thumbnailUrl: this.resource.thumbnailUrl ?? ''
    };
    this.editOpen = true;
  }

  protected saveEdit(): void {
    if (!this.resource) return;
    this.resourceService.update(this.resource.id, this.editForm).subscribe({
      next: (resource) => {
        this.resource = resource;
        this.editOpen = false;
        this.loadPreview(resource.id);
      },
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not update resource')
    });
  }

  protected attachTag(): void {
    if (!this.resource || !this.selectedTagId) return;
    this.resourceService.attachTag(this.resource.id, this.selectedTagId).subscribe({ next: (resource) => { this.resource = resource; this.selectedTagId = ''; }, error: (error) => (this.error = error instanceof Error ? error.message : 'Could not attach tag') });
  }

  protected detachTag(tag: Tag): void {
    if (!this.resource) return;
    this.resourceService.detachTag(this.resource.id, tag.id).subscribe({ next: (resource) => (this.resource = resource), error: (error) => (this.error = error instanceof Error ? error.message : 'Could not remove tag') });
  }

  protected isImage(): boolean {
    if (!this.preview?.supported || !this.previewBlobUrl) return false;
    return this.previewMime().startsWith('image/') || IMAGE_EXTENSIONS.includes(this.previewExtension());
  }

  protected isPdf(): boolean {
    if (!this.preview?.supported || !this.previewBlobUrl) return false;
    return this.previewMime() === 'application/pdf' || this.previewExtension() === 'pdf';
  }

  protected isDocxPreview(): boolean {
    return this.preview?.supported === true && this.previewExtension() === 'docx' && Boolean(this.documentPreview?.supported);
  }

  protected isDocxLoading(): boolean {
    return this.previewExtension() === 'docx' && this.documentLoading;
  }

  protected isTextPreview(): boolean {
    if (!this.preview?.supported || !this.previewBlobUrl) return false;
    const mime = this.previewMime();
    return mime.startsWith('text/') || mime === 'application/json' || TEXT_PREVIEW_EXTENSIONS.includes(this.previewExtension());
  }

  protected hasInlinePreview(): boolean {
    return this.isImage() || (this.isPdf() && Boolean(this.safePreviewUrl)) || this.isDocxPreview() || this.isTextPreview();
  }

  protected fileIcon(): string {
    const extension = this.previewExtension();
    if (extension === 'pdf') return 'picture_as_pdf';
    if (['doc', 'docx'].includes(extension)) return 'description';
    if (['xls', 'xlsx', 'csv'].includes(extension)) return 'table';
    if (['ppt', 'pptx'].includes(extension)) return 'slideshow';
    if (['zip', 'rar', '7z'].includes(extension)) return 'folder_zip';
    if (['mp3', 'wav'].includes(extension)) return 'audio_file';
    if (['mp4', 'webm', 'mov'].includes(extension)) return 'video_file';
    if (TEXT_PREVIEW_EXTENSIONS.includes(extension)) return 'code';
    if (IMAGE_EXTENSIONS.includes(extension)) return 'image';
    return 'draft';
  }

  protected canOpenFile(): boolean {
    return Boolean(this.previewBlobUrl || this.resource?.fileUrl);
  }

  protected openFile(): void {
    const url = this.previewBlobUrl || this.resource?.fileUrl;
    if (!url) return;
    window.open(url, '_blank', 'noopener');
  }

  protected downloadFile(): void {
    if (!this.resource) return;

    if (this.fileBlob) {
      this.saveBlob(this.fileBlob);
      return;
    }

    this.fileLoading = true;
    this.resourceService.downloadFile(this.resource.id).subscribe({
      next: (blob) => {
        this.fileLoading = false;
        this.fileBlob = blob;
        this.ensureBlobUrl(blob);
        this.saveBlob(blob);
      },
      error: (error) => {
        this.fileLoading = false;
        this.error = error instanceof Error ? error.message : 'Could not download file';
      }
    });
  }

  protected copyOriginalFileLink(): void {
    const link = this.resource?.fileUrl || this.openUrl || '';
    if (!link) return;

    this.copyToClipboard(link)
      .then(() => {
        this.copyState = 'Copied';
        window.setTimeout(() => (this.copyState = ''), 1600);
      })
      .catch(() => {
        this.copyState = 'Copy failed';
        window.setTimeout(() => (this.copyState = ''), 1600);
      });
  }

  protected copyLink(): void {
    const link = this.resource?.canonicalUrl || this.resource?.url || '';
    if (!link) return;

    this.copyToClipboard(link)
      .then(() => {
        this.linkCopyState = 'Copied';
        window.setTimeout(() => (this.linkCopyState = ''), 1600);
      })
      .catch(() => {
        this.linkCopyState = 'Copy failed';
        window.setTimeout(() => (this.linkCopyState = ''), 1600);
      });
  }

  protected refreshLinkPreview(): void {
    if (!this.resource || this.resource.resourceType !== 'LINK' || this.linkPreviewRefreshing) {
      return;
    }

    this.linkPreviewRefreshing = true;
    this.error = '';
    this.resourceService.refreshLinkPreview(this.resource.id).subscribe({
      next: (resource) => {
        this.linkPreviewRefreshing = false;
        this.resource = resource;
      },
      error: (error) => {
        this.linkPreviewRefreshing = false;
        this.error = error instanceof Error ? error.message : 'Could not refresh link preview';
      }
    });
  }

  protected fileInsights(): Array<{ icon: string; label: string; value: string }> {
    if (!this.resource) return [];

    return [
      { icon: 'extension', label: 'Format', value: this.previewExtension().toUpperCase() || 'UNKNOWN' },
      { icon: 'deployed_code', label: 'Preview engine', value: this.previewEngineLabel() },
      { icon: 'hard_drive', label: 'Size', value: this.formatFileSize(this.resource.fileSize) },
      { icon: 'cloud_done', label: 'Storage', value: this.resource.storageProvider || 'Cloud storage' },
      { icon: 'lock', label: 'Access', value: 'Authenticated owner only' },
      { icon: 'history', label: 'Updated', value: new Date(this.resource.updatedAt).toLocaleString() }
    ];
  }

  protected formatFileSize(size?: number | null): string {
    if (!size || size <= 0) return 'Unknown';
    if (size < 1024) return `${size} B`;
    if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
  }

  private loadResource(): void {
    const resourceId = this.route.snapshot.paramMap.get('resourceId');
    if (!resourceId) return;

    const requestId = ++this.loadRequestId;
    this.resource = undefined;
    this.preview = undefined;
    this.documentPreview = undefined;
    this.safePreviewUrl = undefined;
    this.clearBlobUrl();
    this.fileBlob = undefined;
    this.textPreview = '';
    this.textError = '';
    this.documentError = '';
    this.documentLoading = false;
    this.fileLoading = false;
    this.linkCopyState = '';
    this.linkPreviewRefreshing = false;
    this.activeFileTab = 'preview';
    this.error = '';

    this.resourceService.get(resourceId).subscribe({
      next: (resource) => {
        if (requestId !== this.loadRequestId) {
          return;
        }

        this.resource = resource;
        this.resourceService.recordView(resource.id).subscribe({ error: () => undefined });
        this.loadPreview(resource.id, requestId);
      },
      error: (error) => {
        if (requestId === this.loadRequestId) {
          this.error = error instanceof Error ? error.message : 'Could not load resource';
        }
      }
    });
  }

  private loadTags(): void {
    this.tagService.list().subscribe({ next: (tags) => (this.tags = tags), error: () => (this.tags = []) });
  }

  private loadPreview(resourceId: string, requestId = this.loadRequestId): void {
    this.resourceService.preview(resourceId).subscribe({
      next: (preview) => {
        if (requestId !== this.loadRequestId) {
          return;
        }

        this.preview = preview;
        if (this.resource?.resourceType === 'FILE' && preview.supported) {
          this.loadFilePreview(resourceId, requestId);
        }
      },
      error: () => undefined
    });
  }

  private loadFilePreview(resourceId: string, requestId: number): void {
    this.fileLoading = true;
    this.textPreview = '';
    this.textError = '';
    this.documentPreview = undefined;
    this.documentError = '';
    this.documentLoading = false;
    this.clearBlobUrl();
    this.fileBlob = undefined;

    this.resourceService.downloadFile(resourceId).subscribe({
      next: (blob) => {
        if (requestId !== this.loadRequestId) {
          return;
        }

        this.fileLoading = false;
        this.fileBlob = blob;
        this.ensureBlobUrl(blob);

        if (this.isTextPreview()) {
          this.loadTextPreview(blob, requestId);
        }
        if (this.previewExtension() === 'docx') {
          this.loadDocumentPreview(resourceId, requestId);
        }
      },
      error: (error) => {
        if (requestId !== this.loadRequestId) {
          return;
        }

        this.fileLoading = false;
        this.textError = error instanceof Error ? error.message : 'File preview could not be loaded.';
      }
    });
  }

  private loadTextPreview(blob: Blob, requestId: number): void {
    this.textPreview = '';
    this.textError = '';
    blob.text()
      .then((text) => {
        if (requestId === this.loadRequestId) {
          this.textPreview = text;
        }
      })
      .catch(() => {
        if (requestId === this.loadRequestId) {
          this.textError = 'Text preview could not be loaded. Use Download instead.';
        }
      });
  }

  private loadDocumentPreview(resourceId: string, requestId: number): void {
    this.documentLoading = true;
    this.resourceService.documentPreview(resourceId).subscribe({
      next: (documentPreview) => {
        if (requestId !== this.loadRequestId) {
          return;
        }

        this.documentLoading = false;
        this.documentPreview = documentPreview;
        this.documentError = documentPreview.reason ?? '';
      },
      error: (error) => {
        if (requestId === this.loadRequestId) {
          this.documentLoading = false;
          this.documentError = error instanceof Error ? error.message : 'DOCX preview could not be loaded.';
        }
      }
    });
  }

  private emptyEditForm(): ResourceRequest {
    return { title: '', description: '', resourceType: 'LINK', url: '', content: '', codeLanguage: '', sourceName: '', thumbnailUrl: '' };
  }

  private ensureBlobUrl(blob: Blob): void {
    this.clearBlobUrl();
    this.previewBlobUrl = URL.createObjectURL(blob);
    this.safePreviewUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.previewBlobUrl);
  }

  private clearBlobUrl(): void {
    if (this.previewBlobUrl) {
      URL.revokeObjectURL(this.previewBlobUrl);
    }
    this.previewBlobUrl = '';
    this.safePreviewUrl = undefined;
  }

  private saveBlob(blob: Blob): void {
    const link = document.createElement('a');
    link.href = this.previewBlobUrl || URL.createObjectURL(blob);
    link.download = this.preview?.fileName || this.resource?.fileName || this.resource?.title || 'resource-file';
    link.rel = 'noopener';
    document.body.appendChild(link);
    link.click();
    link.remove();
  }

  private previewMime(): string {
    return (this.preview?.mimeType ?? '').toLowerCase();
  }

  private previewEngineLabel(): string {
    if (this.isPdf()) return 'PDF iframe via secure blob';
    if (this.isDocxPreview()) return 'DOCX text extraction';
    if (this.isImage()) return 'Image blob preview';
    if (this.isTextPreview()) return 'Text/code reader';
    return 'Download fallback';
  }

  private copyToClipboard(value: string): Promise<void> {
    if (navigator.clipboard?.writeText) {
      return navigator.clipboard.writeText(value);
    }

    const textarea = document.createElement('textarea');
    textarea.value = value;
    textarea.style.position = 'fixed';
    textarea.style.opacity = '0';
    document.body.appendChild(textarea);
    textarea.select();
    const copied = document.execCommand('copy');
    textarea.remove();
    return copied ? Promise.resolve() : Promise.reject(new Error('Copy failed'));
  }

  private previewExtension(): string {
    return this.extensionOf(this.preview?.fileName ?? this.resource?.fileName ?? '');
  }

  private extensionOf(fileName: string): string {
    const dotIndex = fileName.lastIndexOf('.');
    if (dotIndex < 0 || dotIndex === fileName.length - 1) {
      return '';
    }

    return fileName.slice(dotIndex + 1).toLowerCase();
  }
}
