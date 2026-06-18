export interface AuditLog {
  id: string;
  workspaceId: string;
  actorUserId: string;
  actorUsername: string;
  action: string;
  targetType: string;
  targetId: string;
  metadata?: string | null;
  createdAt: string;
}
