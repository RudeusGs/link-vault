export interface Tag {
  id: string;
  name: string;
  color?: string;
  usageCount: number;
}

export interface TagRequest {
  name: string;
  color?: string;
}
