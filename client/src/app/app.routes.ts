import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'dashboard'
  },
  {
    path: 'dashboard',
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent)
  },
  {
    path: 'vaults',
    loadComponent: () => import('./features/vaults/vaults.component').then((m) => m.VaultsComponent)
  },
  {
    path: 'vaults/:vaultId',
    loadComponent: () =>
      import('./features/vaults/vault-detail.component').then((m) => m.VaultDetailComponent)
  },
  {
    path: 'folders/:folderId',
    loadComponent: () =>
      import('./features/folders/folder-detail.component').then((m) => m.FolderDetailComponent)
  },
  {
    path: 'resources/:resourceId',
    loadComponent: () =>
      import('./features/resources/resource-detail.component').then((m) => m.ResourceDetailComponent)
  },
  {
    path: 'tags',
    loadComponent: () => import('./features/tags/tags.component').then((m) => m.TagsComponent)
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
