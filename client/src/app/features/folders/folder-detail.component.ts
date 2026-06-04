import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, ViewChild, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { Folder, FolderRequest } from '../../core/models/folder.model';
import { ResourceType } from '../../core/models/resource.model';
import { FolderService } from '../../core/services/folder.service';
import { ResourceFormComponent } from '../resources/resource-form.component';
import { ResourceListComponent } from '../resources/resource-list.component';

@Component({
  selector: 'app-folder-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ResourceFormComponent, ResourceListComponent],
  template: `
    <section *ngIf="!loading && folder; else loadingTpl">
      <div class="lv-page-header mb-4">
        <div class="lv-page-header-copy">
          <a [routerLink]="['/vaults', folder.vaultId]" class="d-inline-flex align-items-center gap-2 lv-primary fw-semibold mb-2">
            <span class="material-symbols-outlined" style="font-size:18px">arrow_back</span>
            Back to vault
          </a>
          <div class="d-flex align-items-center gap-3 flex-wrap">
            <h1 class="lv-page-title lv-break-title">{{ folder.name }}</h1>
            <span class="badge rounded-pill lv-badge-secondary px-3 py-2">Folder</span>
          </div>
          <p class="lv-muted mb-0 mt-1 lv-line-clamp-3 text-break">{{ folder.description || 'No description' }}</p>
        </div>
        <div class="lv-action-toolbar">
          <button class="btn btn-outline-primary" type="button" (click)="openEditFolder()"><span class="material-symbols-outlined me-1" style="font-size:18px">edit</span>Edit</button>
          <button class="btn btn-outline-danger" type="button" (click)="deleteFolder()"><span class="material-symbols-outlined me-1" style="font-size:18px">delete</span>Delete</button>
        </div>
      </div>

      <div *ngIf="error" class="alert alert-danger">{{ error }}</div>

      <div class="row g-4 mb-4">
        <div class="col-xl-4">
          <article class="lv-card p-4 h-100">
            <div class="d-flex align-items-center justify-content-between mb-3">
              <h2 class="lv-section-title mb-0">Child folders</h2>
              <button class="lv-icon-button" type="button" (click)="openCreateChild()"><span class="material-symbols-outlined">create_new_folder</span></button>
            </div>

            <div *ngIf="children.length === 0" class="lv-empty-state py-4">No child folders.</div>

            <div class="d-grid gap-2">
              <div *ngFor="let child of children" class="lv-soft-panel p-3 d-flex align-items-center gap-3 text-dark min-w-0">
                <a class="d-flex align-items-center gap-3 min-w-0 flex-grow-1 text-dark" [routerLink]="['/folders', child.id]">
                  <span class="lv-icon-box"><span class="material-symbols-outlined">folder</span></span>
                  <span class="min-w-0 flex-grow-1">
                    <strong class="d-block text-truncate">{{ child.name }}</strong>
                    <small class="lv-muted d-block text-truncate">{{ child.description || 'No description' }}</small>
                  </span>
                </a>
                <button class="btn btn-sm btn-outline-danger flex-shrink-0" type="button" (click)="deleteChild(child, $event)"><span class="material-symbols-outlined" style="font-size:16px">delete</span></button>
              </div>
            </div>
          </article>
        </div>

        <div class="col-xl-8">
          <article class="lv-card p-4">
            <div class="lv-section-toolbar mb-4">
              <div class="min-w-0">
                <h2 class="lv-section-title mb-1">Folder resources</h2>
                <p class="lv-muted mb-0">Create and manage content in this folder.</p>
              </div>
              <div class="lv-action-toolbar compact">
                <button class="btn btn-outline-primary" type="button" (click)="openResource('LINK')">New Link</button>
                <button class="btn btn-outline-primary" type="button" (click)="openResource('NOTE')">New Note</button>
                <button class="btn btn-outline-primary" type="button" (click)="openResource('SNIPPET')">New Snippet</button>
                <button class="btn btn-primary" type="button" (click)="openResource('FILE')"><span class="material-symbols-outlined me-1" style="font-size:18px">upload</span>Upload</button>
              </div>
            </div>
            <app-resource-list #resourceList title="Folder resources" [vaultId]="folder.vaultId" [folderId]="folder.id" />
          </article>
        </div>
      </div>

      <app-resource-form #resourceForm [vaultId]="folder.vaultId" [folderId]="folder.id" (saved)="resourceList.load()" />
    </section>

    <div class="lv-modal-backdrop" *ngIf="folderModalOpen" (click)="closeFolderModal()">
      <section class="lv-modal-card p-4" (click)="$event.stopPropagation()">
        <div class="d-flex justify-content-between align-items-start gap-3 mb-3">
          <h2 class="lv-section-title">{{ editingFolder ? 'Edit Folder' : 'Create Child Folder' }}</h2>
          <button class="lv-icon-button" type="button" (click)="closeFolderModal()"><span class="material-symbols-outlined">close</span></button>
        </div>
        <form class="row g-3" (ngSubmit)="saveFolderModal()">
          <div class="col-md-8"><label class="form-label fw-semibold">Name</label><input class="form-control" name="folderName" required [(ngModel)]="folderForm.name" /></div>
          <div class="col-md-4"><label class="form-label fw-semibold">Sort order</label><input class="form-control" name="sortOrder" type="number" [(ngModel)]="folderForm.sortOrder" /></div>
          <div class="col-12"><label class="form-label fw-semibold">Description</label><textarea class="form-control" name="folderDescription" rows="3" [(ngModel)]="folderForm.description"></textarea></div>
          <div class="col-12 lv-form-actions"><button class="btn btn-outline-secondary" type="button" (click)="closeFolderModal()">Cancel</button><button class="btn btn-primary" type="submit" [disabled]="savingFolder">Save folder</button></div>
        </form>
      </section>
    </div>

    <ng-template #loadingTpl>
      <div *ngIf="loading" class="lv-card p-4"><span class="spinner-border spinner-border-sm me-2"></span>Loading folder...</div>
      <div *ngIf="!loading && error" class="alert alert-danger">{{ error }}</div>
    </ng-template>
  `
})
export class FolderDetailComponent implements OnInit {
  @ViewChild('resourceList') resourceList!: ResourceListComponent;
  @ViewChild('resourceForm') resourceForm!: ResourceFormComponent;

  protected folder?: Folder;
  protected loading = true;
  protected children: Folder[] = [];
  protected error = '';
  protected folderModalOpen = false;
  protected editingFolder = false;
  protected savingFolder = false;
  protected folderForm: FolderRequest = this.emptyFolderForm();

  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly folderService = inject(FolderService);
  private loadRequestId = 0;

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.load()
    });
  }

  protected load(): void {
    const folderId = this.route.snapshot.paramMap.get('folderId');
    if (!folderId) return;

    const requestId = ++this.loadRequestId;
    this.loading = true;
    this.error = '';

    this.folderService.get(folderId).subscribe({
      next: (folder) => {
        if (requestId === this.loadRequestId) {
          this.folder = folder;
          this.loading = false;
        }
      },
      error: (error) => {
        if (requestId === this.loadRequestId) {
          this.error = error instanceof Error ? error.message : 'Could not load folder';
          this.loading = false;
        }
      }
    });
    this.folderService.listChildren(folderId).subscribe({
      next: (children) => {
        if (requestId === this.loadRequestId) {
          this.children = children;
        }
      },
      error: (error) => {
        if (requestId === this.loadRequestId) {
          this.error = error instanceof Error ? error.message : 'Could not load child folders';
        }
      }
    });
  }

  protected openResource(type: ResourceType): void { this.resourceForm.open(type); }

  protected openCreateChild(): void {
    this.editingFolder = false;
    this.folderForm = this.emptyFolderForm();
    this.folderModalOpen = true;
  }

  protected openEditFolder(): void {
    if (!this.folder) return;
    this.editingFolder = true;
    this.folderForm = { name: this.folder.name, description: this.folder.description ?? '', icon: this.folder.icon ?? 'folder', sortOrder: this.folder.sortOrder };
    this.folderModalOpen = true;
  }

  protected closeFolderModal(): void {
    this.folderModalOpen = false;
    this.editingFolder = false;
    this.folderForm = this.emptyFolderForm();
  }

  protected saveFolderModal(): void {
    if (!this.folder) return;
    this.savingFolder = true;
    const action = this.editingFolder ? this.folderService.update(this.folder.id, this.folderForm) : this.folderService.createChild(this.folder.id, this.folderForm);
    action.subscribe({
      next: () => {
        this.savingFolder = false;
        this.closeFolderModal();
        this.load();
      },
      error: (error) => {
        this.savingFolder = false;
        this.error = error instanceof Error ? error.message : 'Could not save folder';
      }
    });
  }

  protected deleteChild(folder: Folder, event: Event): void {
    event.preventDefault();
    event.stopPropagation();
    if (!confirm(`Delete folder "${folder.name}"?`)) return;
    this.folderService.delete(folder.id).subscribe({ next: () => this.load(), error: (error) => (this.error = error instanceof Error ? error.message : 'Could not delete child folder') });
  }

  protected deleteFolder(): void {
    if (!this.folder || !confirm(`Delete folder "${this.folder.name}"?`)) return;
    const vaultId = this.folder.vaultId;
    this.folderService.delete(this.folder.id).subscribe({ next: () => this.router.navigate(['/vaults', vaultId]), error: (error) => (this.error = error instanceof Error ? error.message : 'Could not delete folder') });
  }

  private emptyFolderForm(): FolderRequest { return { name: '', description: '', icon: 'folder', sortOrder: 0 }; }
}
