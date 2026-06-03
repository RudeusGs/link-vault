import { CommonModule } from '@angular/common';
import { Component, OnInit, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Folder, FolderRequest } from '../../core/models/folder.model';
import { FolderService } from '../../core/services/folder.service';
import { ResourceFormComponent } from '../resources/resource-form.component';
import { ResourceListComponent } from '../resources/resource-list.component';

@Component({
  selector: 'app-folder-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ResourceFormComponent, ResourceListComponent],
  template: `
    <section class="page" *ngIf="folder">
      <header class="page-header">
        <div>
          <p class="eyebrow">Folder</p>
          <h1>{{ folder.name }}</h1>
          <p class="muted">{{ folder.description }}</p>
        </div>
        <a class="btn" [routerLink]="['/vaults', folder.vaultId]">Back to vault</a>
      </header>

      <div *ngIf="error" class="error">{{ error }}</div>

      <section class="grid cols-2">
        <article class="panel stack">
          <h2>Create child folder</h2>
          <form class="form-grid" (ngSubmit)="createFolder()">
            <label>
              Name
              <input name="folderName" required [(ngModel)]="folderForm.name" />
            </label>
            <label>
              Sort order
              <input name="sortOrder" type="number" [(ngModel)]="folderForm.sortOrder" />
            </label>
            <label class="full">
              Description
              <textarea name="folderDescription" [(ngModel)]="folderForm.description"></textarea>
            </label>
            <button class="btn primary full" type="submit">Create child folder</button>
          </form>
        </article>

        <article class="panel stack">
          <h2>Child folders</h2>
          <p *ngIf="children.length === 0" class="muted">No child folders.</p>
          <div *ngFor="let child of children" class="folder-row">
            <div>
              <a [routerLink]="['/folders', child.id]"><strong>{{ child.name }}</strong></a>
              <p class="muted">{{ child.description || 'No description' }}</p>
            </div>
            <button class="btn danger" type="button" (click)="deleteFolder(child)">Delete</button>
          </div>
        </article>
      </section>

      <app-resource-form
        [vaultId]="folder.vaultId"
        [folderId]="folder.id"
        (saved)="resourceList.load()"
      />
      <app-resource-list #resourceList title="Folder resources" [vaultId]="folder.vaultId" [folderId]="folder.id" />
    </section>
  `,
  styles: [
    `
      .folder-row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        padding: 12px;
        border-radius: 8px;
        background: #f5faf8;
      }
    `
  ]
})
export class FolderDetailComponent implements OnInit {
  @ViewChild('resourceList') resourceList!: ResourceListComponent;

  protected folder?: Folder;
  protected children: Folder[] = [];
  protected folderForm: FolderRequest = this.emptyFolderForm();
  protected error = '';

  private readonly route = inject(ActivatedRoute);
  private readonly folderService = inject(FolderService);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    const folderId = this.route.snapshot.paramMap.get('folderId');
    if (!folderId) {
      return;
    }

    this.folderService.get(folderId).subscribe({
      next: (folder) => {
        this.folder = folder;
        this.folderForm = this.emptyFolderForm(folder);
      },
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not load folder')
    });

    this.folderService.listChildren(folderId).subscribe({
      next: (children) => (this.children = children),
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not load child folders')
    });
  }

  protected createFolder(): void {
    this.folderService.create(this.folderForm).subscribe({
      next: () => {
        this.load();
      },
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not create folder')
    });
  }

  protected deleteFolder(folder: Folder): void {
    if (!confirm(`Delete folder "${folder.name}" and its resources?`)) {
      return;
    }

    this.folderService.delete(folder.id).subscribe({
      next: () => this.load(),
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not delete folder')
    });
  }

  private emptyFolderForm(folder?: Folder): FolderRequest {
    return {
      vaultId: folder?.vaultId ?? '',
      parentId: folder?.id ?? null,
      name: '',
      description: '',
      icon: 'folder',
      sortOrder: 0
    };
  }
}
