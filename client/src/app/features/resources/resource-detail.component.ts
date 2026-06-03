import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ActivatedRoute, Router } from '@angular/router';

import { Resource, ResourcePreview, ResourceRequest } from '../../core/models/resource.model';
import { Tag } from '../../core/models/tag.model';
import { ResourceService } from '../../core/services/resource.service';
import { TagService } from '../../core/services/tag.service';

@Component({
  selector: 'app-resource-detail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section *ngIf="resource; else loadingTpl">
      <div class="d-flex flex-column flex-md-row align-items-md-center justify-content-between gap-3 mb-4">
        <div>
          <button class="btn btn-link px-0 lv-primary fw-semibold" type="button" (click)="goBack()">
            <span class="material-symbols-outlined me-1" style="font-size:18px">arrow_back</span>
            Back
          </button>
          <div class="d-flex align-items-center gap-3 flex-wrap">
            <h1 class="lv-page-title">{{ resource.title }}</h1>
            <span class="badge rounded-pill lv-badge-soft px-3 py-2">{{ resource.resourceType }}</span>
            <span *ngIf="resource.isFavorite" class="badge rounded-pill text-bg-warning px-3 py-2">Favorite</span>
            <span *ngIf="resource.isArchived" class="badge rounded-pill text-bg-secondary px-3 py-2">Archived</span>
          </div>
          <p class="lv-muted mt-2 mb-0">{{ resource.description || 'No description' }}</p>
        </div>

        <div class="d-flex flex-wrap gap-2">
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
              <div class="d-flex justify-content-between gap-3"><span class="lv-muted">Vault</span><strong class="text-end">{{ resource.vaultName }}</strong></div>
              <div class="d-flex justify-content-between gap-3" *ngIf="resource.folderName"><span class="lv-muted">Folder</span><strong class="text-end">{{ resource.folderName }}</strong></div>
              <div class="d-flex justify-content-between gap-3"><span class="lv-muted">Type</span><strong>{{ resource.resourceType }}</strong></div>
              <div class="d-flex justify-content-between gap-3" *ngIf="resource.fileName"><span class="lv-muted">File</span><strong class="text-end text-truncate">{{ resource.fileName }}</strong></div>
              <div class="d-flex justify-content-between gap-3" *ngIf="resource.mimeType"><span class="lv-muted">MIME</span><strong class="text-end">{{ resource.mimeType }}</strong></div>
              <div class="d-flex justify-content-between gap-3" *ngIf="resource.fileSize"><span class="lv-muted">Size</span><strong>{{ resource.fileSize | number }} bytes</strong></div>
              <div class="d-flex justify-content-between gap-3"><span class="lv-muted">Updated</span><strong>{{ resource.updatedAt | date:'mediumDate' }}</strong></div>
            </div>
          </article>

          <article class="lv-card p-4">
            <div class="d-flex justify-content-between align-items-center mb-3">
              <h2 class="lv-section-title mb-0">Tags</h2>
            </div>
            <div class="d-flex flex-wrap gap-2 mb-3">
              <span *ngFor="let tag of resource.tags" class="badge rounded-pill text-bg-light border px-3 py-2">
                {{ tag.name }}
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
            <div class="d-flex justify-content-between align-items-center gap-3 mb-3">
              <div>
                <h2 class="lv-section-title mb-1">Resource preview</h2>
                <p class="lv-muted mb-0">Preview the saved content or open it externally.</p>
              </div>
              <a *ngIf="openUrl" class="btn btn-primary" [href]="openUrl" target="_blank">
                Open
                <span class="material-symbols-outlined ms-1" style="font-size:16px">open_in_new</span>
              </a>
            </div>

            <ng-container [ngSwitch]="resource.resourceType">
              <div *ngSwitchCase="'LINK'" class="lv-soft-panel p-4">
                <span class="material-symbols-outlined lv-primary mb-3" style="font-size:36px">link</span>
                <h3 class="lv-section-title">{{ resource.title }}</h3>
                <a class="lv-primary text-break" [href]="resource.url || '#'" target="_blank">{{ resource.url }}</a>
              </div>

              <div *ngSwitchCase="'NOTE'" class="lv-soft-panel p-4" style="white-space:pre-wrap;line-height:1.7">{{ resource.content || 'No content' }}</div>

              <pre *ngSwitchCase="'SNIPPET'" class="lv-code-preview"><code>{{ resource.content || '// No code' }}</code></pre>

              <div *ngSwitchCase="'FILE'" class="lv-soft-panel p-4">
                <img *ngIf="isImage()" class="img-fluid rounded-3" [src]="preview?.previewUrl || ''" [alt]="resource.title" />
                <iframe *ngIf="isPdf() && safePreviewUrl" class="w-100 rounded-3 border" style="min-height:70vh" [src]="safePreviewUrl" title="PDF preview"></iframe>
                <pre *ngIf="isTextPreview()" class="lv-code-preview"><code>{{ textPreview }}</code></pre>
                <p *ngIf="preview && !preview.supported" class="lv-muted mb-0">{{ preview.reason || 'Preview unavailable. Use Open instead.' }}</p>
                <p *ngIf="textError" class="lv-muted mb-0">{{ textError }}</p>
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
          <div class="col-12 d-flex justify-content-end gap-2"><button class="btn btn-outline-secondary" type="button" (click)="editOpen = false">Cancel</button><button class="btn btn-primary" type="submit">Save changes</button></div>
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
export class ResourceDetailComponent implements OnInit {
  protected resource?: Resource;
  protected preview?: ResourcePreview;
  protected tags: Tag[] = [];
  protected selectedTagId = '';
  protected safePreviewUrl?: SafeResourceUrl;
  protected textPreview = '';
  protected textError = '';
  protected error = '';
  protected editOpen = false;
  protected editForm: ResourceRequest = this.emptyEditForm();

  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly resourceService = inject(ResourceService);
  private readonly tagService = inject(TagService);
  private readonly http = inject(HttpClient);
  private readonly sanitizer = inject(DomSanitizer);
  private loadRequestId = 0;

  get openUrl(): string | undefined {
    return this.resource?.resourceType === 'FILE' ? this.resource.fileUrl ?? undefined : this.resource?.url ?? undefined;
  }

  ngOnInit(): void {
    this.loadTags();
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.loadResource()
    });
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

  protected isImage(): boolean { return this.preview?.mimeType?.startsWith('image/') ?? false; }
  protected isPdf(): boolean { return this.preview?.mimeType === 'application/pdf'; }

  protected isTextPreview(): boolean {
    if (!this.preview?.supported || !this.preview.previewUrl) return false;
    const mime = this.preview.mimeType ?? '';
    const name = this.preview.fileName ?? '';
    return mime.startsWith('text/') || mime === 'application/json' || ['.txt', '.md', '.json', '.java', '.ts', '.js', '.html', '.css'].some((ext) => name.toLowerCase().endsWith(ext));
  }

  private loadResource(): void {
    const resourceId = this.route.snapshot.paramMap.get('resourceId');
    if (!resourceId) return;

    const requestId = ++this.loadRequestId;
    this.resource = undefined;
    this.preview = undefined;
    this.safePreviewUrl = undefined;
    this.textPreview = '';
    this.textError = '';
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
        this.safePreviewUrl = preview.previewUrl ? this.sanitizer.bypassSecurityTrustResourceUrl(preview.previewUrl) : undefined;
        this.loadTextPreview(requestId);
      },
      error: () => undefined
    });
  }

  private loadTextPreview(requestId: number): void {
    this.textPreview = '';
    this.textError = '';
    if (!this.isTextPreview() || !this.preview?.previewUrl) return;
    this.http.get(this.preview.previewUrl, { responseType: 'text' }).subscribe({
      next: (text) => {
        if (requestId === this.loadRequestId) {
          this.textPreview = text;
        }
      },
      error: () => {
        if (requestId === this.loadRequestId) {
          this.textError = 'Text preview could not be loaded. Use Open instead.';
        }
      }
    });
  }

  private emptyEditForm(): ResourceRequest {
    return { title: '', description: '', resourceType: 'LINK', url: '', content: '', codeLanguage: '', sourceName: '', thumbnailUrl: '' };
  }
}
