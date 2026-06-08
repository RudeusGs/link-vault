import { Resource } from '../../resources/models/resource.model';
import { Tag } from '../../tags/models/tag.model';

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
