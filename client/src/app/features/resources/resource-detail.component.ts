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
import { ShareDialogComponent, PublicAccessLevel } from '../../shared/components/share-dialog/share-dialog.component';

const IMAGE_EXTENSIONS = ['jpg', 'jpeg', 'png', 'webp', 'gif', 'svg'];
const TEXT_PREVIEW_EXTENSIONS = [
  'txt', 'md', 'csv', 'json', 'xml', 'yaml', 'yml', 'log',
  'java', 'kt', 'py', 'ts', 'tsx', 'js', 'jsx', 'html', 'css', 'scss',
  'sql', 'sh', 'ps1', 'c', 'cpp', 'h', 'hpp', 'cs', 'go', 'rs', 'php',
  'rb', 'swift', 'dart'
];
const TEXT_PREVIEW_MAX_LINES = 300;
const TEXT_PREVIEW_MAX_BYTES = 200 * 1024;
const DOC_PREVIEW_MAX_CHARS = 30000;
const NOTE_PREVIEW_MAX_CHARS = 50000;

type FileTab = 'preview' | 'insights';

@Component({
  selector: 'app-resource-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, LinkPreviewCardComponent, ShareDialogComponent],
  template: `
    <section *ngIf="resource; else loadingTpl" class="lv-resource-detail-page">
      <header class="lv-resource-detail-hero">
        <div class="lv-resource-detail-main">
          <button class="lv-plain-link" type="button" (click)="goBack()">
            <span class="material-symbols-outlined">arrow_back</span>
            Back to {{ resource.folderName || resource.vaultName || 'workspace' }}
          </button>

          <div class="lv-resource-heading-row">
            <span class="lv-resource-heading-icon">
              <span class="material-symbols-outlined">{{ resourceIcon() }}</span>
            </span>
            <div class="lv-resource-heading-copy">
              <div class="lv-resource-eyebrow">{{ resource.resourceType }} resource</div>
              <h1 class="lv-resource-detail-title">{{ resource.title }}</h1>
              <p class="lv-resource-detail-description">
                {{ resource.description || fallbackDescription() }}
              </p>
            </div>
          </div>

          <div class="lv-resource-status-row">
            <span class="lv-status-pill strong">{{ resource.publicAccess }}</span>
            <span *ngIf="resource.isFavorite" class="lv-status-pill warning">Favorite</span>
            <span *ngIf="resource.isArchived" class="lv-status-pill muted">Archived</span>
            <span class="lv-status-pill">Updated {{ resource.updatedAt | date:'mediumDate' }}</span>
          </div>
        </div>

        <div class="lv-resource-actions">
          <button class="lv-detail-action primary" type="button" (click)="openShareResource()">
            <span class="material-symbols-outlined">share</span>
            Share
          </button>
          <button class="lv-detail-action" type="button" (click)="favorite()">
            <span class="material-symbols-outlined">{{ resource.isFavorite ? 'star' : 'star_border' }}</span>
            {{ resource.isFavorite ? 'Unfavorite' : 'Favorite' }}
          </button>
          <button class="lv-detail-action" type="button" (click)="archive()">
            <span class="material-symbols-outlined">archive</span>
            {{ resource.isArchived ? 'Unarchive' : 'Archive' }}
          </button>
          <button class="lv-detail-action" type="button" (click)="openEdit()">
            <span class="material-symbols-outlined">edit</span>
            Edit
          </button>
          <button class="lv-detail-action danger" type="button" (click)="delete()">
            <span class="material-symbols-outlined">delete</span>
            Delete
          </button>
        </div>
      </header>

      <div *ngIf="error" class="lv-inline-error">
        <span class="material-symbols-outlined">error</span>
        {{ error }}
      </div>

      <main class="lv-resource-detail-layout">
        <aside class="lv-resource-detail-sidebar">
          <article class="lv-detail-side-card">
            <div class="lv-side-card-header">
              <h2>Information</h2>
              <span class="lv-side-card-subtitle">Resource metadata</span>
            </div>

            <div class="lv-meta-list">
              <div class="lv-meta-row">
                <span>Vault</span>
                <strong>{{ resource.vaultName || 'Untitled vault' }}</strong>
              </div>
              <div class="lv-meta-row" *ngIf="resource.folderName">
                <span>Folder</span>
                <strong>{{ resource.folderName }}</strong>
              </div>
              <div class="lv-meta-row">
                <span>Type</span>
                <strong>{{ resource.resourceType }}</strong>
              </div>
              <div class="lv-meta-row" *ngIf="resource.fileName">
                <span>File name</span>
                <strong>{{ resource.fileName }}</strong>
              </div>
              <div class="lv-meta-row" *ngIf="resource.mimeType">
                <span>MIME</span>
                <strong>{{ resource.mimeType }}</strong>
              </div>
              <div class="lv-meta-row" *ngIf="resource.fileSize">
                <span>Size</span>
                <strong>{{ formatFileSize(resource.fileSize) }}</strong>
              </div>
              <div class="lv-meta-row">
                <span>Created</span>
                <strong>{{ resource.createdAt | date:'mediumDate' }}</strong>
              </div>
            </div>
          </article>

          <article class="lv-detail-side-card">
            <div class="lv-side-card-header horizontal">
              <div>
                <h2>Tags</h2>
                <span class="lv-side-card-subtitle">Organize this resource</span>
              </div>
              <span class="lv-count-pill">{{ resource.tags.length }}</span>
            </div>

            <div class="lv-tag-stack" *ngIf="resource.tags.length; else noTagsTpl">
              <span *ngFor="let tag of resource.tags" class="lv-resource-tag">
                <span>{{ tag.name }}</span>
                <button type="button" aria-label="Remove tag" (click)="detachTag(tag)">×</button>
              </span>
            </div>
            <ng-template #noTagsTpl>
              <p class="lv-empty-copy">No tags attached yet.</p>
            </ng-template>

            <div class="lv-attach-tag-row">
              <select class="lv-input" name="tagId" [(ngModel)]="selectedTagId">
                <option value="">Choose tag</option>
                <option *ngFor="let tag of tags" [value]="tag.id">{{ tag.name }}</option>
              </select>
              <button class="lv-detail-action primary compact" type="button" (click)="attachTag()">Attach</button>
            </div>
          </article>
        </aside>

        <section class="lv-resource-preview-panel">
          <div class="lv-preview-topbar">
            <div>
              <h2>Preview</h2>
              <p>{{ previewSubtitle() }}</p>
            </div>

            <div class="lv-preview-actions" *ngIf="resource.resourceType === 'FILE'; else nonFileActionsTpl">
              <button class="lv-detail-action" type="button" (click)="copyOriginalFileLink()" [disabled]="!resource.fileUrl">
                <span class="material-symbols-outlined">content_copy</span>
                {{ copyState || 'Copy link' }}
              </button>
              <button class="lv-detail-action" type="button" (click)="downloadFile()" [disabled]="fileLoading">
                <span class="material-symbols-outlined">download</span>
                Download
              </button>
              <button class="lv-detail-action primary" type="button" (click)="openFile()" [disabled]="fileLoading || !canOpenFile()">
                Open
                <span class="material-symbols-outlined">open_in_new</span>
              </button>
            </div>

            <ng-template #nonFileActionsTpl>
              <div *ngIf="resource.resourceType === 'LINK'; else standardOpenTpl" class="lv-preview-actions">
                <button class="lv-detail-action" type="button" (click)="copyLink()" [disabled]="!resource.url">
                  <span class="material-symbols-outlined">content_copy</span>
                  {{ linkCopyState || 'Copy link' }}
                </button>
                <button class="lv-detail-action" type="button" (click)="refreshLinkPreview()" [disabled]="linkPreviewRefreshing">
                  <span *ngIf="linkPreviewRefreshing" class="lv-mini-spinner"></span>
                  <span *ngIf="!linkPreviewRefreshing" class="material-symbols-outlined">refresh</span>
                  Refresh
                </button>
                <a *ngIf="openUrl" class="lv-detail-action primary" [href]="openUrl" target="_blank" rel="noopener">
                  Open link
                  <span class="material-symbols-outlined">open_in_new</span>
                </a>
              </div>
            </ng-template>

            <ng-template #standardOpenTpl>
              <a *ngIf="openUrl" class="lv-detail-action primary" [href]="openUrl" target="_blank" rel="noopener">
                Open
                <span class="material-symbols-outlined">open_in_new</span>
              </a>
            </ng-template>
          </div>

          <ng-container [ngSwitch]="resource.resourceType">
            <div *ngSwitchCase="'LINK'" class="lv-preview-body simple">
              <app-link-preview-card
                [resource]="resource"
                [showActions]="true"
                [showRefresh]="true"
                [refreshing]="linkPreviewRefreshing"
                (refresh)="refreshLinkPreview()"
              ></app-link-preview-card>
              <div *ngIf="resource.previewStatus && resource.previewStatus !== 'OK'" class="lv-inline-warning">
                <span class="material-symbols-outlined">info</span>
                {{ resource.previewError || 'Preview metadata is not available yet.' }} The original link is still saved.
              </div>
            </div>

            <article *ngSwitchCase="'NOTE'" class="lv-readable-panel">
              <div class="lv-readable-header">
                <span class="material-symbols-outlined">notes</span>
                <strong>Note content</strong>
              </div>
              <div class="lv-readable-content">{{ limitedNoteContent() }}</div>
              <div *ngIf="isNoteTruncated()" class="lv-preview-limit-notice">
                This note is long, so the preview is limited for performance.
              </div>
            </article>

            <section *ngSwitchCase="'SNIPPET'" class="lv-code-panel">
              <div class="lv-code-toolbar">
                <div>
                  <strong>{{ resource.codeLanguage || 'Code snippet' }}</strong>
                  <span>{{ snippetLines().length }} visible lines</span>
                </div>
                <button class="lv-detail-action compact" type="button" (click)="copySnippet()">
                  <span class="material-symbols-outlined">content_copy</span>
                  {{ snippetCopyState || 'Copy' }}
                </button>
              </div>
              <div class="lv-code-window">
                <div *ngFor="let line of snippetLines(); let index = index; trackBy: trackByLineIndex" class="lv-code-line">
                  <span class="lv-line-number">{{ index + 1 }}</span>
                  <code>{{ line || ' ' }}</code>
                </div>
              </div>
              <div *ngIf="isSnippetTruncated()" class="lv-preview-limit-notice">
                Only the first {{ previewLineLimit }} lines are shown to keep the page fast.
              </div>
            </section>

            <div *ngSwitchCase="'FILE'" class="lv-file-preview-shell">
              <div class="lv-preview-tabs" role="tablist">
                <button class="lv-preview-tab" [class.active]="activeFileTab === 'preview'" type="button" (click)="activeFileTab = 'preview'">
                  <span class="material-symbols-outlined">visibility</span>
                  Preview
                </button>
                <button class="lv-preview-tab" [class.active]="activeFileTab === 'insights'" type="button" (click)="activeFileTab = 'insights'">
                  <span class="material-symbols-outlined">analytics</span>
                  Insights
                </button>
              </div>

              <div *ngIf="fileLoading || !preview" class="lv-preview-loading">
                <span class="lv-spinner"></span>
                Preparing secure preview...
              </div>

              <ng-container *ngIf="preview && !fileLoading">
                <ng-container *ngIf="activeFileTab === 'preview'; else fileInsightsTpl">
                  <div *ngIf="isImage()" class="lv-image-preview-frame">
                    <img [src]="previewBlobUrl || ''" [alt]="resource.title" />
                  </div>

                  <iframe *ngIf="isPdf() && safePreviewUrl" class="lv-pdf-preview-frame" [src]="safePreviewUrl" title="PDF preview"></iframe>

                  <article *ngIf="isDocxPreview()" class="lv-doc-preview-panel">
                    <div class="lv-doc-preview-header">
                      <div>
                        <strong>{{ documentPreview?.title || resource.title }}</strong>
                        <span>{{ documentPreview?.paragraphCount || 0 }} paragraphs extracted</span>
                      </div>
                      <span class="lv-status-pill strong">DOCX</span>
                    </div>
                    <div class="lv-doc-preview-content">{{ visibleDocumentText() }}</div>
                    <div *ngIf="isDocumentPreviewTruncated()" class="lv-preview-limit-notice">
                      DOCX preview is limited to {{ docPreviewMaxChars | number }} characters. Download the file to read everything.
                    </div>
                  </article>

                  <div *ngIf="isDocxLoading()" class="lv-preview-loading compact">
                    <span class="lv-spinner"></span>
                    Rendering DOCX preview...
                  </div>

                  <section *ngIf="isTextPreview() && !textError" class="lv-code-panel">
                    <div class="lv-code-toolbar">
                      <div>
                        <strong>{{ preview.fileName || resource.fileName || resource.title }}</strong>
                        <span>{{ textPreviewSummary() }}</span>
                      </div>
                      <button class="lv-detail-action compact" type="button" (click)="copyVisibleTextPreview()" [disabled]="!textPreviewLines.length">
                        <span class="material-symbols-outlined">content_copy</span>
                        {{ textPreviewCopyState || 'Copy visible' }}
                      </button>
                    </div>
                    <div class="lv-code-window limited">
                      <div *ngFor="let line of textPreviewLines; let index = index; trackBy: trackByLineIndex" class="lv-code-line">
                        <span class="lv-line-number">{{ index + 1 }}</span>
                        <code>{{ line || ' ' }}</code>
                      </div>
                    </div>
                    <div *ngIf="textPreviewLines.length === 0" class="lv-empty-inline">No readable text found in this file.</div>
                    <div *ngIf="textPreviewTruncated" class="lv-preview-limit-notice strong">
                      This file is large. Showing only the first {{ previewLineLimit }} lines or {{ previewByteLimitLabel() }} so the browser stays smooth.
                      Use Download/Open for the full file.
                    </div>
                  </section>

                  <div *ngIf="documentError || textError" class="lv-inline-warning">
                    <span class="material-symbols-outlined">info</span>
                    {{ documentError || textError }}
                  </div>

                  <div *ngIf="(!hasInlinePreview() && !isDocxLoading()) || documentError || textError" class="lv-file-fallback">
                    <span class="lv-file-fallback-icon">
                      <span class="material-symbols-outlined">{{ fileIcon() }}</span>
                    </span>
                    <div>
                      <strong>{{ preview.fileName || resource.fileName || resource.title }}</strong>
                      <span>{{ preview.mimeType || 'Unknown file type' }}</span>
                      <p>{{ preview.reason || documentError || textError || 'Inline preview is not available for this file yet.' }}</p>
                    </div>
                    <button class="lv-detail-action primary" type="button" (click)="downloadFile()">
                      <span class="material-symbols-outlined">download</span>
                      Download
                    </button>
                  </div>
                </ng-container>

                <ng-template #fileInsightsTpl>
                  <div class="lv-insight-grid">
                    <div class="lv-insight-tile" *ngFor="let item of fileInsights()">
                      <span class="material-symbols-outlined">{{ item.icon }}</span>
                      <small>{{ item.label }}</small>
                      <strong>{{ item.value }}</strong>
                    </div>
                  </div>

                  <div class="lv-secure-note">
                    <span class="material-symbols-outlined">verified_user</span>
                    <span>Preview is served through the backend proxy. Raw storage URLs stay behind authenticated access.</span>
                  </div>
                </ng-template>
              </ng-container>
            </div>
          </ng-container>
        </section>
      </main>
    </section>

    <div class="lv-modal-backdrop" *ngIf="editOpen" (click)="editOpen = false">
      <section class="lv-modal-card lv-resource-edit-modal" (click)="$event.stopPropagation()">
        <div class="lv-modal-header-clean">
          <div>
            <h2>Edit resource</h2>
            <p>Update metadata without changing the current workspace context.</p>
          </div>
          <button class="lv-icon-button" type="button" (click)="editOpen = false"><span class="material-symbols-outlined">close</span></button>
        </div>

        <form class="lv-edit-form" (ngSubmit)="saveEdit()">
          <label>
            <span>Title</span>
            <input class="lv-input" name="title" required [(ngModel)]="editForm.title" />
          </label>

          <label *ngIf="resource?.resourceType === 'LINK'">
            <span>Source</span>
            <input class="lv-input" name="sourceName" [(ngModel)]="editForm.sourceName" />
          </label>

          <label class="wide">
            <span>Description</span>
            <textarea class="lv-input" name="description" rows="3" [(ngModel)]="editForm.description"></textarea>
          </label>

          <label class="wide" *ngIf="resource?.resourceType === 'LINK'">
            <span>URL</span>
            <input class="lv-input" name="url" type="url" [(ngModel)]="editForm.url" />
          </label>

          <label *ngIf="resource?.resourceType === 'SNIPPET'">
            <span>Language</span>
            <input class="lv-input" name="codeLanguage" [(ngModel)]="editForm.codeLanguage" />
          </label>

          <label class="wide" *ngIf="resource?.resourceType === 'NOTE' || resource?.resourceType === 'SNIPPET'">
            <span>Content</span>
            <textarea class="lv-input mono" name="content" rows="10" [(ngModel)]="editForm.content"></textarea>
          </label>

          <div class="lv-form-actions wide">
            <button class="lv-detail-action" type="button" (click)="editOpen = false">Cancel</button>
            <button class="lv-detail-action primary" type="submit">Save changes</button>
          </div>
        </form>
      </section>
    </div>

    <app-share-dialog
      *ngIf="shareModalOpen && resource"
      [title]="resource.title"
      [currentAccess]="resource.publicAccess"
      [publicUrlPath]="'/public/resources/' + resource.id"
      (close)="shareModalOpen = false"
      (accessChanged)="updateResourceAccess($event)"
    />

    <ng-template #loadingTpl>
      <div class="lv-resource-skeleton">
        <span class="lv-spinner"></span>
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
  protected textPreviewLines: string[] = [];
  protected textPreviewLoadedBytes = 0;
  protected textPreviewSourceBytes = 0;
  protected textPreviewTruncated = false;
  protected textPreviewCopyState = '';
  protected snippetCopyState = '';
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
  protected readonly previewLineLimit = TEXT_PREVIEW_MAX_LINES;
  protected readonly docPreviewMaxChars = DOC_PREVIEW_MAX_CHARS;

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
    this.resourceService.toggleFavorite(this.resource.id).subscribe({
      next: (resource) => (this.resource = resource),
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not update favorite')
    });
  }

  protected archive(): void {
    if (!this.resource) return;
    this.resourceService.toggleArchive(this.resource.id).subscribe({
      next: (resource) => (this.resource = resource),
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not update archive')
    });
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

  protected shareModalOpen = false;

  protected openShareResource(): void {
    this.shareModalOpen = true;
  }

  protected updateResourceAccess(access: PublicAccessLevel): void {
    if (!this.resource) return;

    const updateRequest: ResourceRequest = {
      title: this.resource.title,
      description: this.resource.description || undefined,
      resourceType: this.resource.resourceType,
      url: this.resource.url || undefined,
      content: this.resource.content || undefined,
      codeLanguage: this.resource.codeLanguage || undefined,
      sourceName: this.resource.sourceName || undefined,
      thumbnailUrl: this.resource.thumbnailUrl || undefined,
      publicAccess: access
    };

    this.resourceService.update(this.resource.id, updateRequest).subscribe({
      next: (resource) => {
        this.resource = resource;
        this.shareModalOpen = false;
      },
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not update resource access')
    });
  }

  protected attachTag(): void {
    if (!this.resource || !this.selectedTagId) return;
    this.resourceService.attachTag(this.resource.id, this.selectedTagId).subscribe({
      next: (resource) => {
        this.resource = resource;
        this.selectedTagId = '';
      },
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not attach tag')
    });
  }

  protected detachTag(tag: Tag): void {
    if (!this.resource) return;
    this.resourceService.detachTag(this.resource.id, tag.id).subscribe({
      next: (resource) => (this.resource = resource),
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not remove tag')
    });
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

  protected resourceIcon(): string {
    if (!this.resource) return 'draft';
    if (this.resource.resourceType === 'LINK') return 'link';
    if (this.resource.resourceType === 'NOTE') return 'notes';
    if (this.resource.resourceType === 'SNIPPET') return 'code_blocks';
    return this.fileIcon();
  }

  protected fallbackDescription(): string {
    if (!this.resource) return 'No description provided.';
    if (this.resource.resourceType === 'FILE') return 'File stored securely in this workspace.';
    if (this.resource.resourceType === 'LINK') return 'Saved link with reusable metadata and preview.';
    if (this.resource.resourceType === 'SNIPPET') return 'Saved code snippet for later reference.';
    return 'No description provided.';
  }

  protected previewSubtitle(): string {
    if (!this.resource) return '';
    if (this.resource.resourceType === 'FILE') return 'Fast, limited preview that keeps large files from freezing the browser.';
    if (this.resource.resourceType === 'LINK') return 'Review the saved URL metadata and open the original source.';
    if (this.resource.resourceType === 'SNIPPET') return 'Code viewer with a safe line limit for long snippets.';
    return 'Readable note view with clean spacing.';
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

  protected copyVisibleTextPreview(): void {
    if (!this.textPreviewLines.length) return;
    this.copyToClipboard(this.textPreviewLines.join('\n'))
      .then(() => {
        this.textPreviewCopyState = 'Copied';
        window.setTimeout(() => (this.textPreviewCopyState = ''), 1600);
      })
      .catch(() => {
        this.textPreviewCopyState = 'Copy failed';
        window.setTimeout(() => (this.textPreviewCopyState = ''), 1600);
      });
  }

  protected copySnippet(): void {
    const content = this.resource?.content || '';
    if (!content) return;
    this.copyToClipboard(content)
      .then(() => {
        this.snippetCopyState = 'Copied';
        window.setTimeout(() => (this.snippetCopyState = ''), 1600);
      })
      .catch(() => {
        this.snippetCopyState = 'Copy failed';
        window.setTimeout(() => (this.snippetCopyState = ''), 1600);
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
      { icon: 'visibility', label: 'Preview mode', value: this.previewEngineLabel() },
      { icon: 'hard_drive', label: 'Size', value: this.formatFileSize(this.resource.fileSize) },
      { icon: 'cloud_done', label: 'Storage', value: this.resource.storageProvider || 'Cloud storage' },
      { icon: 'lock', label: 'Access', value: 'Workspace authenticated' },
      { icon: 'history', label: 'Updated', value: new Date(this.resource.updatedAt).toLocaleString() }
    ];
  }

  protected formatFileSize(size?: number | null): string {
    if (!size || size <= 0) return 'Unknown';
    if (size < 1024) return `${size} B`;
    if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
    return `${(size / (1024 * 1024)).toFixed(1)} MB`;
  }

  protected textPreviewSummary(): string {
    if (!this.textPreviewLines.length) return 'No visible lines loaded';
    const bytes = this.textPreviewLoadedBytes ? this.formatFileSize(this.textPreviewLoadedBytes) : 'preview chunk';
    const source = this.textPreviewSourceBytes ? this.formatFileSize(this.textPreviewSourceBytes) : 'unknown size';
    return `${this.textPreviewLines.length} lines shown · ${bytes} of ${source}`;
  }

  protected previewByteLimitLabel(): string {
    return this.formatFileSize(TEXT_PREVIEW_MAX_BYTES);
  }

  protected visibleDocumentText(): string {
    const text = this.documentPreview?.plainText || 'No readable text found in this DOCX file.';
    return text.length > DOC_PREVIEW_MAX_CHARS ? `${text.slice(0, DOC_PREVIEW_MAX_CHARS)}\n\n...` : text;
  }

  protected isDocumentPreviewTruncated(): boolean {
    return (this.documentPreview?.plainText?.length || 0) > DOC_PREVIEW_MAX_CHARS;
  }

  protected limitedNoteContent(): string {
    const content = this.resource?.content || 'No content';
    return content.length > NOTE_PREVIEW_MAX_CHARS ? `${content.slice(0, NOTE_PREVIEW_MAX_CHARS)}\n\n...` : content;
  }

  protected isNoteTruncated(): boolean {
    return (this.resource?.content?.length || 0) > NOTE_PREVIEW_MAX_CHARS;
  }

  protected snippetLines(): string[] {
    const content = this.resource?.content || '// No code';
    return this.toLines(content).slice(0, TEXT_PREVIEW_MAX_LINES);
  }

  protected isSnippetTruncated(): boolean {
    return this.toLines(this.resource?.content || '').length > TEXT_PREVIEW_MAX_LINES;
  }

  protected trackByLineIndex(index: number): number {
    return index;
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
    this.resetTextPreviewState();
    this.documentError = '';
    this.documentLoading = false;
    this.fileLoading = false;
    this.linkCopyState = '';
    this.copyState = '';
    this.snippetCopyState = '';
    this.textPreviewCopyState = '';
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
    this.resetTextPreviewState();
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
    this.resetTextPreviewState();
    const previewBytes = Math.min(blob.size, TEXT_PREVIEW_MAX_BYTES);
    const previewBlob = blob.slice(0, previewBytes);

    previewBlob.text()
      .then((text) => {
        if (requestId !== this.loadRequestId) {
          return;
        }

        const allLoadedLines = this.toLines(text);
        this.textPreviewLoadedBytes = previewBytes;
        this.textPreviewSourceBytes = blob.size;
        this.textPreviewLines = allLoadedLines.slice(0, TEXT_PREVIEW_MAX_LINES);
        this.textPreview = this.textPreviewLines.join('\n');
        this.textPreviewTruncated = blob.size > TEXT_PREVIEW_MAX_BYTES || allLoadedLines.length > TEXT_PREVIEW_MAX_LINES;
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
    if (this.isPdf()) return 'PDF secure iframe';
    if (this.isDocxPreview()) return 'DOCX text extraction';
    if (this.isImage()) return 'Image viewer';
    if (this.isTextPreview()) return 'Limited text/code viewer';
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

  private toLines(value: string): string[] {
    return value.replace(/\r\n/g, '\n').replace(/\r/g, '\n').split('\n');
  }

  private resetTextPreviewState(): void {
    this.textPreview = '';
    this.textPreviewLines = [];
    this.textPreviewLoadedBytes = 0;
    this.textPreviewSourceBytes = 0;
    this.textPreviewTruncated = false;
    this.textError = '';
  }
}
