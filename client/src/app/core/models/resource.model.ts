import { Tag } from './tag.model';

export type ResourceType = 'LINK' | 'FILE' | 'NOTE' | 'SNIPPET';

export interface Resource {
  id: string;
  vaultId: string;
  vaultName: string;
  folderId?: string | null;
  folderName?: string | null;
  title: string;
  description?: string;
  resourceType: ResourceType;
  url?: string;
  fileUrl?: string;
  fileName?: string;
  fileSize?: number;
  mimeType?: string;
  storageProvider?: string;
  storageKey?: string;
  content?: string;
  codeLanguage?: string;
  sourceName?: string;
  thumbnailUrl?: string;
  isFavorite: boolean;
  isArchived: boolean;
  tags: Tag[];
  createdAt: string;
  updatedAt: string;
}

export interface ResourceRequest {
  title: string;
  description?: string;
  resourceType: ResourceType;
  url?: string;
  content?: string;
  codeLanguage?: string;
  sourceName?: string;
  thumbnailUrl?: string;
}

export interface ResourceSearchParams {
  keyword?: string;
  type?: ResourceType | '';
  tagId?: string;
  vaultId?: string;
  folderId?: string;
  favorite?: boolean;
}

export interface ResourcePreview {
  id: string;
  resourceType: ResourceType;
  title: string;
  previewUrl?: string;
  mimeType?: string;
  fileName?: string;
  supported: boolean;
  reason?: string;
}
