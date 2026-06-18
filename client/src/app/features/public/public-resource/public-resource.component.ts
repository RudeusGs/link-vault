import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { PublicApiService } from '../../../core/http/public-api.service';
import { Resource } from '../../resources/models/resource.model';
import { LinkPreviewCardComponent } from '../../resources/link-preview-card.component';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-public-resource',
  standalone: true,
  imports: [CommonModule, LinkPreviewCardComponent],
  template: `
    <div class="container py-5 max-w-4xl">
      <div *ngIf="loading" class="text-center py-5">
        <span class="spinner-border text-primary"></span>
        <p class="mt-3 text-muted">Loading resource...</p>
      </div>

      <div *ngIf="error" class="alert alert-danger shadow-sm">
        <h4 class="alert-heading">Access Denied</h4>
        <p class="mb-0">{{ error }}</p>
      </div>

      <ng-container *ngIf="!loading && resource">
        <div class="lv-page-header mb-4">
          <div class="d-flex align-items-center gap-3 flex-wrap mb-2">
            <span class="lv-icon-box bg-light">
              <span class="material-symbols-outlined text-primary" style="font-size:24px">{{ getIcon() }}</span>
            </span>
            <h1 class="lv-page-title m-0">{{ resource.title }}</h1>
            <span class="badge rounded-pill text-bg-light border px-3 py-2">{{ resource.resourceType }}</span>
            <span class="badge rounded-pill lv-badge-soft px-3 py-2">
              <span class="material-symbols-outlined align-middle me-1" style="font-size:14px">public</span>
              Public {{ resource.publicAccess === 'EDIT' ? 'Editor' : 'Viewer' }}
            </span>
          </div>
          <p class="lv-muted lead mb-0">{{ resource.description || 'No description provided.' }}</p>
        </div>

        <div class="lv-card p-4">
          <ng-container [ngSwitch]="resource.resourceType">
            
            <div *ngSwitchCase="'LINK'">
              <app-link-preview-card [resource]="resource" [showActions]="false"></app-link-preview-card>
              <div class="mt-4 text-center">
                <a [href]="resource.url" target="_blank" rel="noopener" class="btn btn-primary btn-lg px-5 shadow-sm rounded-pill d-inline-flex align-items-center gap-2">
                  Visit Link
                  <span class="material-symbols-outlined" style="font-size:20px">open_in_new</span>
                </a>
              </div>
            </div>

            <div *ngSwitchCase="'NOTE'" class="bg-light p-4 rounded-3 border" style="white-space:pre-wrap;line-height:1.7; font-size: 1.1rem;">
              {{ resource.content || 'No content' }}
            </div>

            <div *ngSwitchCase="'SNIPPET'">
              <div class="d-flex justify-content-between align-items-center bg-dark text-white px-3 py-2 rounded-top">
                <span class="font-monospace small">{{ resource.codeLanguage || 'Code' }}</span>
              </div>
              <pre class="bg-light p-4 rounded-bottom border border-top-0 m-0 overflow-auto"><code>{{ resource.content || '// No code' }}</code></pre>
            </div>

            <div *ngSwitchCase="'FILE'" class="text-center py-5">
              <span class="material-symbols-outlined text-muted mb-3" style="font-size:64px">description</span>
              <h3 class="h5">{{ resource.fileName || resource.title }}</h3>
              
              <div *ngIf="resource.fileUrl" class="mt-4">
                <a [href]="resource.fileUrl" target="_blank" class="btn btn-primary d-inline-flex align-items-center gap-2">
                  <span class="material-symbols-outlined">download</span>
                  Download File
                </a>
              </div>
              <div *ngIf="!resource.fileUrl" class="alert alert-warning d-inline-block mt-3 text-start">
                <span class="material-symbols-outlined align-middle me-2">warning</span>
                Direct download not available for this public file yet.
              </div>
            </div>

          </ng-container>
        </div>
      </ng-container>
    </div>
  `
})
export class PublicResourceComponent implements OnInit {
  resource?: Resource;
  loading = true;
  error = '';

  private readonly route = inject(ActivatedRoute);
  private readonly publicApi = inject(PublicApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly sanitizer = inject(DomSanitizer);

  ngOnInit() {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      const id = params.get('id');
      if (id) {
        this.loadResource(id);
      }
    });
  }

  loadResource(id: string) {
    this.loading = true;
    this.error = '';
    
    this.publicApi.getResource(id).subscribe({
      next: (res) => {
        this.resource = res.data;
        this.loading = false;
      },
      error: (err) => {
        this.error = err.error?.message || 'Failed to load resource.';
        this.loading = false;
      }
    });
  }

  getIcon(): string {
    if (!this.resource) return 'draft';
    if (this.resource.resourceType === 'LINK') return 'link';
    if (this.resource.resourceType === 'FILE') return 'description';
    if (this.resource.resourceType === 'NOTE') return 'notes';
    return 'code';
  }
}
