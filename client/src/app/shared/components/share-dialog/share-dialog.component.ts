import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';

export type PublicAccessLevel = 'PRIVATE' | 'VIEW' | 'EDIT';

@Component({
  selector: 'app-share-dialog',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="lv-modal-backdrop" (click)="close.emit()">
      <div class="lv-modal-card p-4" style="max-width: 500px;" (click)="$event.stopPropagation()">
        <h3 class="mb-4">Share "{{ title }}"</h3>
        
        <div class="mb-4">
          <label class="form-label fw-bold">General Access</label>
          <div class="d-flex align-items-center gap-3">
            <select class="form-select" [ngModel]="getGeneralAccess()" (ngModelChange)="setGeneralAccess($event)">
              <option value="PRIVATE">Restricted</option>
              <option value="PUBLIC">Anyone with the link</option>
            </select>
            
            <select class="form-select" *ngIf="currentAccess !== 'PRIVATE'" [(ngModel)]="currentAccess">
              <option value="VIEW">Viewer</option>
              <option value="EDIT">Editor</option>
            </select>
          </div>
          <div class="form-text mt-2">
            <span *ngIf="currentAccess === 'PRIVATE'">Only workspace members can access.</span>
            <span *ngIf="currentAccess === 'VIEW'">Anyone on the internet with the link can view.</span>
            <span *ngIf="currentAccess === 'EDIT'">Anyone on the internet with the link can edit.</span>
          </div>
        </div>

        <div class="mb-4" *ngIf="currentAccess !== 'PRIVATE'">
          <label class="form-label fw-bold">Public Link</label>
          <div class="input-group">
            <input type="text" class="form-control" [value]="getPublicUrl()" readonly #linkInput>
            <button class="btn btn-outline-secondary" type="button" (click)="copyLink(linkInput)">Copy</button>
          </div>
        </div>

        <div class="text-end">
          <button type="button" class="btn btn-outline-secondary me-2" (click)="close.emit()">Cancel</button>
          <button type="button" class="btn btn-primary" (click)="save()">Save</button>
        </div>
      </div>
    </div>
  `
})
export class ShareDialogComponent {
  @Input() title = '';
  @Input() currentAccess: PublicAccessLevel = 'PRIVATE';
  @Input() publicUrlPath = ''; // e.g. '/p/v/123'
  
  @Output() close = new EventEmitter<void>();
  @Output() accessChanged = new EventEmitter<PublicAccessLevel>();

  getGeneralAccess(): string {
    return this.currentAccess === 'PRIVATE' ? 'PRIVATE' : 'PUBLIC';
  }

  setGeneralAccess(val: string) {
    if (val === 'PRIVATE') {
      this.currentAccess = 'PRIVATE';
    } else if (this.currentAccess === 'PRIVATE') {
      this.currentAccess = 'VIEW'; // Default when switching to public
    }
  }

  getPublicUrl(): string {
    return window.location.origin + this.publicUrlPath;
  }

  copyLink(input: HTMLInputElement) {
    input.select();
    document.execCommand('copy');
  }

  save() {
    this.accessChanged.emit(this.currentAccess);
  }
}
