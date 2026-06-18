import { AuthUser } from '../../auth/models/auth.model';

export interface Workspace {
  id: string;
  name: string;
  slug: string;
  description?: string | null;
  plan: 'FREE' | 'PRO' | 'TEAM';
  role: 'OWNER' | 'ADMIN' | 'MEMBER' | 'VIEWER';
  createdAt: string;
}

export interface WorkspaceRequest {
  name: string;
  description?: string | null;
}

export interface WorkspaceMember {
  id: string;
  user: AuthUser;
  role: 'OWNER' | 'ADMIN' | 'MEMBER';
  joinedAt: string;
}

export interface WorkspaceUsage {
  storageBytes: number;
  vaultsCount: number;
  limitStorageBytes: number;
  limitVaults: number;
}
