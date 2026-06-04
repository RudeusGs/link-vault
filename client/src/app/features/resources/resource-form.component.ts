import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { LinkPreview, ResourceRequest, ResourceType } from '../../core/models/resource.model';
import { ResourceService } from '../../core/services/resource.service';
import { LinkPreviewCardComponent } from './link-preview-card.component';

const MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024;
const MAX_FILE_SIZE_LABEL = '20MB';
const ALLOWED_FILE_EXTENSIONS = [
  'jpg', 'jpeg', 'png', 'webp', 'gif', 'svg',
  'pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx',
  'txt', 'md', 'csv', 'json', 'xml', 'yaml', 'yml', 'log',
  'java', 'kt', 'py', 'ts', 'tsx', 'js', 'jsx', 'html', 'css', 'scss',
  'sql', 'sh', 'ps1', 'c', 'cpp', 'h', 'hpp', 'cs', 'go', 'rs', 'php',
  'rb', 'swift', 'dart', 'zip', 'rar', '7z', 'mp3', 'wav', 'mp4', 'webm', 'mov'
];

@Component({
  selector: 'app-resource-form',
  standalone: true,
  imports: [CommonModule, FormsModule, LinkPreviewCardComponent],
  template: `
    <div class="lv-modal-backdrop" *ngIf="visible" (click)="close()">
      <section class="lv-modal-card p-4" (click)="$event.stopPropagation()">
        <div class="d-flex justify-content-between align-items-start gap-3 mb-3">
          <div>
            <h2 class="lv-section-title mb-1">{{ titleFor(form.resourceType) }}</h2>
            <p class="lv-muted mb-0">Create inside {{ folderId ? 'selected folder' : 'vault root' }}. No manual IDs.</p>
          </div>
          <button class="lv-icon-button" type="button" (click)="close()">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>

        <div *ngIf="error" class="alert alert-danger">{{ error }}</div>

        <div class="row g-2 mb-3">
          <div class="col-6 col-md-3" *ngFor="let type of resourceTypes">
            <button class="btn w-100 border d-flex flex-column align-items-center gap-2 py-3" [class.btn-primary]="form.resourceType === type" [class.text-white]="form.resourceType === type" type="button" (click)="selectType(type)">
              <span class="material-symbols-outlined">{{ iconFor(type) }}</span>
              <span class="fw-semibold small">{{ type }}</span>
            </button>
          </div>
        </div>

        <form class="row g-3" (ngSubmit)="save()">
          <div class="col-md-7">
            <label class="form-label fw-semibold">Title</label>
            <input class="form-control" name="title" [required]="form.resourceType !== 'FILE'" [(ngModel)]="form.title" />
          </div>

          <div class="col-md-5" *ngIf="form.resourceType === 'LINK'">
            <label class="form-label fw-semibold">Source</label>
            <input class="form-control" name="sourceName" placeholder="Figma, GitHub..." [(ngModel)]="form.sourceName" />
          </div>

          <div class="col-12">
            <label class="form-label fw-semibold">Description</label>
            <textarea class="form-control" name="description" rows="2" [(ngModel)]="form.description"></textarea>
          </div>

          <div *ngIf="form.resourceType === 'LINK'" class="col-12">
            <label class="form-label fw-semibold">URL</label>
            <div class="input-group lv-preview-input-group">
              <input class="form-control" name="url" type="url" placeholder="https://..." required [(ngModel)]="form.url" />
              <button class="btn btn-outline-primary" type="button" (click)="fetchLinkPreview()" [disabled]="linkPreviewLoading || !form.url">
                <span *ngIf="linkPreviewLoading" class="spinner-border spinner-border-sm me-1"></span>
                <span *ngIf="!linkPreviewLoading" class="material-symbols-outlined me-1" style="font-size:16px">travel_explore</span>
                Fetch preview
              </button>
            </div>
            <div *ngIf="linkPreviewMessage" class="alert py-2 mt-2 mb-0" [class.alert-success]="linkPreview?.previewStatus === 'OK'" [class.alert-warning]="linkPreview?.previewStatus !== 'OK'">
              {{ linkPreviewMessage }}
            </div>
          </div>

          <div *ngIf="form.resourceType === 'LINK' && linkPreview" class="col-12">
            <app-link-preview-card [preview]="linkPreview" [showActions]="true"></app-link-preview-card>
          </div>

          <div *ngIf="form.resourceType === 'SNIPPET'" class="col-md-5">
            <label class="form-label fw-semibold">Code language</label>
            <input class="form-control" name="codeLanguage" placeholder="java, ts, js..." [(ngModel)]="form.codeLanguage" />
          </div>

          <div *ngIf="form.resourceType === 'NOTE' || form.resourceType === 'SNIPPET'" class="col-12">
            <label class="form-label fw-semibold">Content</label>
            <textarea class="form-control" name="content" rows="8" [(ngModel)]="form.content"></textarea>
          </div>

          <div *ngIf="form.resourceType === 'FILE'" class="col-12">
            <label class="form-label fw-semibold">Upload file from computer</label>
            <label
              class="lv-empty-state lv-upload-dropzone d-block cursor-pointer"
              [class.active]="dragActive"
              style="cursor:pointer"
              (dragover)="onDragOver($event)"
              (dragleave)="onDragLeave($event)"
              (drop)="onDrop($event)"
            >
              <span class="material-symbols-outlined d-block mb-2" style="font-size:34px">upload_file</span>
              <strong class="d-block text-break">{{ selectedFile?.name || 'Click to choose a file' }}</strong>
              <p class="lv-muted mb-0 small" *ngIf="selectedFile">{{ selectedFile.size | number }} bytes</p>
              <p class="lv-muted mb-0 small" *ngIf="!selectedFile">Drag a file here or browse. PDF/DOCX preview is supported. Max {{ maxFileSizeLabel }}.</p>
              <input class="d-none" name="file" type="file" [attr.accept]="acceptedFileTypes" (change)="selectFile($event)" />
            </label>
          </div>

          <div class="col-12 lv-form-actions">
            <button class="btn btn-outline-secondary" type="button" (click)="close()">Cancel</button>
            <button class="btn btn-primary" type="submit" [disabled]="saving">
              <span *ngIf="saving" class="spinner-border spinner-border-sm me-2"></span>
              Save resource
            </button>
          </div>
        </form>
      </section>
    </div>
  `
})
export class ResourceFormComponent {
  @Input({ required: true }) vaultId!: string;
  @Input() folderId?: string | null;
  @Output() saved = new EventEmitter<void>();

  protected readonly resourceTypes: ResourceType[] = ['LINK', 'NOTE', 'SNIPPET', 'FILE'];
  protected visible = false;
  protected saving = false;
  protected error = '';
  protected selectedFile?: File;
  protected dragActive = false;
  protected linkPreview?: LinkPreview;
  protected linkPreviewLoading = false;
  protected linkPreviewMessage = '';
  protected form: ResourceRequest = this.emptyForm();
  protected readonly acceptedFileTypes = ALLOWED_FILE_EXTENSIONS.map((extension) => `.${extension}`).join(',');
  protected readonly maxFileSizeLabel = MAX_FILE_SIZE_LABEL;

  private readonly resourceService = inject(ResourceService);

  open(type: ResourceType = 'LINK'): void {
    this.form = this.emptyForm(type);
    this.selectedFile = undefined;
    this.resetLinkPreview();
    this.error = '';
    this.visible = true;
  }

  close(): void {
    this.visible = false;
    this.saving = false;
    this.error = '';
    this.resetLinkPreview();
  }

  protected selectType(type: ResourceType): void {
    this.form = { ...this.emptyForm(type), title: this.form.title, description: this.form.description };
    this.selectedFile = undefined;
    this.resetLinkPreview();
  }

  protected selectFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    this.handleSelectedFile(file);
    if (!this.selectedFile) {
      input.value = '';
    }
  }

  protected onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragActive = true;
  }

  protected onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.dragActive = false;
  }

  protected onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragActive = false;
    this.handleSelectedFile(event.dataTransfer?.files?.[0]);
  }

  protected fetchLinkPreview(): void {
    const url = this.form.url?.trim();
    this.linkPreviewMessage = '';

    if (!url) {
      this.linkPreviewMessage = 'Enter a URL first.';
      return;
    }

    this.linkPreviewLoading = true;
    this.resourceService.fetchLinkPreview(url).subscribe({
      next: (preview) => {
        this.linkPreviewLoading = false;
        this.linkPreview = preview;
        this.applyPreviewSuggestion(preview);
        this.linkPreviewMessage = preview.previewStatus === 'OK'
          ? 'Preview loaded. Metadata will be saved with this link.'
          : `${preview.previewError || 'Preview metadata is unavailable.'} You can still save this link.`;
      },
      error: (error) => {
        this.linkPreviewLoading = false;
        this.linkPreview = undefined;
        this.linkPreviewMessage = error instanceof Error
          ? `${error.message}. You can still save this link.`
          : 'Preview could not be loaded. You can still save this link.';
      }
    });
  }

  private handleSelectedFile(file?: File): void {
    this.error = '';
    this.selectedFile = undefined;

    if (!file) {
      return;
    }

    if (file.size > MAX_FILE_SIZE_BYTES) {
      this.error = `File is larger than ${MAX_FILE_SIZE_LABEL}.`;
      return;
    }

    const extension = this.extensionOf(file.name);
    if (!extension || !ALLOWED_FILE_EXTENSIONS.includes(extension)) {
      this.error = extension ? `.${extension} files are not supported yet.` : 'File must have a supported extension.';
      return;
    }

    this.selectedFile = file;
    if (this.selectedFile && !this.form.title) {
      this.form.title = this.selectedFile.name;
    }
  }

  protected save(): void {
    this.error = '';
    this.saving = true;

    if (this.form.resourceType === 'FILE') {
      this.uploadFile();
      return;
    }

    const request: ResourceRequest = {
      ...this.form,
      url: this.form.url?.trim(),
      content: this.form.content?.trim()
    };

    const action = this.folderId
      ? this.resourceService.createInFolder(this.folderId, request)
      : this.resourceService.createInVault(this.vaultId, request);

    action.subscribe({
      next: () => this.afterSaved(),
      error: (error) => this.afterError(error)
    });
  }

  protected titleFor(type: ResourceType): string {
    switch (type) {
      case 'LINK': return 'New Link';
      case 'NOTE': return 'New Note';
      case 'SNIPPET': return 'New Snippet';
      case 'FILE': return 'Upload File';
    }
  }

  protected iconFor(type: ResourceType): string {
    switch (type) {
      case 'LINK': return 'link';
      case 'NOTE': return 'notes';
      case 'SNIPPET': return 'code';
      case 'FILE': return 'upload_file';
    }
  }

  private uploadFile(): void {
    if (!this.selectedFile) {
      this.afterError(new Error('Choose a file first'));
      return;
    }

    if (this.selectedFile.size > MAX_FILE_SIZE_BYTES) {
      this.afterError(new Error(`File is larger than ${MAX_FILE_SIZE_LABEL}.`));
      return;
    }

    const data = new FormData();
    data.append('title', this.form.title || this.selectedFile.name);
    data.append('description', this.form.description ?? '');
    data.append('file', this.selectedFile);

    const action = this.folderId
      ? this.resourceService.uploadToFolder(this.folderId, data)
      : this.resourceService.uploadToVault(this.vaultId, data);

    action.subscribe({
      next: () => this.afterSaved(),
      error: (error) => this.afterError(error)
    });
  }

  private afterSaved(): void {
    this.saving = false;
    this.visible = false;
    this.form = this.emptyForm();
    this.selectedFile = undefined;
    this.resetLinkPreview();
    this.saved.emit();
  }

  private afterError(error: unknown): void {
    this.saving = false;
    this.error = error instanceof Error ? error.message : 'Could not save resource';
  }

  private emptyForm(type: ResourceType = 'LINK'): ResourceRequest {
    return {
      title: '',
      description: '',
      resourceType: type,
      url: '',
      content: '',
      codeLanguage: '',
      sourceName: '',
      thumbnailUrl: ''
    };
  }

  private extensionOf(fileName: string): string {
    const dotIndex = fileName.lastIndexOf('.');
    if (dotIndex < 0 || dotIndex === fileName.length - 1) {
      return '';
    }

    return fileName.slice(dotIndex + 1).toLowerCase();
  }

  private applyPreviewSuggestion(preview: LinkPreview): void {
    const source = preview.sourceName || preview.siteName || preview.domain || '';
    if (!this.form.sourceName && source) {
      this.form.sourceName = this.truncate(source, 255);
    }
    if (!this.form.thumbnailUrl && preview.thumbnailUrl) {
      this.form.thumbnailUrl = this.truncate(preview.thumbnailUrl, 2000);
    }
    if (!this.form.title && preview.previewTitle) {
      this.form.title = this.truncate(preview.previewTitle, 255);
    }
    if (!this.form.description && preview.previewDescription) {
      this.form.description = this.truncate(preview.previewDescription, 1000);
    }
  }

  private resetLinkPreview(): void {
    this.linkPreview = undefined;
    this.linkPreviewLoading = false;
    this.linkPreviewMessage = '';
  }

  private truncate(value: string, maxLength: number): string {
    return value.length > maxLength ? value.slice(0, maxLength) : value;
  }
}
