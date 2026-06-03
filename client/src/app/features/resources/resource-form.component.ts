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
      </div>

      <div *ngIf="error" class="error">{{ error }}</div>

      <form class="form-grid" (ngSubmit)="save()">
        <label>
          Type
          <select name="resourceType" [(ngModel)]="form.resourceType">
            <option *ngFor="let type of resourceTypes" [value]="type">{{ type }}</option>
          </select>
        </label>

        <label>
          Title
          <input name="title" required [(ngModel)]="form.title" />
        </label>

        <label class="full">
          Description
          <textarea name="description" [(ngModel)]="form.description"></textarea>
        </label>

        <label *ngIf="form.resourceType === 'LINK'" class="full">
          URL
          <input name="url" type="url" [(ngModel)]="form.url" />
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
          File
          <input name="file" type="file" (change)="selectFile($event)" />
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
    this.saving = true;

    if (this.form.resourceType === 'FILE') {
      this.uploadFile();
      return;
    }

    const request: ResourceRequest = {
      ...this.form,
      vaultId: this.vaultId,
      folderId: this.folderId ?? null
    };

    this.resourceService.create(request).subscribe({
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
    data.append('vaultId', this.vaultId);
    if (this.folderId) {
      data.append('folderId', this.folderId);
    }
    data.append('title', this.form.title);
    data.append('description', this.form.description ?? '');
    data.append('file', this.selectedFile);

    this.resourceService.uploadFile(data).subscribe({
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
      vaultId: '',
      folderId: null,
      title: '',
      description: '',
      resourceType: 'LINK',
      url: '',
      content: '',
      codeLanguage: '',
      sourceName: ''
    };
  }
}
