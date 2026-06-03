import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ResourceRequest, ResourceType } from '../../core/models/resource.model';
import { ResourceService } from '../../core/services/resource.service';

@Component({
  selector: 'app-resource-form',
  standalone: true,
  imports: [CommonModule, FormsModule],
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
            <input class="form-control" name="url" type="url" placeholder="https://..." required [(ngModel)]="form.url" />
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
            <label class="lv-empty-state d-block cursor-pointer" style="cursor:pointer">
              <span class="material-symbols-outlined d-block mb-2" style="font-size:34px">upload_file</span>
              <strong>{{ selectedFile?.name || 'Click to choose a file' }}</strong>
              <p class="lv-muted mb-0 small" *ngIf="selectedFile">{{ selectedFile.size | number }} bytes</p>
              <input class="d-none" name="file" type="file" (change)="selectFile($event)" />
            </label>
          </div>

          <div class="col-12 d-flex gap-2 justify-content-end">
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
  protected form: ResourceRequest = this.emptyForm();

  private readonly resourceService = inject(ResourceService);

  open(type: ResourceType = 'LINK'): void {
    this.form = this.emptyForm(type);
    this.selectedFile = undefined;
    this.error = '';
    this.visible = true;
  }

  close(): void {
    this.visible = false;
    this.saving = false;
    this.error = '';
  }

  protected selectType(type: ResourceType): void {
    this.form = { ...this.emptyForm(type), title: this.form.title, description: this.form.description };
    this.selectedFile = undefined;
  }

  protected selectFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0];
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
}
