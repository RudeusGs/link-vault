export interface Tag {
  id: string;
  name: string;
  color?: string | null;
  usageCount: number;
}

export interface TagRequest {
  name: string;
  color?: string;
}
