import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { Vault, VaultRequest } from '../../core/models/vault.model';
import { VaultService } from '../../core/services/vault.service';

@Component({
  selector: 'app-vaults',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="page">
      <header class="page-header">
        <div>
          <p class="eyebrow">Vaults</p>
          <h1>Vault library</h1>
        </div>
      </header>

      <div *ngIf="error" class="error">{{ error }}</div>

      <section class="panel stack">
        <h2>{{ editingId ? 'Edit vault' : 'Create vault' }}</h2>
        <form class="form-grid" (ngSubmit)="save()">
          <label>
            Name
            <input name="name" required [(ngModel)]="form.name" />
          </label>
          <label>
            Color
            <input name="color" placeholder="#2f7d6d" [(ngModel)]="form.color" />
          </label>
          <label>
            Icon
            <input name="icon" placeholder="book-open" [(ngModel)]="form.icon" />
          </label>
          <label class="full">
            Description
            <textarea name="description" [(ngModel)]="form.description"></textarea>
          </label>
          <div class="row full">
            <button class="btn primary" type="submit">{{ editingId ? 'Update' : 'Create' }}</button>
            <button class="btn" type="button" (click)="reset()">Cancel</button>
          </div>
        </form>
      </section>

      <section class="grid cols-3">
        <article class="card vault-card" *ngFor="let vault of vaults">
          <div class="row">
            <span class="swatch" [style.background]="vault.color || '#2f7d6d'"></span>
            <h2><a [routerLink]="['/vaults', vault.id]">{{ vault.name }}</a></h2>
          </div>
          <p class="muted">{{ vault.description || 'No description' }}</p>
          <div class="row wrap">
            <button class="btn" type="button" (click)="edit(vault)">Edit</button>
            <button class="btn danger" type="button" (click)="remove(vault)">Delete</button>
          </div>
        </article>
      </section>
    </section>
  `,
  styles: [
    `
      .vault-card {
        display: grid;
        gap: 12px;
      }

      .vault-card a {
        color: inherit;
        text-decoration: none;
      }

      .swatch {
        width: 16px;
        height: 16px;
        border-radius: 4px;
      }
    `
  ]
})
export class VaultsComponent implements OnInit {
  protected vaults: Vault[] = [];
  protected form: VaultRequest = this.emptyForm();
  protected editingId?: string;
  protected error = '';

  private readonly vaultService = inject(VaultService);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.vaultService.list().subscribe({
      next: (vaults) => (this.vaults = vaults),
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not load vaults')
    });
  }

  protected save(): void {
    const action = this.editingId
      ? this.vaultService.update(this.editingId, this.form)
      : this.vaultService.create(this.form);

    action.subscribe({
      next: () => {
        this.reset();
        this.load();
      },
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not save vault')
    });
  }

  protected edit(vault: Vault): void {
    this.editingId = vault.id;
    this.form = {
      name: vault.name,
      description: vault.description,
      icon: vault.icon,
      color: vault.color
    };
  }

  protected remove(vault: Vault): void {
    if (!confirm(`Delete vault "${vault.name}" and its contents?`)) {
      return;
    }

    this.vaultService.delete(vault.id).subscribe({
      next: () => this.load(),
      error: (error) => (this.error = error instanceof Error ? error.message : 'Could not delete vault')
    });
  }

  protected reset(): void {
    this.editingId = undefined;
    this.form = this.emptyForm();
    this.error = '';
  }

  private emptyForm(): VaultRequest {
    return {
      name: '',
      description: '',
      icon: '',
      color: '#2f7d6d'
    };
  }
}
