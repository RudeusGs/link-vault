import { Resource } from './resource.model';
import { Tag } from './tag.model';

export interface DashboardSummary {
  totalVaults: number;
  totalFolders: number;
  totalResources: number;
  totalLinks: number;
  totalFiles: number;
  totalNotes: number;
  totalSnippets: number;
  totalFavorites: number;
  recentResources: Resource[];
  topTags: Tag[];
}
