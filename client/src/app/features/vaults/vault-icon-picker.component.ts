import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';

interface VaultIconOption {
  label: string;
  icon: string;
}

@Component({
  selector: 'app-vault-icon-picker',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="lv-icon-picker" role="radiogroup" aria-label="Vault icon">
      <button
        *ngFor="let option of options"
        class="lv-icon-choice"
        [class.active]="isSelected(option.icon)"
        type="button"
        role="radio"
        [attr.aria-checked]="isSelected(option.icon)"
        [title]="option.label"
        (click)="select(option.icon)"
      >
        <span class="material-symbols-outlined">{{ option.icon }}</span>
        <span class="small text-truncate">{{ option.label }}</span>
      </button>
    </div>
  `
})
export class VaultIconPickerComponent {
  @Input() selectedIcon?: string | null;
  @Output() selectedIconChange = new EventEmitter<string>();

  protected readonly options: VaultIconOption[] = [
    { label: 'Work', icon: 'work' },
    { label: 'Wallet', icon: 'account_balance_wallet' },
    { label: 'Book', icon: 'menu_book' },
    { label: 'Code', icon: 'code' },
    { label: 'Design', icon: 'palette' },
    { label: 'School', icon: 'school' },
    { label: 'Business', icon: 'business_center' },
    { label: 'Cloud', icon: 'cloud' },
    { label: 'Folder', icon: 'folder_special' },
    { label: 'Star', icon: 'star' },
    { label: 'Travel', icon: 'flight_takeoff' },
    { label: 'Home', icon: 'home' },
    { label: 'Research', icon: 'science' },
    { label: 'Security', icon: 'lock' },
    { label: 'Finance', icon: 'payments' },
    { label: 'Idea', icon: 'lightbulb' }
  ];

  protected select(icon: string): void {
    this.selectedIcon = icon;
    this.selectedIconChange.emit(icon);
  }

  protected isSelected(icon: string): boolean {
    return (this.selectedIcon?.trim() || 'work') === icon;
  }
}
