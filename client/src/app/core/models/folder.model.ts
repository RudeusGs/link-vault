export interface Folder {
  id: string;
  vaultId: string;
  parentId?: string | null;
  name: string;
  description?: string | null;
  icon?: string | null;
  sortOrder: number;
  createdAt: string;
  updatedAt: string;
}

export interface FolderRequest {
  name: string;
  description?: string;
  icon?: string;
  sortOrder?: number;
}
