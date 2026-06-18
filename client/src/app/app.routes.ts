import { Routes } from '@angular/router';

import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () => import('./features/public/landing/landing.component').then((m) => m.LandingComponent)
  },
  {
    path: 'login',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'register',
    canActivate: [guestGuard],
    loadComponent: () => import('./features/auth/register.component').then((m) => m.RegisterComponent)
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent)
  },
  {
    path: 'vaults',
    canActivate: [authGuard],
    loadComponent: () => import('./features/vaults/vaults.component').then((m) => m.VaultsComponent)
  },
  {
    path: 'vaults/:vaultId',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/vaults/vault-detail.component').then((m) => m.VaultDetailComponent)
  },
  {
    path: 'folders/:folderId',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/folders/folder-detail.component').then((m) => m.FolderDetailComponent)
  },
  {
    path: 'resources',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/resources/resources-page.component').then((m) => m.ResourcesPageComponent)
  },
  {
    path: 'resources/:resourceId',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/resources/resource-detail.component').then((m) => m.ResourceDetailComponent)
  },
  {
    path: 'tags',
    canActivate: [authGuard],
    loadComponent: () => import('./features/tags/tags.component').then((m) => m.TagsComponent)
  },
  {
    path: 'settings',
    canActivate: [authGuard],
    loadComponent: () => import('./features/settings/settings-layout.component').then((m) => m.SettingsLayoutComponent),
    children: [
      { path: '', redirectTo: 'profile', pathMatch: 'full' },
      { 
        path: 'profile', 
        loadComponent: () => import('./features/settings/profile-settings.component').then((m) => m.ProfileSettingsComponent) 
      },
      { 
        path: 'workspaces', 
        loadComponent: () => import('./features/settings/workspace-settings.component').then((m) => m.WorkspaceSettingsComponent) 
      },
      { 
        path: 'billing', 
        loadComponent: () => import('./features/settings/billing-settings.component').then((m) => m.BillingSettingsComponent)
      },
      { 
        path: 'audit-logs', 
        loadComponent: () => import('./features/settings/audit-logs.component').then((m) => m.AuditLogsComponent) 
      }
    ]
  },
  {
    path: 'invitations/:token',
    canActivate: [authGuard],
    loadComponent: () => import('./features/workspaces/invitation-accept.component').then((m) => m.InvitationAcceptComponent)
  },
  {
    path: 'public',
    loadChildren: () => import('./features/public/public.routes').then((m) => m.PUBLIC_ROUTES)
  },
  {
    path: '**',
    redirectTo: 'dashboard'
  }
];
