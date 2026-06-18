export interface Vault {
  id: string;
  name: string;
  description?: string | null;
  icon?: string | null;
  color?: string | null;
  publicAccess: 'PRIVATE' | 'VIEW' | 'EDIT';
  createdAt: string;
  updatedAt: string;
}

export interface VaultRequest {
  name: string;
  description?: string;
  icon?: string;
  color?: string;
  publicAccess?: 'PRIVATE' | 'VIEW' | 'EDIT';
}
