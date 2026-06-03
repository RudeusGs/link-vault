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
    <section class="page">
      <header class="page-header">
        <div>
          <p class="eyebrow">Tags</p>
          <h1>Tag manager</h1>
        </div>
      </header>

      <div *ngIf="error" class="error">{{ error }}</div>

      <section class="panel stack">
        <h2>{{ editingId ? 'Edit tag' : 'Create tag' }}</h2>
        <form class="form-grid" (ngSubmit)="save()">
          <label>
            Name
            <input name="name" required [(ngModel)]="form.name" />
          </label>
          <label>
            Color
            <input name="color" placeholder="#edf5f2" [(ngModel)]="form.color" />
          </label>
          <div class="row full">
            <button class="btn primary" type="submit">{{ editingId ? 'Update' : 'Create' }}</button>
            <button class="btn" type="button" (click)="reset()">Cancel</button>
          </div>
        </form>
      </section>

      <section class="grid cols-3">
        <article class="card tag-card" *ngFor="let tag of tags">
          <span class="pill" [style.background]="tag.color || null">{{ tag.name }}</span>
          <p class="muted">{{ tag.usageCount }} resources</p>
          <div class="row wrap">
            <button class="btn" type="button" (click)="edit(tag)">Edit</button>
            <button class="btn danger" type="button" (click)="remove(tag)">Delete</button>
          </div>
        </article>
      </section>
    </section>
  `,
  styles: [
    `
      .tag-card {
        display: grid;
        gap: 12px;
      }
    `
  ]
})
export class TagsComponent implements OnInit {
  protected tags: Tag[] = [];
  protected form: TagRequest = this.emptyForm();
  protected editingId?: string;
  protected error = '';

  private readonly tagService = inject(TagService);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.tagService.list().subscribe({
      next: (tags) => (this.tags = tags),
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not load tags')
    });
  }

  protected save(): void {
    const action = this.editingId
      ? this.tagService.update(this.editingId, this.form)
      : this.tagService.create(this.form);

    action.subscribe({
      next: () => {
        this.reset();
        this.load();
      },
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not save tag')
    });
  }

  protected edit(tag: Tag): void {
    this.editingId = tag.id;
    this.form = { name: tag.name, color: tag.color };
  }

  protected remove(tag: Tag): void {
    if (!confirm(`Delete tag "${tag.name}"?`)) {
      return;
    }

    this.tagService.delete(tag.id).subscribe({
      next: () => this.load(),
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not delete tag')
    });
  }

  protected reset(): void {
    this.editingId = undefined;
    this.form = this.emptyForm();
    this.error = '';
  }

  private emptyForm(): TagRequest {
    return { name: '', color: '#edf5f2' };
  }
}
