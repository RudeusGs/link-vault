import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-settings-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <section class="container-fluid">
      <div class="lv-page-header mb-4">
        <div class="lv-page-header-copy">
          <h1 class="lv-page-title">Settings</h1>
          <p class="lv-muted fs-6 mb-0">Manage your profile, workspaces, and preferences.</p>
        </div>
      </div>

      <div class="row g-4">
        <div class="col-md-3">
          <div class="lv-card p-3">
            <nav class="nav flex-column gap-1">
              <a 
                class="nav-link lv-nav-link d-flex align-items-center gap-2" 
                routerLink="/settings/profile" 
                routerLinkActive="active"
              >
                <span class="material-symbols-outlined" style="font-size: 20px;">person</span>
                Profile & Sessions
              </a>
              <a 
                class="nav-link lv-nav-link d-flex align-items-center gap-2" 
                routerLink="/settings/workspaces" 
                routerLinkActive="active"
              >
                <span class="material-symbols-outlined" style="font-size: 20px;">workspaces</span>
                Workspaces
              </a>
              <a 
                class="nav-link lv-nav-link d-flex align-items-center gap-2" 
                routerLink="/settings/billing" 
                routerLinkActive="active"
              >
                <span class="material-symbols-outlined" style="font-size: 20px;">credit_card</span>
                Billing & Plans
              </a>
              <a 
                class="nav-link lv-nav-link d-flex align-items-center gap-2" 
                routerLink="/settings/audit-logs" 
                routerLinkActive="active"
              >
                <span class="material-symbols-outlined" style="font-size: 20px;">history</span>
                Audit Logs
              </a>
            </nav>
          </div>
        </div>
        <div class="col-md-9">
          <router-outlet></router-outlet>
        </div>
      </div>
    </section>
  `
})
export class SettingsLayoutComponent {}
