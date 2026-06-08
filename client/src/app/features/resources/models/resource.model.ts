import { Tag } from '../../tags/models/tag.model';

export type ResourceType = 'LINK' | 'FILE' | 'NOTE' | 'SNIPPET';

export interface Resource {
  id: string;
  vaultId: string;
  vaultName: string;
  folderId?: string | null;
  folderName?: string | null;
  title: string;
  description?: string | null;
  resourceType: ResourceType;
  url?: string | null;
  fileUrl?: string | null;
  fileName?: string | null;
  fileSize?: number | null;
  mimeType?: string | null;
  storageProvider?: string | null;
  storageKey?: string | null;
  content?: string | null;
  codeLanguage?: string | null;
  sourceName?: string | null;
  thumbnailUrl?: string | null;
  previewTitle?: string | null;
  previewDescription?: string | null;
  faviconUrl?: string | null;
  siteName?: string | null;
  canonicalUrl?: string | null;
  previewFetchedAt?: string | null;
  previewStatus?: string | null;
  previewError?: string | null;
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
  rootOnly?: boolean;
  favorite?: boolean;
}

export interface LinkPreviewRequest {
  url: string;
}

export interface ResourcePreview {
  id: string;
  resourceType: ResourceType;
  title: string;
  previewUrl?: string | null;
  mimeType?: string | null;
  fileName?: string | null;
  supported: boolean;
  reason?: string | null;
}

export interface LinkPreview {
  requestedUrl?: string | null;
  url?: string | null;
  sourceName?: string | null;
  domain?: string | null;
  thumbnailUrl?: string | null;
  previewTitle?: string | null;
  previewDescription?: string | null;
  faviconUrl?: string | null;
  siteName?: string | null;
  canonicalUrl?: string | null;
  previewFetchedAt?: string | null;
  previewStatus?: string | null;
  previewError?: string | null;
}

export interface DocumentPreview {
  id: string;
  title: string;
  fileName?: string | null;
  mimeType?: string | null;
  plainText: string;
  paragraphCount: number;
  supported: boolean;
  reason?: string | null;
}
