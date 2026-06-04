import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Tag, TagRequest } from '../../core/models/tag.model';
import { TagService } from '../../core/services/tag.service';

@Component({
  selector: 'app-tags',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section>
      <div class="lv-page-header mb-4">
        <div class="lv-page-header-copy">
          <h1 class="lv-page-title">Tags</h1>
          <p class="lv-muted fs-6 mb-0">Organize resources by labels.</p>
        </div>
        <button class="btn btn-primary d-inline-flex align-items-center gap-2" type="button" (click)="openCreate()">
          <span class="material-symbols-outlined" style="font-size:20px">add</span>
          New Tag
        </button>
      </div>

      <div *ngIf="error" class="alert alert-danger">{{ error }}</div>

      <div *ngIf="loading" class="lv-card p-4"><span class="spinner-border spinner-border-sm me-2"></span>Loading tags...</div>

      <ng-container *ngIf="!loading && tags.length > 0; else emptyTpl">
        <div class="row g-4">
          <div class="col-sm-6 col-lg-4 col-xl-3" *ngFor="let tag of tags">
            <article class="lv-card lv-card-hover p-4 h-100">
              <div class="d-flex justify-content-between align-items-start gap-3 mb-3">
                <div class="d-flex align-items-center gap-3 min-w-0 flex-grow-1">
                  <span class="rounded-circle d-inline-block" style="width:12px;height:12px" [style.background]="tag.color || '#003d9b'"></span>
                  <h2 class="lv-section-title fs-5 text-truncate mb-0">{{ tag.name }}</h2>
                </div>
                <div class="dropdown">
                  <button class="lv-icon-button" type="button" data-bs-toggle="dropdown">
                    <span class="material-symbols-outlined">more_vert</span>
                  </button>
                  <ul class="dropdown-menu dropdown-menu-end border-0 shadow p-2">
                    <li><button class="dropdown-item rounded-2" type="button" (click)="openEdit(tag)">Edit</button></li>
                    <li><button class="dropdown-item rounded-2 text-danger" type="button" (click)="delete(tag)">Delete</button></li>
                  </ul>
                </div>
              </div>
              <p class="lv-muted mb-0">{{ tag.usageCount }} resources tagged</p>
            </article>
          </div>
        </div>
      </ng-container>

      <ng-template #emptyTpl>
        <div *ngIf="!loading" class="lv-empty-state">
          <span class="material-symbols-outlined d-block mb-3" style="font-size:42px">sell</span>
          <h2 class="lv-section-title">No tags yet</h2>
          <p>Create labels to organize resources faster.</p>
          <button class="btn btn-primary" type="button" (click)="openCreate()">Create tag</button>
        </div>
      </ng-template>
    </section>

    <div class="lv-modal-backdrop" *ngIf="modalOpen" (click)="closeModal()">
      <section class="lv-modal-card p-4" (click)="$event.stopPropagation()">
        <div class="d-flex justify-content-between align-items-start gap-3 mb-3">
          <div>
            <h2 class="lv-section-title mb-1">{{ editing ? 'Edit Tag' : 'Create Tag' }}</h2>
            <p class="lv-muted mb-0">Use color to visually group resources.</p>
          </div>
          <button class="lv-icon-button" type="button" (click)="closeModal()"><span class="material-symbols-outlined">close</span></button>
        </div>

        <form class="row g-3" (ngSubmit)="save()">
          <div class="col-md-8">
            <label class="form-label fw-semibold">Name</label>
            <input class="form-control" name="tagName" required [(ngModel)]="form.name" />
          </div>
          <div class="col-md-4">
            <label class="form-label fw-semibold">Color</label>
            <input class="form-control form-control-color w-100" name="tagColor" type="color" [(ngModel)]="form.color" />
          </div>
          <div class="col-12 lv-form-actions">
            <button class="btn btn-outline-secondary" type="button" (click)="closeModal()">Cancel</button>
            <button class="btn btn-primary" type="submit" [disabled]="saving">
              <span *ngIf="saving" class="spinner-border spinner-border-sm me-2"></span>
              Save tag
            </button>
          </div>
        </form>
      </section>
    </div>
  `
})
export class TagsComponent implements OnInit {
  protected tags: Tag[] = [];
  protected loading = false;
  protected saving = false;
  protected error = '';
  protected modalOpen = false;
  protected editing?: Tag;
  protected form: TagRequest = this.emptyForm();

  private readonly tagService = inject(TagService);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading = true;
    this.error = '';
    this.tagService.list().subscribe({
      next: (tags) => {
        this.loading = false;
        this.tags = tags;
      },
      error: (error) => {
        this.loading = false;
        this.error = error instanceof Error ? error.message : 'Could not load tags';
      }
    });
  }

  protected openCreate(): void {
    this.editing = undefined;
    this.form = this.emptyForm();
    this.modalOpen = true;
  }

  protected openEdit(tag: Tag): void {
    this.editing = tag;
    this.form = { name: tag.name, color: tag.color ?? '#003d9b' };
    this.modalOpen = true;
  }

  protected closeModal(): void {
    this.modalOpen = false;
    this.editing = undefined;
    this.form = this.emptyForm();
  }

  protected save(): void {
    this.saving = true;
    const action = this.editing ? this.tagService.update(this.editing.id, this.form) : this.tagService.create(this.form);
    action.subscribe({
      next: () => {
        this.saving = false;
        this.closeModal();
        this.load();
      },
      error: (error) => {
        this.saving = false;
        this.error = error instanceof Error ? error.message : 'Could not save tag';
      }
    });
  }

  protected delete(tag: Tag): void {
    if (!confirm(`Delete tag "${tag.name}"?`)) return;
    this.tagService.delete(tag.id).subscribe({ next: () => this.load(), error: (error) => (this.error = error instanceof Error ? error.message : 'Could not delete tag') });
  }

  private emptyForm(): TagRequest {
    return { name: '', color: '#003d9b' };
  }
}
