import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { DashboardSummary } from '../models/dashboard-summary.model';
import { ApiService } from '../../../core/http/api.service';
import { WorkspaceService } from '../../settings/data-access/workspace.service';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly api = inject(ApiService);
  private readonly workspaceService = inject(WorkspaceService);

  getSummary(): Observable<DashboardSummary> {
    return this.api.get<DashboardSummary>(`${this.workspaceService.pathPrefix()}/dashboard/summary`);
  }
}
