import { CommonModule } from '@angular/common';
import { Component, OnInit, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuditLogService } from './data-access/audit-log.service';
import { WorkspaceService } from './data-access/workspace.service';
import { AuditLog } from './models/audit-log.model';
import { Workspace } from './models/workspace.model';

@Component({
  selector: 'app-audit-logs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="lv-card p-4 mb-4">
      <div class="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h2 class="lv-section-title mb-1">Audit Logs</h2>
          <p class="lv-muted mb-0">View the history of actions in your workspaces.</p>
        </div>
        
        <div *ngIf="workspaces.length > 0">
          <select class="form-select" [(ngModel)]="selectedWorkspaceId" (ngModelChange)="loadLogs()">
            <option *ngFor="let ws of workspaces" [value]="ws.id">{{ ws.name }}</option>
          </select>
        </div>
      </div>

      <div *ngIf="error" class="alert alert-danger">{{ error }}</div>

      <div *ngIf="workspaces.length === 0 && !loading" class="alert alert-info">
        You don't belong to any workspaces yet.
      </div>

      <div class="table-responsive" *ngIf="selectedWorkspaceId">
        <table class="table table-hover align-middle">
          <thead>
            <tr>
              <th>Time</th>
              <th>Actor</th>
              <th>Action</th>
              <th>Target</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let log of logs">
              <td class="text-nowrap">{{ log.createdAt | date:'short' }}</td>
              <td><strong>{{ log.actorUsername }}</strong></td>
              <td><span class="badge bg-secondary">{{ log.action }}</span></td>
              <td>
                <div class="small">{{ log.targetType }}</div>
                <div class="lv-muted" style="font-size: 0.75rem">{{ log.targetId }}</div>
              </td>
            </tr>
            <tr *ngIf="logs.length === 0 && !logsLoading">
              <td colspan="4" class="text-center py-4 lv-muted">No audit logs found.</td>
            </tr>
          </tbody>
        </table>
        <div *ngIf="logsLoading" class="text-center py-3">
          <span class="spinner-border spinner-border-sm text-primary"></span> Loading logs...
        </div>
      </div>
    </div>
  `
})
export class AuditLogsComponent implements OnInit {
  protected workspaces: Workspace[] = [];
  protected selectedWorkspaceId: string | null = null;
  protected logs: AuditLog[] = [];
  
  protected loading = false;
  protected logsLoading = false;
  protected error = '';

  private readonly wsService = inject(WorkspaceService);
  private readonly auditService = inject(AuditLogService);

  ngOnInit() {
    this.loading = true;
    this.wsService.list().subscribe({
      next: (data) => {
        this.workspaces = data;
        if (data.length > 0) {
          this.selectedWorkspaceId = data[0].id;
          this.loadLogs();
        }
        this.loading = false;
      },
      error: (err) => {
        this.error = 'Failed to load workspaces';
        this.loading = false;
      }
    });
  }

  loadLogs() {
    if (!this.selectedWorkspaceId) return;
    
    this.logsLoading = true;
    this.error = '';
    this.auditService.list(this.selectedWorkspaceId).subscribe({
      next: (data) => {
        this.logs = data;
        this.logsLoading = false;
      },
      error: (err) => {
        this.error = 'Failed to load audit logs';
        this.logsLoading = false;
      }
    });
  }
}
