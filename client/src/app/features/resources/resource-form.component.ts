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
    <section class="panel stack">
      <div>
        <p class="eyebrow">Create resource</p>
        <h2>New {{ form.resourceType.toLowerCase() }}</h2>
        <p class="muted">Resource will be saved into the current {{ folderId ? 'folder' : 'vault' }}.</p>
      </div>

      <div *ngIf="error" class="error">{{ error }}</div>

      <form class="form-grid" (ngSubmit)="save()">
        <label>
          Type
          <select name="resourceType" [(ngModel)]="form.resourceType" (change)="selectedFile = undefined">
            <option *ngFor="let type of resourceTypes" [value]="type">{{ type }}</option>
          </select>
        </label>

        <label>
          Title
          <input name="title" [required]="form.resourceType !== 'FILE'" [(ngModel)]="form.title" />
        </label>

        <label class="full">
          Description
          <textarea name="description" [(ngModel)]="form.description"></textarea>
        </label>

        <label *ngIf="form.resourceType === 'LINK'" class="full">
          URL
          <input name="url" type="url" required [(ngModel)]="form.url" />
        </label>

        <label *ngIf="form.resourceType === 'LINK'">
          Source
          <input name="sourceName" [(ngModel)]="form.sourceName" />
        </label>

        <label *ngIf="form.resourceType === 'SNIPPET'">
          Code language
          <input name="codeLanguage" placeholder="java, ts, js..." [(ngModel)]="form.codeLanguage" />
        </label>

        <label *ngIf="form.resourceType === 'NOTE' || form.resourceType === 'SNIPPET'" class="full">
          Content
          <textarea name="content" [(ngModel)]="form.content"></textarea>
        </label>

        <label *ngIf="form.resourceType === 'FILE'" class="full">
          File from computer
          <input name="file" type="file" required (change)="selectFile($event)" />
        </label>

        <div class="row full">
          <button class="btn primary" type="submit" [disabled]="saving">
            {{ saving ? 'Saving...' : 'Create resource' }}
          </button>
          <button class="btn" type="button" (click)="reset()">Reset</button>
        </div>
      </form>
    </section>
  `
})
export class ResourceFormComponent {
  @Input({ required: true }) vaultId!: string;
  @Input() folderId?: string | null;
  @Output() saved = new EventEmitter<void>();

  protected readonly resourceTypes: ResourceType[] = ['LINK', 'NOTE', 'SNIPPET', 'FILE'];
  protected saving = false;
  protected error = '';
  protected selectedFile?: File;
  protected form: ResourceRequest = this.emptyForm();

  private readonly resourceService = inject(ResourceService);

  protected selectFile(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0];
  }

  protected save(): void {
    this.error = '';

    if (!this.vaultId) {
      this.error = 'Vault context is missing';
      return;
    }

    this.saving = true;

    if (this.form.resourceType === 'FILE') {
      this.uploadFile();
      return;
    }

    const request: ResourceRequest = {
      title: this.form.title.trim(),
      description: this.form.description?.trim() || undefined,
      resourceType: this.form.resourceType,
      url: this.form.url?.trim() || undefined,
      content: this.form.content,
      codeLanguage: this.form.codeLanguage?.trim() || undefined,
      sourceName: this.form.sourceName?.trim() || undefined,
      thumbnailUrl: this.form.thumbnailUrl?.trim() || undefined
    };

    const save$ = this.folderId
      ? this.resourceService.createInFolder(this.folderId, request)
      : this.resourceService.createInVault(this.vaultId, request);

    save$.subscribe({
      next: () => this.afterSaved(),
      error: (error) => this.afterError(error)
    });
  }

  protected reset(): void {
    this.form = this.emptyForm();
    this.selectedFile = undefined;
    this.error = '';
  }

  private uploadFile(): void {
    if (!this.selectedFile) {
      this.afterError(new Error('Choose a file first'));
      return;
    }

    const data = new FormData();
    data.append('title', this.form.title.trim());
    data.append('description', this.form.description?.trim() ?? '');
    data.append('file', this.selectedFile);

    const upload$ = this.folderId
      ? this.resourceService.uploadToFolder(this.folderId, data)
      : this.resourceService.uploadToVault(this.vaultId, data);

    upload$.subscribe({
      next: () => this.afterSaved(),
      error: (error) => this.afterError(error)
    });
  }

  private afterSaved(): void {
    this.saving = false;
    this.reset();
    this.saved.emit();
  }

  private afterError(error: unknown): void {
    this.saving = false;
    this.error = error instanceof Error ? error.message : 'Could not save resource';
  }

  private emptyForm(): ResourceRequest {
    return {
      title: '',
      description: '',
      resourceType: 'LINK',
      url: '',
      content: '',
      codeLanguage: '',
      sourceName: '',
      thumbnailUrl: ''
    };
  }
}
