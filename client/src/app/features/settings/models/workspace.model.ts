export type WorkspacePlan = 'FREE' | 'PRO' | 'TEAM';
export type WorkspaceRole = 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER';
export type InvitationStatus = 'PENDING' | 'ACCEPTED' | 'DECLINED' | 'CANCELLED' | 'EXPIRED';

export interface Workspace {
  id: string;
  name: string;
  slug: string;
  ownerUserId: string;
  plan: WorkspacePlan;
  role: WorkspaceRole;
  createdAt: string;
  updatedAt?: string;
}

export interface WorkspaceRequest {
  name: string;
}

export interface WorkspaceMember {
  id: string;
  userId: string;
  username: string;
  email: string;
  displayName?: string | null;
  role: WorkspaceRole;
  joinedAt: string;
}

export interface WorkspaceInvitation {
  id: string;
  workspaceId: string;
  workspaceName?: string | null;
  invitedIdentifier: string;
  invitedUserId?: string | null;
  invitedByUserId: string;
  invitedByDisplayName?: string | null;
  invitedByUsername?: string | null;
  role: WorkspaceRole;
  token: string;
  acceptPath: string;
  status: InvitationStatus;
  expiresAt: string;
  acceptedAt?: string | null;
  createdAt: string;
  updatedAt?: string | null;
}

export interface WorkspaceUsage {
  storageBytes: number;
  vaultsCount: number;
  limitStorageBytes: number;
  limitVaults: number;
}
