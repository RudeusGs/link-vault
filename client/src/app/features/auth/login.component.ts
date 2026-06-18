import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <main class="lv-auth-page">
      <section class="lv-auth-shell">
        <aside class="lv-auth-info" aria-label="LinkVault overview">
          <a class="lv-auth-logo" routerLink="/">
            <span class="lv-auth-logo-mark">L</span>
            <span>LinkVault</span>
          </a>

          <div class="lv-auth-info-copy">
            <p class="lv-auth-kicker">Workspace resource hub</p>
            <h1>Organize links, files and notes without the noise.</h1>
            <p>
              Keep every project resource searchable, shareable and grouped by workspace.
              No messy bookmark folders, no random files lost in chat.
            </p>
          </div>

          <div class="lv-auth-benefits">
            <article>
              <span class="material-symbols-outlined">folder_managed</span>
              <div>
                <strong>Vault structure</strong>
                <small>Group resources by workspace, vault and folder.</small>
              </div>
            </article>
            <article>
              <span class="material-symbols-outlined">manage_search</span>
              <div>
                <strong>Fast retrieval</strong>
                <small>Find links, docs and snippets from one clean place.</small>
              </div>
            </article>
            <article>
              <span class="material-symbols-outlined">group</span>
              <div>
                <strong>Team ready</strong>
                <small>Invite members and manage shared workspaces.</small>
              </div>
            </article>
          </div>
        </aside>

        <section class="lv-auth-form-panel" aria-label="Sign in form">
          <div class="lv-auth-form-card">
            <div class="lv-auth-form-head">
              <span class="lv-auth-pill">Sign in</span>
              <h2>Welcome back</h2>
              <p>Enter your username and password to continue.</p>
            </div>

            <form class="lv-auth-form" (ngSubmit)="login()" novalidate>
              <div *ngIf="error" class="lv-auth-alert" role="alert">{{ error }}</div>

              <label class="lv-auth-field">
                <span>Username</span>
                <div class="lv-auth-input-wrap">
                  <span class="material-symbols-outlined">person</span>
                  <input
                    class="lv-auth-input"
                    name="username"
                    required
                    autocomplete="username"
                    placeholder="quan1908"
                    [(ngModel)]="username"
                  />
                </div>
              </label>

              <label class="lv-auth-field">
                <span>Password</span>
                <div class="lv-auth-input-wrap">
                  <span class="material-symbols-outlined">lock</span>
                  <input
                    class="lv-auth-input has-toggle"
                    name="password"
                    required
                    autocomplete="current-password"
                    placeholder="••••••••"
                    [type]="showPassword ? 'text' : 'password'"
                    [(ngModel)]="password"
                  />
                  <button
                    class="lv-auth-toggle"
                    type="button"
                    [attr.aria-label]="showPassword ? 'Hide password' : 'Show password'"
                    (click)="showPassword = !showPassword"
                  >
                    <span class="material-symbols-outlined">{{ showPassword ? 'visibility_off' : 'visibility' }}</span>
                  </button>
                </div>
              </label>

              <button class="lv-auth-submit" type="submit" [disabled]="loading">
                <span *ngIf="loading" class="lv-auth-spinner" aria-hidden="true"></span>
                <span>{{ loading ? 'Signing in...' : 'Sign in' }}</span>
              </button>
            </form>

            <p class="lv-auth-switch">
              New to LinkVault?
              <a routerLink="/register">Create an account</a>
            </p>
          </div>
        </section>
      </section>
    </main>
  `
})
export class LoginComponent {
  protected username = '';
  protected password = '';
  protected loading = false;
  protected error = '';
  protected showPassword = false;

  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected login(): void {
    this.error = '';
    this.loading = true;

    this.auth.login({ username: this.username.trim(), password: this.password }).subscribe({
      next: () => {
        this.loading = false;
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/dashboard';
        this.router.navigateByUrl(returnUrl);
      },
      error: (error) => {
        this.loading = false;
        this.error = error instanceof Error ? error.message : 'Could not sign in';
      }
    });
  }
}
