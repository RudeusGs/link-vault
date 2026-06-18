import { Routes } from '@angular/router';

export const PUBLIC_ROUTES: Routes = [
  {
    path: 'vaults/:id',
    loadComponent: () => import('./public-vault/public-vault.component').then((m) => m.PublicVaultComponent)
  },
  {
    path: 'resources/:id',
    loadComponent: () => import('./public-resource/public-resource.component').then((m) => m.PublicResourceComponent)
  }
];
