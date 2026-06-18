import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, ViewChild, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { FolderService } from '../folders/data-access/folder.service';
import { Folder, FolderRequest } from '../folders/models/folder.model';
import { ResourceService } from '../resources/data-access/resource.service';
import { Resource, ResourceType } from '../resources/models/resource.model';
import { VaultService } from './data-access/vault.service';
import { Vault, VaultRequest } from './models/vault.model';
import { ResourceFormComponent } from '../resources/resource-form.component';
import { ResourceListComponent } from '../resources/resource-list.component';
import { VaultIconPickerComponent } from './vault-icon-picker.component';
import { ShareDialogComponent, PublicAccessLevel } from '../../shared/components/share-dialog/share-dialog.component';

interface FolderNode extends Folder {
  children: FolderNode[];
  expanded: boolean;
  resources: Resource[];
  resourceCount: number;
}

@Component({
  selector: 'app-vault-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, ResourceFormComponent, ResourceListComponent, VaultIconPickerComponent, ShareDialogComponent],
  template: `
    <section *ngIf="!loading && vault; else loadingTpl">
      <div class="lv-page-header mb-4">
        <div class="lv-page-header-copy">
          <a routerLink="/vaults" class="d-inline-flex align-items-center gap-2 lv-primary fw-semibold mb-2">
            <span class="material-symbols-outlined" style="font-size:18px">arrow_back</span>
            Back to Vaults
          </a>
          <div class="d-flex align-items-center gap-3 flex-wrap">
            <span class="lv-icon-box lv-icon-box-lg" [style.color]="vault.color || null">
              <span class="material-symbols-outlined" style="font-size:28px">{{ iconFor(vault.icon) }}</span>
            </span>
            <h1 class="lv-page-title lv-break-title">{{ vault.name }}</h1>
            <span class="badge rounded-pill lv-badge-soft px-3 py-2">Active Project</span>
          </div>
          <p class="lv-muted mb-0 mt-1 lv-line-clamp-3 text-break">{{ vault.description || 'Centralized resource hub for links, files, notes and snippets.' }}</p>
        </div>

        <div class="lv-action-toolbar">
          <button class="btn btn-primary d-inline-flex align-items-center gap-2" type="button" (click)="openShareVault()">
            <span class="material-symbols-outlined" style="font-size:18px">share</span>
            Share
          </button>
          <button class="btn btn-outline-primary d-inline-flex align-items-center gap-2" type="button" (click)="openVaultEdit()">
            <span class="material-symbols-outlined" style="font-size:18px">edit</span>
            Edit
          </button>
          <button class="btn btn-outline-danger d-inline-flex align-items-center gap-2" type="button" (click)="deleteVault()">
            <span class="material-symbols-outlined" style="font-size:18px">delete</span>
            Delete
          </button>
        </div>
      </div>

      <div *ngIf="error" class="alert alert-danger">{{ error }}</div>

      <div class="row g-4">
        <div class="col-lg-4 col-xl-3">
          <aside class="lv-soft-panel lv-folder-panel d-flex flex-column">
            <div class="p-3 border-bottom d-flex align-items-center justify-content-between">
              <div class="d-flex align-items-center gap-2">
                <span class="material-symbols-outlined lv-primary">folder_shared</span>
                <strong>Folders</strong>
              </div>
              <button class="lv-icon-button" type="button" title="New folder" (click)="openFolderCreate(null)">
                <span class="material-symbols-outlined">create_new_folder</span>
              </button>
            </div>

            <div class="flex-grow-1 p-2 lv-tree-scroll">
              <div class="lv-folder-row d-flex align-items-center gap-1">
                <button class="lv-folder-node d-flex align-items-center justify-content-between" [class.active]="!selectedFolder" type="button" (click)="selectRoot()">
                  <span class="d-flex align-items-center gap-2 min-w-0">
                    <span class="material-symbols-outlined" style="font-size:20px">folder_open</span>
                    <span class="fw-semibold text-truncate">All Resources</span>
                  </span>
                  <small class="lv-tree-count">{{ treeResources.length }}</small>
                </button>
              </div>

              <ul *ngIf="rootResources.length" class="list-unstyled m-0 border-start ms-3 ps-2">
                <li *ngFor="let resource of rootResources; trackBy: trackResourceById">
                  <a class="lv-tree-resource" [routerLink]="['/resources', resource.id]" [title]="resourceLabel(resource)">
                    <span class="lv-tree-resource-icon" [ngClass]="resourceAccent(resource)">
                      <span class="material-symbols-outlined">{{ resourceIcon(resource) }}</span>
                    </span>
                    <span class="lv-tree-resource-main">
                      <span class="lv-tree-resource-title">{{ resourceLabel(resource) }}</span>
                      <small class="lv-tree-resource-meta">{{ resourceMeta(resource) }}</small>
                    </span>
                  </a>
                </li>
              </ul>

              <ul class="list-unstyled m-0 mt-1">
                <ng-container *ngFor="let folder of tree">
                  <ng-container *ngTemplateOutlet="folderTpl; context: { $implicit: folder, level: 0 }"></ng-container>
                </ng-container>
              </ul>
            </div>

            <div class="p-3 border-top">
              <button class="btn btn-primary w-100 d-inline-flex align-items-center justify-content-center gap-2" type="button" (click)="openFolderCreate(selectedFolder || null)">
                <span class="material-symbols-outlined" style="font-size:18px">add</span>
                New Folder
              </button>
            </div>
          </aside>
        </div>

        <div class="col-lg-8 col-xl-9">
          <section class="lv-card p-4 mb-4">
            <nav class="d-flex align-items-center gap-2 lv-muted small mb-3 flex-wrap">
              <a routerLink="/vaults" class="lv-primary">Vaults</a>
              <span class="material-symbols-outlined" style="font-size:16px">chevron_right</span>
              <span class="lv-breadcrumb-label">{{ vault.name }}</span>
              <ng-container *ngIf="selectedFolder">
                <span class="material-symbols-outlined" style="font-size:16px">chevron_right</span>
                <strong class="text-dark lv-breadcrumb-label">{{ selectedFolder.name }}</strong>
              </ng-container>
            </nav>

            <div class="lv-section-toolbar align-end mb-4">
              <div class="min-w-0">
                <h2 class="lv-page-title fs-2 lv-break-title">{{ selectedFolder?.name || vault.name }}</h2>
                <p class="lv-muted mb-0 lv-line-clamp-2 text-break">{{ selectedFolder?.description || (selectedFolder ? 'Selected folder resources.' : 'Resources stored in vault root.') }}</p>
              </div>
              <div class="lv-action-toolbar compact">
                <button class="btn btn-outline-secondary d-inline-flex align-items-center gap-2" type="button" (click)="openFolderCreate(selectedFolder || null)">
                  <span class="material-symbols-outlined" style="font-size:18px">create_new_folder</span>
                  New Folder
                </button>
                <button class="btn btn-outline-primary" type="button" (click)="openResource('LINK')">New Link</button>
                <button class="btn btn-outline-primary" type="button" (click)="openResource('NOTE')">New Note</button>
                <button class="btn btn-outline-primary" type="button" (click)="openResource('SNIPPET')">New Snippet</button>
                <button class="btn btn-primary d-inline-flex align-items-center gap-2" type="button" (click)="openResource('FILE')">
                  <span class="material-symbols-outlined" style="font-size:18px">upload</span>
                  Upload
                </button>
              </div>
            </div>

            <app-resource-list
              #resourceList
              [title]="selectedFolder ? 'Folder resources' : 'Vault resources'"
              [vaultId]="vault.id"
              [folderId]="selectedFolder?.id || null"
              [rootOnly]="!selectedFolder"
            />
          </section>
        </div>
      </div>

      <app-resource-form #resourceForm [vaultId]="vault.id" [folderId]="selectedFolder?.id || null" (saved)="reloadResources()" />
    </section>

    <ng-template #folderTpl let-folder let-level="level">
      <li [style.marginLeft.px]="level * 16">
        <div class="lv-folder-row d-flex align-items-center gap-1">
          <button class="lv-folder-node d-flex align-items-center justify-content-between" [class.active]="selectedFolder?.id === folder.id" type="button" (click)="selectFolder(folder)">
            <span class="d-flex align-items-center gap-2 min-w-0">
              <span *ngIf="hasFolderBranch(folder); else noBranchTpl" class="material-symbols-outlined lv-folder-toggle" (click)="toggleFolder(folder, $event)">{{ folder.expanded ? 'expand_more' : 'chevron_right' }}</span>
              <ng-template #noBranchTpl><span class="lv-tree-spacer"></span></ng-template>
              <span class="material-symbols-outlined" style="font-size:20px">{{ folder.expanded ? 'folder_open' : 'folder' }}</span>
              <span class="fw-semibold text-truncate">{{ folder.name }}</span>
            </span>
            <span class="d-flex align-items-center gap-1 flex-shrink-0">
              <small *ngIf="folder.resourceCount" class="lv-tree-count">{{ folder.resourceCount }}</small>
              <span class="lv-node-actions d-flex gap-1 flex-shrink-0">
              <span class="material-symbols-outlined" style="font-size:16px" (click)="openFolderCreate(folder, $event)">add</span>
              <span class="material-symbols-outlined" style="font-size:16px" (click)="openFolderEdit(folder, $event)">edit</span>
              <span class="material-symbols-outlined text-danger" style="font-size:16px" (click)="deleteFolder(folder, $event)">delete</span>
              </span>
            </span>
          </button>
        </div>
        <ul *ngIf="folder.expanded && hasFolderBranch(folder)" class="list-unstyled m-0 border-start ms-3 ps-2">
          <ng-container *ngFor="let child of folder.children">
            <ng-container *ngTemplateOutlet="folderTpl; context: { $implicit: child, level: level + 1 }"></ng-container>
          </ng-container>
          <li *ngFor="let resource of folder.resources; trackBy: trackResourceById">
            <a class="lv-tree-resource" [routerLink]="['/resources', resource.id]" [title]="resourceLabel(resource)">
              <span class="lv-tree-resource-icon" [ngClass]="resourceAccent(resource)">
                <span class="material-symbols-outlined">{{ resourceIcon(resource) }}</span>
              </span>
              <span class="lv-tree-resource-main">
                <span class="lv-tree-resource-title">{{ resourceLabel(resource) }}</span>
                <small class="lv-tree-resource-meta">{{ resourceMeta(resource) }}</small>
              </span>
            </a>
          </li>
        </ul>
      </li>
    </ng-template>

    <div class="lv-modal-backdrop" *ngIf="folderModalOpen" (click)="closeFolderModal()">
      <section class="lv-modal-card p-4" (click)="$event.stopPropagation()">
        <div class="d-flex justify-content-between align-items-start gap-3 mb-3">
          <div>
            <h2 class="lv-section-title mb-1">{{ editingFolder ? 'Edit Folder' : 'Create Folder' }}</h2>
            <p class="lv-muted mb-0">{{ folderParent ? 'Child folder under ' + folderParent.name : 'Root folder inside ' + vault?.name }}</p>
          </div>
          <button class="lv-icon-button" type="button" (click)="closeFolderModal()">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>

        <form class="row g-3" (ngSubmit)="saveFolder()">
          <div class="col-md-8">
            <label class="form-label fw-semibold">Name</label>
            <input class="form-control" name="folderName" required [(ngModel)]="folderForm.name" />
          </div>
          <div class="col-md-4">
            <label class="form-label fw-semibold">Sort order</label>
            <input class="form-control" name="sortOrder" type="number" [(ngModel)]="folderForm.sortOrder" />
          </div>
          <div class="col-12">
            <label class="form-label fw-semibold">Description</label>
            <textarea class="form-control" name="folderDescription" rows="3" [(ngModel)]="folderForm.description"></textarea>
          </div>
          <div class="col-12 lv-form-actions">
            <button class="btn btn-outline-secondary" type="button" (click)="closeFolderModal()">Cancel</button>
            <button class="btn btn-primary" type="submit" [disabled]="savingFolder">
              <span *ngIf="savingFolder" class="spinner-border spinner-border-sm me-2"></span>
              Save folder
            </button>
          </div>
        </form>
      </section>
    </div>

    <div class="lv-modal-backdrop" *ngIf="vaultModalOpen" (click)="vaultModalOpen = false">
      <section class="lv-modal-card p-4" (click)="$event.stopPropagation()">
        <div class="d-flex justify-content-between align-items-start gap-3 mb-3">
          <h2 class="lv-section-title">Edit Vault</h2>
          <button class="lv-icon-button" type="button" (click)="vaultModalOpen = false"><span class="material-symbols-outlined">close</span></button>
        </div>
        <form class="row g-3" (ngSubmit)="saveVault()">
          <div class="col-md-8"><label class="form-label fw-semibold">Name</label><input class="form-control" name="vaultName" required [(ngModel)]="vaultForm.name" /></div>
          <div class="col-md-4"><label class="form-label fw-semibold">Color</label><input class="form-control form-control-color w-100" name="vaultColor" type="color" [(ngModel)]="vaultForm.color" /></div>
          <div class="col-12"><label class="form-label fw-semibold">Icon</label><app-vault-icon-picker [selectedIcon]="vaultForm.icon" (selectedIconChange)="vaultForm.icon = $event" /></div>
          <div class="col-12"><label class="form-label fw-semibold">Description</label><textarea class="form-control" name="vaultDescription" rows="3" [(ngModel)]="vaultForm.description"></textarea></div>
          <div class="col-12 lv-form-actions"><button class="btn btn-outline-secondary" type="button" (click)="vaultModalOpen = false">Cancel</button><button class="btn btn-primary" type="submit">Save changes</button></div>
        </form>
      </section>
    </div>

    <app-share-dialog 
      *ngIf="shareModalOpen && vault"
      [title]="vault.name"
      [currentAccess]="vault.publicAccess"
      [publicUrlPath]="'/public/vaults/' + vault.id"
      (close)="shareModalOpen = false"
      (accessChanged)="updateVaultAccess($event)"
    />

    <ng-template #loadingTpl>
      <div *ngIf="loading" class="lv-card p-4">
        <span class="spinner-border spinner-border-sm me-2"></span>
        Loading vault...
      </div>
      <div *ngIf="!loading && error" class="alert alert-danger">{{ error }}</div>
    </ng-template>
  `
})
export class VaultDetailComponent implements OnInit {
  @ViewChild('resourceForm') resourceForm!: ResourceFormComponent;
  @ViewChild('resourceList') resourceList?: ResourceListComponent;

  protected vault?: Vault;
  protected loading = true;
  protected folders: Folder[] = [];
  protected tree: FolderNode[] = [];
  protected treeResources: Resource[] = [];
  protected rootResources: Resource[] = [];
  protected selectedFolder: FolderNode | null = null;
  protected error = '';
  protected folderModalOpen = false;
  protected vaultModalOpen = false;
  protected savingFolder = false;
  protected editingFolder?: FolderNode;
  protected folderParent: FolderNode | null = null;
  protected vaultForm: VaultRequest = this.emptyVaultForm();
  protected folderForm: FolderRequest = this.emptyFolderForm();

  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly vaultService = inject(VaultService);
  private readonly folderService = inject(FolderService);
  private readonly resourceService = inject(ResourceService);
  private loadRequestId = 0;

  ngOnInit(): void {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => this.load()
    });
  }

  protected load(): void {
    const vaultId = this.route.snapshot.paramMap.get('vaultId');
    if (!vaultId) {
      return;
    }

    const requestId = ++this.loadRequestId;
    this.loading = true;
    this.error = '';
    this.tree = [];
    this.treeResources = [];
    this.rootResources = [];

    this.vaultService.get(vaultId).subscribe({
      next: (vault) => {
        if (requestId !== this.loadRequestId) {
          return;
        }

        this.vault = vault;
        this.loading = false;
        this.vaultForm = {
          name: vault.name,
          description: vault.description ?? '',
          icon: vault.icon ?? 'work',
          color: vault.color ?? '#003d9b'
        };
      },
      error: (error) => {
        if (requestId === this.loadRequestId) {
          this.error = error instanceof Error ? error.message : 'Could not load vault';
          this.loading = false;
        }
      }
    });

    this.folderService.listByVault(vaultId).subscribe({
      next: (folders) => {
        if (requestId !== this.loadRequestId) {
          return;
        }

        this.folders = folders;
        this.rebuildTree();
      },
      error: (error) => {
        if (requestId === this.loadRequestId) {
          this.error = error instanceof Error ? error.message : 'Could not load folders';
        }
      }
    });

    this.loadTreeResources(vaultId, requestId);
  }

  protected selectRoot(): void {
    this.selectedFolder = null;
    queueMicrotask(() => this.resourceList?.load());
  }

  protected selectFolder(folder: FolderNode): void {
    this.selectedFolder = folder;
    queueMicrotask(() => this.resourceList?.load());
  }

  protected toggleFolder(folder: FolderNode, event: Event): void {
    event.stopPropagation();
    folder.expanded = !folder.expanded;
  }

  protected openResource(type: ResourceType): void {
    this.resourceForm.open(type);
  }

  protected reloadResources(): void {
    this.resourceList?.load();
    if (this.vault) {
      this.loadTreeResources(this.vault.id, this.loadRequestId);
    }
  }

  protected openFolderCreate(parent: FolderNode | null, event?: Event): void {
    event?.stopPropagation();
    this.editingFolder = undefined;
    this.folderParent = parent;
    this.folderForm = this.emptyFolderForm();
    this.folderModalOpen = true;
  }

  protected openFolderEdit(folder: FolderNode, event?: Event): void {
    event?.stopPropagation();
    this.editingFolder = folder;
    this.folderParent = null;
    this.folderForm = {
      name: folder.name,
      description: folder.description ?? '',
      icon: folder.icon ?? 'folder',
      sortOrder: folder.sortOrder ?? 0
    };
    this.folderModalOpen = true;
  }

  protected closeFolderModal(): void {
    this.folderModalOpen = false;
    this.editingFolder = undefined;
    this.folderParent = null;
    this.folderForm = this.emptyFolderForm();
  }

  protected saveFolder(): void {
    if (!this.vault) {
      return;
    }

    this.savingFolder = true;
    const action = this.editingFolder
      ? this.folderService.update(this.editingFolder.id, this.folderForm)
      : this.folderParent
        ? this.folderService.createChild(this.folderParent.id, this.folderForm)
        : this.folderService.createInVault(this.vault.id, this.folderForm);

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

  protected deleteFolder(folder: FolderNode, event?: Event): void {
    event?.stopPropagation();
    if (!confirm(`Delete folder "${folder.name}" and its resources?`)) {
      return;
    }

    this.folderService.delete(folder.id).subscribe({
      next: () => {
        if (this.selectedFolder?.id === folder.id) {
          this.selectedFolder = null;
        }
        this.load();
      },
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not delete folder')
    });
  }

  protected openVaultEdit(): void {
    this.vaultModalOpen = true;
  }

  protected saveVault(): void {
    if (!this.vault) {
      return;
    }

    this.vaultService.update(this.vault.id, this.vaultForm).subscribe({
      next: () => {
        this.vaultModalOpen = false;
        this.load();
      },
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not update vault')
    });
  }

  protected deleteVault(): void {
    if (!this.vault || !confirm(`Delete vault "${this.vault.name}"?`)) {
      return;
    }

    this.vaultService.delete(this.vault.id).subscribe({
      next: () => this.router.navigate(['/vaults']),
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not delete vault')
    });
  }

  protected shareModalOpen = false;

  protected openShareVault(): void {
    this.shareModalOpen = true;
  }

  protected updateVaultAccess(access: PublicAccessLevel): void {
    if (!this.vault) return;
    
    // Create a new form combining existing data and the new access level
    const updateRequest: VaultRequest = {
      name: this.vault.name,
      description: this.vault.description || undefined,
      icon: this.vault.icon || undefined,
      color: this.vault.color || undefined,
      publicAccess: access
    };

    this.vaultService.update(this.vault.id, updateRequest).subscribe({
      next: () => {
        this.shareModalOpen = false;
        this.load();
      },
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not update vault access')
    });
  }

  protected hasFolderBranch(folder: FolderNode): boolean {
    return folder.children.length > 0 || folder.resources.length > 0;
  }

  protected resourceIcon(resource: Resource): string {
    if (resource.resourceType === 'LINK') {
      return 'link';
    }
    if (resource.resourceType === 'NOTE') {
      return 'notes';
    }
    if (resource.resourceType === 'SNIPPET') {
      return 'code';
    }

    switch (this.resourceExtension(resource)) {
      case 'pdf':
        return 'picture_as_pdf';
      case 'doc':
      case 'docx':
        return 'description';
      case 'xls':
      case 'xlsx':
      case 'csv':
        return 'table_chart';
      case 'ppt':
      case 'pptx':
        return 'slideshow';
      case 'jpg':
      case 'jpeg':
      case 'png':
      case 'webp':
      case 'gif':
      case 'svg':
        return 'image';
      case 'zip':
      case 'rar':
      case '7z':
        return 'folder_zip';
      case 'mp3':
      case 'wav':
        return 'audio_file';
      case 'mp4':
      case 'webm':
      case 'mov':
        return 'video_file';
      case 'txt':
      case 'md':
      case 'json':
      case 'xml':
      case 'yaml':
      case 'yml':
      case 'log':
      case 'java':
      case 'kt':
      case 'py':
      case 'ts':
      case 'tsx':
      case 'js':
      case 'jsx':
      case 'html':
      case 'css':
      case 'scss':
      case 'sql':
      case 'sh':
      case 'ps1':
      case 'c':
      case 'cpp':
      case 'cs':
      case 'go':
      case 'rs':
      case 'php':
      case 'rb':
      case 'swift':
      case 'dart':
        return 'code';
      default:
        return 'draft';
    }
  }

  protected resourceAccent(resource: Resource): string {
    if (resource.resourceType === 'LINK') {
      return 'link';
    }
    if (resource.resourceType === 'NOTE') {
      return 'note';
    }
    if (resource.resourceType === 'SNIPPET') {
      return 'snippet';
    }

    const extension = this.resourceExtension(resource);
    if (extension === 'pdf') {
      return 'pdf';
    }
    if (['doc', 'docx'].includes(extension)) {
      return 'doc';
    }
    if (['xls', 'xlsx', 'csv'].includes(extension)) {
      return 'sheet';
    }
    if (['ppt', 'pptx'].includes(extension)) {
      return 'slide';
    }
    if (['jpg', 'jpeg', 'png', 'webp', 'gif', 'svg'].includes(extension)) {
      return 'image';
    }
    if (['zip', 'rar', '7z'].includes(extension)) {
      return 'archive';
    }
    return 'file';
  }

  protected resourceLabel(resource: Resource): string {
    return resource.fileName || resource.previewTitle || resource.title || 'Untitled resource';
  }

  protected resourceMeta(resource: Resource): string {
    if (resource.resourceType === 'FILE') {
      const extension = this.resourceExtension(resource);
      return extension ? extension.toUpperCase() : 'FILE';
    }
    if (resource.resourceType === 'LINK') {
      return resource.sourceName || resource.siteName || 'LINK';
    }
    return resource.resourceType;
  }

  protected trackResourceById(_index: number, resource: Resource): string {
    return resource.id;
  }

  protected iconFor(icon?: string | null): string {
    const normalized = icon?.trim() || 'work';
    return normalized === 'book-open' ? 'menu_book' : normalized;
  }

  private loadTreeResources(vaultId: string, requestId: number): void {
    this.resourceService.listByVault(vaultId).subscribe({
      next: (resources) => {
        if (requestId !== this.loadRequestId) {
          return;
        }

        this.treeResources = resources;
        this.rebuildTree();
      },
      error: (error) => {
        if (requestId === this.loadRequestId) {
          this.error = error instanceof Error ? error.message : 'Could not load folder resources';
        }
      }
    });
  }

  private rebuildTree(): void {
    const selectedFolderId = this.selectedFolder?.id;
    this.tree = this.buildTree(this.folders, this.treeResources);
    this.selectedFolder = selectedFolderId ? this.findNode(this.tree, selectedFolderId) : null;
  }

  private buildTree(folders: Folder[], resources: Resource[]): FolderNode[] {
    const map = new Map<string, FolderNode>();
    const resourcesByFolder = new Map<string, Resource[]>();

    this.rootResources = [];
    resources.forEach((resource) => {
      if (resource.folderId) {
        const folderResources = resourcesByFolder.get(resource.folderId) ?? [];
        folderResources.push(resource);
        resourcesByFolder.set(resource.folderId, folderResources);
        return;
      }
      this.rootResources.push(resource);
    });

    this.rootResources = this.sortResources(this.rootResources);
    folders.forEach((folder) => map.set(folder.id, {
      ...folder,
      children: [],
      expanded: true,
      resources: this.sortResources(resourcesByFolder.get(folder.id) ?? []),
      resourceCount: 0
    }));

    const roots: FolderNode[] = [];
    map.forEach((node) => {
      const parentId = node.parentId ?? null;
      if (parentId && map.has(parentId)) {
        map.get(parentId)?.children.push(node);
      } else {
        roots.push(node);
      }
    });

    roots.forEach((node) => this.updateResourceCount(node));
    return roots.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.name.localeCompare(b.name));
  }

  private updateResourceCount(node: FolderNode): number {
    node.children.sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0) || a.name.localeCompare(b.name));
    node.resourceCount = node.resources.length + node.children.reduce((total, child) => total + this.updateResourceCount(child), 0);
    return node.resourceCount;
  }

  private sortResources(resources: Resource[]): Resource[] {
    return [...resources].sort((a, b) => {
      const typeOrder = this.resourceSortOrder(a) - this.resourceSortOrder(b);
      if (typeOrder !== 0) {
        return typeOrder;
      }
      return this.resourceLabel(a).localeCompare(this.resourceLabel(b));
    });
  }

  private resourceSortOrder(resource: Resource): number {
    return resource.resourceType === 'FILE' ? 0 : resource.resourceType === 'LINK' ? 1 : resource.resourceType === 'NOTE' ? 2 : 3;
  }

  private findNode(nodes: FolderNode[], id: string): FolderNode | null {
    for (const node of nodes) {
      if (node.id === id) {
        return node;
      }
      const child = this.findNode(node.children, id);
      if (child) {
        return child;
      }
    }
    return null;
  }

  private resourceExtension(resource: Resource): string {
    const fileName = resource.fileName || resource.title || '';
    const dotIndex = fileName.lastIndexOf('.');
    if (dotIndex < 0 || dotIndex === fileName.length - 1) {
      return '';
    }
    return fileName.slice(dotIndex + 1).toLowerCase();
  }

  private emptyVaultForm(): VaultRequest {
    return { name: '', description: '', icon: 'work', color: '#003d9b' };
  }

  private emptyFolderForm(): FolderRequest {
    return { name: '', description: '', icon: 'folder', sortOrder: 0 };
  }
}
