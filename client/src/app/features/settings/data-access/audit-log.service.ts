import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/http/api.service';
import { AuditLog } from '../models/audit-log.model';

@Injectable({ providedIn: 'root' })
export class AuditLogService {
  private readonly api = inject(ApiService);

  list(workspaceId: string): Observable<AuditLog[]> {
    return this.api.get<AuditLog[]>(`/workspaces/${workspaceId}/audit-logs`);
  }
}
