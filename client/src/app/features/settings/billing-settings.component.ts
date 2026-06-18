import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { WorkspaceService } from './data-access/workspace.service';
import { Workspace } from './models/workspace.model';

@Component({
  selector: 'app-billing-settings',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="lv-card p-4 mb-4">
      <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 class="lv-section-title mb-1">Billing & Plans</h2>
          <p class="lv-muted mb-0">Manage your subscription and billing details.</p>
        </div>
      </div>

      <div *ngIf="error" class="alert alert-danger">{{ error }}</div>
      
      <div class="mb-4">
        <label class="form-label fw-bold">Select Workspace</label>
        <select class="form-select" [ngModel]="activeWorkspace?.id" (ngModelChange)="selectWorkspace($event)">
          <option *ngFor="let ws of workspaces" [value]="ws.id">{{ ws.name }}</option>
        </select>
      </div>

      <div *ngIf="activeWorkspace">
        <div class="p-4 rounded-4 mb-4" [ngClass]="getPlanBgClass(activeWorkspace.plan)">
          <div class="d-flex justify-content-between align-items-center">
            <div>
              <h4 class="mb-1 d-flex align-items-center gap-2">
                Current Plan: <strong>{{ activeWorkspace.plan }}</strong>
                <span *ngIf="activeWorkspace.plan === 'PRO'" class="badge bg-primary text-white">Most Popular</span>
              </h4>
              <p class="mb-0 text-muted">Billed monthly. Next billing date: 1st of next month.</p>
            </div>
          </div>
        </div>

        <h4 class="mb-3">Upgrade Options</h4>
        <div class="row g-4">
          <!-- Free Plan -->
          <div class="col-md-4">
            <div class="lv-card p-4 h-100 d-flex flex-column" [class.border-primary]="activeWorkspace.plan === 'FREE'">
              <h5 class="fw-bold">Free</h5>
              <h3 class="fw-bold">$0<span class="fs-6 text-muted fw-normal">/mo</span></h3>
              <ul class="list-unstyled mt-3 mb-4 flex-grow-1">
                <li><span class="material-symbols-outlined text-success fs-6 me-1">check</span> 1 Vault</li>
                <li><span class="material-symbols-outlined text-success fs-6 me-1">check</span> 50 Resources</li>
                <li><span class="material-symbols-outlined text-success fs-6 me-1">check</span> Standard Support</li>
              </ul>
              <button 
                class="btn w-100" 
                [ngClass]="activeWorkspace.plan === 'FREE' ? 'btn-secondary' : 'btn-outline-primary'"
                [disabled]="activeWorkspace.plan === 'FREE' || upgrading"
                (click)="upgradePlan('FREE')">
                {{ activeWorkspace.plan === 'FREE' ? 'Current Plan' : 'Downgrade' }}
              </button>
            </div>
          </div>

          <!-- Pro Plan -->
          <div class="col-md-4">
            <div class="lv-card p-4 h-100 d-flex flex-column" [class.border-primary]="activeWorkspace.plan === 'PRO'">
              <h5 class="fw-bold">Pro</h5>
              <h3 class="fw-bold">$9<span class="fs-6 text-muted fw-normal">/mo</span></h3>
              <ul class="list-unstyled mt-3 mb-4 flex-grow-1">
                <li><span class="material-symbols-outlined text-success fs-6 me-1">check</span> Unlimited Vaults</li>
                <li><span class="material-symbols-outlined text-success fs-6 me-1">check</span> Unlimited Resources</li>
                <li><span class="material-symbols-outlined text-success fs-6 me-1">check</span> Priority Support</li>
              </ul>
              <button 
                class="btn w-100" 
                [ngClass]="activeWorkspace.plan === 'PRO' ? 'btn-secondary' : 'btn-primary'"
                [disabled]="activeWorkspace.plan === 'PRO' || upgrading"
                (click)="upgradePlan('PRO')">
                {{ activeWorkspace.plan === 'PRO' ? 'Current Plan' : 'Upgrade to Pro' }}
              </button>
            </div>
          </div>

          <!-- Team Plan -->
          <div class="col-md-4">
            <div class="lv-card p-4 h-100 d-flex flex-column" [class.border-primary]="activeWorkspace.plan === 'TEAM'">
              <h5 class="fw-bold">Team</h5>
              <h3 class="fw-bold">$29<span class="fs-6 text-muted fw-normal">/mo</span></h3>
              <ul class="list-unstyled mt-3 mb-4 flex-grow-1">
                <li><span class="material-symbols-outlined text-success fs-6 me-1">check</span> Everything in Pro</li>
                <li><span class="material-symbols-outlined text-success fs-6 me-1">check</span> Up to 10 Members</li>
                <li><span class="material-symbols-outlined text-success fs-6 me-1">check</span> Team Collaboration</li>
              </ul>
              <button 
                class="btn w-100" 
                [ngClass]="activeWorkspace.plan === 'TEAM' ? 'btn-secondary' : 'btn-primary'"
                [disabled]="activeWorkspace.plan === 'TEAM' || upgrading"
                (click)="upgradePlan('TEAM')">
                {{ activeWorkspace.plan === 'TEAM' ? 'Current Plan' : 'Upgrade to Team' }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Mock Checkout Modal -->
    <div class="lv-modal-backdrop" *ngIf="showMockCheckout">
      <div class="lv-modal-card p-4" style="max-width: 400px;">
        <div class="text-center mb-4">
          <span class="material-symbols-outlined text-success" style="font-size: 48px;">check_circle</span>
          <h3 class="mt-2">Payment Successful</h3>
          <p class="text-muted">Your workspace has been upgraded to the {{ pendingPlan }} plan.</p>
        </div>
        <button class="btn btn-primary w-100" (click)="completeUpgrade()">Continue to Dashboard</button>
      </div>
    </div>
  `
})
export class BillingSettingsComponent implements OnInit {
  protected workspaces: Workspace[] = [];
  protected activeWorkspace: Workspace | null = null;
  protected error = '';
  protected upgrading = false;
  
  protected showMockCheckout = false;
  protected pendingPlan: string = '';

  private readonly wsService = inject(WorkspaceService);

  ngOnInit() {
    this.wsService.list().subscribe({
      next: (data) => {
        this.workspaces = data;
        if (this.workspaces.length > 0) {
          this.activeWorkspace = this.workspaces[0];
        }
      },
      error: (err) => this.error = err.message
    });
  }

  selectWorkspace(id: string) {
    this.activeWorkspace = this.workspaces.find(w => w.id === id) || null;
  }

  getPlanBgClass(plan: string): string {
    switch(plan) {
      case 'PRO': return 'bg-primary bg-opacity-10 border border-primary border-opacity-25';
      case 'TEAM': return 'bg-success bg-opacity-10 border border-success border-opacity-25';
      default: return 'bg-light border';
    }
  }

  upgradePlan(plan: string) {
    if (!this.activeWorkspace) return;
    this.upgrading = true;
    this.pendingPlan = plan;
    
    // Simulate a checkout delay
    setTimeout(() => {
      this.showMockCheckout = true;
    }, 800);
  }

  completeUpgrade() {
    if (!this.activeWorkspace) return;
    
    this.wsService.updatePlan(this.activeWorkspace.id, this.pendingPlan).subscribe({
      next: (updatedWs) => {
        this.activeWorkspace = updatedWs;
        
        // Update the item in the array
        const idx = this.workspaces.findIndex(w => w.id === updatedWs.id);
        if (idx !== -1) {
          this.workspaces[idx] = updatedWs;
        }
        
        this.showMockCheckout = false;
        this.upgrading = false;
      },
      error: (err) => {
        this.error = err.message;
        this.showMockCheckout = false;
        this.upgrading = false;
      }
    });
  }
}
