import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Vault, VaultRequest } from '../../core/models/vault.model';
import { VaultService } from '../../core/services/vault.service';
import { VaultIconPickerComponent } from './vault-icon-picker.component';

@Component({
  selector: 'app-vaults',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, VaultIconPickerComponent],
  template: `
    <section>
      <div class="lv-page-header mb-4">
        <div class="lv-page-header-copy">
          <h1 class="lv-page-title">Vaults</h1>
          <p class="lv-muted fs-6 mb-0">Manage your personal collections.</p>
        </div>
        <button class="btn btn-primary d-inline-flex align-items-center gap-2" type="button" (click)="openCreate()">
          <span class="material-symbols-outlined" style="font-size:20px">add_circle</span>
          New Vault
        </button>
      </div>

      <div *ngIf="error" class="alert alert-danger">{{ error }}</div>

      <ng-container *ngIf="vaults.length > 0; else emptyTpl">
        <div class="row g-4">
          <div class="col-md-6 col-xl-4" *ngFor="let vault of vaults">
            <article class="lv-card lv-card-hover p-4 h-100 position-relative lv-stack-card">
              <div class="d-flex justify-content-between align-items-start mb-3">
                <a class="d-flex align-items-start gap-3 flex-grow-1 min-w-0 text-dark" [routerLink]="['/vaults', vault.id]">
                  <span class="lv-icon-box lv-icon-box-lg" [style.color]="vault.color || null">
                    <span class="material-symbols-outlined" style="font-size:28px">{{ iconFor(vault.icon) }}</span>
                  </span>
                  <span class="min-w-0">
                    <strong class="d-block fs-5 text-truncate">{{ vault.name }}</strong>
                    <small class="lv-muted d-block text-truncate">{{ vault.description || 'No description' }}</small>
                  </span>
                </a>

                <div class="dropdown">
                  <button class="lv-icon-button" type="button" data-bs-toggle="dropdown" aria-expanded="false" (click)="$event.stopPropagation()">
                    <span class="material-symbols-outlined">more_vert</span>
                  </button>
                  <ul class="dropdown-menu dropdown-menu-end border-0 shadow p-2">
                    <li><a class="dropdown-item rounded-2" [routerLink]="['/vaults', vault.id]">Open</a></li>
                    <li><button class="dropdown-item rounded-2" type="button" (click)="openEdit(vault)">Edit</button></li>
                    <li><button class="dropdown-item rounded-2 text-danger" type="button" (click)="delete(vault)">Delete</button></li>
                  </ul>
                </div>
              </div>

              <p class="lv-muted lv-line-clamp-2 mb-4">{{ vault.description || 'A clean collection for organizing folders, links, notes, files and code snippets.' }}</p>

              <div class="lv-card-footer mt-auto pt-3 border-top">
                <small class="lv-muted text-truncate">Updated {{ vault.updatedAt | date:'mediumDate' }}</small>
                <a class="btn btn-sm btn-outline-primary" [routerLink]="['/vaults', vault.id]">
                  Open
                  <span class="material-symbols-outlined ms-1" style="font-size:16px">arrow_forward</span>
                </a>
              </div>
            </article>
          </div>
        </div>
      </ng-container>

      <ng-template #emptyTpl>
        <div *ngIf="!loading" class="lv-empty-state">
          <span class="material-symbols-outlined d-block mb-3" style="font-size:42px">account_balance_wallet</span>
          <h2 class="lv-section-title">No vaults yet</h2>
          <p>Create your first vault to start saving links, files, notes and snippets.</p>
          <button class="btn btn-primary" type="button" (click)="openCreate()">Create vault</button>
        </div>
      </ng-template>

      <div *ngIf="loading" class="lv-card p-4">
        <span class="spinner-border spinner-border-sm me-2"></span>
        Loading vaults...
      </div>
    </section>

    <div class="lv-modal-backdrop" *ngIf="showForm" (click)="closeForm()">
      <section class="lv-modal-card p-4" (click)="$event.stopPropagation()">
        <div class="d-flex justify-content-between align-items-start gap-3 mb-3">
          <div>
            <h2 class="lv-section-title mb-1">{{ editing ? 'Edit Vault' : 'Create Vault' }}</h2>
            <p class="lv-muted mb-0">Only visible fields. IDs are handled by the app.</p>
          </div>
          <button class="lv-icon-button" type="button" (click)="closeForm()">
            <span class="material-symbols-outlined">close</span>
          </button>
        </div>

        <form class="row g-3" (ngSubmit)="save()">
          <div class="col-md-8">
            <label class="form-label fw-semibold">Name</label>
            <input class="form-control" name="name" required [(ngModel)]="form.name" />
          </div>
          <div class="col-md-4">
            <label class="form-label fw-semibold">Color</label>
            <input class="form-control form-control-color w-100" name="color" type="color" [(ngModel)]="form.color" />
          </div>
          <div class="col-12">
            <label class="form-label fw-semibold">Icon</label>
            <app-vault-icon-picker [selectedIcon]="form.icon" (selectedIconChange)="form.icon = $event" />
          </div>
          <div class="col-12">
            <label class="form-label fw-semibold">Description</label>
            <textarea class="form-control" name="description" rows="4" [(ngModel)]="form.description"></textarea>
          </div>
          <div class="col-12 lv-form-actions">
            <button class="btn btn-outline-secondary" type="button" (click)="closeForm()">Cancel</button>
            <button class="btn btn-primary" type="submit" [disabled]="saving">
              <span *ngIf="saving" class="spinner-border spinner-border-sm me-2"></span>
              {{ editing ? 'Save changes' : 'Create vault' }}
            </button>
          </div>
        </form>
      </section>
    </div>
  `
})
export class VaultsComponent implements OnInit {
  protected vaults: Vault[] = [];
  protected loading = false;
  protected saving = false;
  protected showForm = false;
  protected editing?: Vault;
  protected error = '';
  protected form: VaultRequest = this.emptyForm();

  private readonly vaultService = inject(VaultService);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading = true;
    this.error = '';
    this.vaultService.list().subscribe({
      next: (vaults) => {
        this.loading = false;
        this.vaults = vaults;
      },
      error: (error) => {
        this.loading = false;
        this.error = error instanceof Error ? error.message : 'Could not load vaults';
      }
    });
  }

  protected openCreate(): void {
    this.editing = undefined;
    this.form = this.emptyForm();
    this.showForm = true;
  }

  protected openEdit(vault: Vault): void {
    this.editing = vault;
    this.form = {
      name: vault.name,
      description: vault.description ?? '',
      icon: vault.icon ?? 'work',
      color: vault.color ?? '#003d9b'
    };
    this.showForm = true;
  }

  protected closeForm(): void {
    this.showForm = false;
    this.editing = undefined;
    this.form = this.emptyForm();
  }

  protected save(): void {
    this.saving = true;
    const action = this.editing
      ? this.vaultService.update(this.editing.id, this.form)
      : this.vaultService.create(this.form);

    action.subscribe({
      next: () => {
        this.saving = false;
        this.closeForm();
        this.load();
      },
      error: (error) => {
        this.saving = false;
        this.error = error instanceof Error ? error.message : 'Could not save vault';
      }
    });
  }

  protected delete(vault: Vault): void {
    if (!confirm(`Delete vault "${vault.name}"?`)) {
      return;
    }

    this.vaultService.delete(vault.id).subscribe({
      next: () => this.load(),
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not delete vault')
    });
  }

  protected iconFor(icon?: string | null): string {
    const normalized = icon?.trim() || 'work';
    return normalized === 'book-open' ? 'menu_book' : normalized;
  }

  private emptyForm(): VaultRequest {
    return {
      name: '',
      description: '',
      icon: 'work',
      color: '#003d9b'
    };
  }
}
