import { CommonModule } from '@angular/common';
import { Component, OnInit, ViewChild, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';

import { Folder, FolderRequest } from '../../core/models/folder.model';
import { Vault } from '../../core/models/vault.model';
import { FolderService } from '../../core/services/folder.service';
import { VaultService } from '../../core/services/vault.service';
import { ResourceFormComponent } from '../resources/resource-form.component';
import { ResourceListComponent } from '../resources/resource-list.component';

@Component({
  selector: 'app-vault-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ResourceFormComponent, ResourceListComponent],
  template: `
    <section class="page" *ngIf="vaultId">
      <header class="page-header">
        <div>
          <p class="eyebrow">Vault</p>
          <h1>{{ vault?.name || 'Vault' }}</h1>
          <p class="muted">{{ vault?.description }}</p>
        </div>
        <a class="btn" routerLink="/vaults">Back to vaults</a>
      </header>

      <div *ngIf="error" class="error">{{ error }}</div>

      <section class="grid cols-2">
        <article class="panel stack">
          <h2>Create folder</h2>
          <p class="muted">Folder will be created inside this vault. No manual ID input needed.</p>
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
            <button class="btn primary full" type="submit">Create folder</button>
          </form>
        </article>

        <article class="panel stack">
          <h2>Folders</h2>
          <p *ngIf="folders.length === 0" class="muted">No folders yet.</p>
          <div *ngFor="let folder of folders" class="folder-row">
            <div>
              <a [routerLink]="['/folders', folder.id]"><strong>{{ folder.name }}</strong></a>
              <p class="muted">{{ folder.description || 'No description' }}</p>
            </div>
            <button class="btn danger" type="button" (click)="deleteFolder(folder)">Delete</button>
          </div>
        </article>
      </section>

      <app-resource-form [vaultId]="vaultId" (saved)="resourceList.load()" />
      <app-resource-list #resourceList title="Vault resources" [vaultId]="vaultId" />
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
export class VaultDetailComponent implements OnInit {
  @ViewChild('resourceList') resourceList!: ResourceListComponent;

  protected vaultId = '';
  protected vault?: Vault;
  protected folders: Folder[] = [];
  protected error = '';
  protected folderForm: FolderRequest = this.emptyFolderForm();

  private readonly route = inject(ActivatedRoute);
  private readonly vaultService = inject(VaultService);
  private readonly folderService = inject(FolderService);

  ngOnInit(): void {
    this.vaultId = this.route.snapshot.paramMap.get('vaultId') ?? '';
    this.folderForm = this.emptyFolderForm();
    this.load();
  }

  protected load(): void {
    if (!this.vaultId) {
      return;
    }

    this.vaultService.get(this.vaultId).subscribe({
      next: (vault) => (this.vault = vault),
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not load vault')
    });

    this.folderService.listByVault(this.vaultId).subscribe({
      next: (folders) => (this.folders = folders.filter((folder) => !folder.parentId)),
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not load folders')
    });
  }

  protected createFolder(): void {
    if (!this.vaultId) {
      this.error = 'Vault context is missing';
      return;
    }

    this.folderService.createInVault(this.vaultId, this.folderForm).subscribe({
      next: () => {
        this.folderForm = this.emptyFolderForm();
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

  private emptyFolderForm(): FolderRequest {
    return {
      name: '',
      description: '',
      icon: 'folder',
      sortOrder: 0
    };
  }
}
