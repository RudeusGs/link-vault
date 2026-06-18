import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <main class="lv-auth-page">
      <section class="lv-auth-shell lv-auth-shell-register">
        <aside class="lv-auth-info" aria-label="LinkVault overview">
          <a class="lv-auth-logo" routerLink="/">
            <span class="lv-auth-logo-mark">L</span>
            <span>LinkVault</span>
          </a>

          <div class="lv-auth-info-copy">
            <p class="lv-auth-kicker">Start clean</p>
            <h1>Create a workspace that your future self can actually use.</h1>
            <p>
              Save important links, documents and snippets into a professional system from day one.
            </p>
          </div>

          <div class="lv-auth-summary-card">
            <div>
              <strong>Default setup</strong>
              <span>Personal workspace, vaults, tags and file previews.</span>
            </div>
            <span class="material-symbols-outlined">check_circle</span>
          </div>
        </aside>

        <section class="lv-auth-form-panel" aria-label="Create account form">
          <div class="lv-auth-form-card lv-auth-form-card-wide">
            <div class="lv-auth-form-head">
              <span class="lv-auth-pill">Create account</span>
              <h2>Set up LinkVault</h2>
              <p>Use a username for login. Email is used for workspace invites and account metadata.</p>
            </div>

            <form class="lv-auth-form" (ngSubmit)="register()" novalidate>
              <div *ngIf="error" class="lv-auth-alert" role="alert">{{ error }}</div>

              <label class="lv-auth-field">
                <span>Username</span>
                <div class="lv-auth-input-wrap">
                  <span class="material-symbols-outlined">alternate_email</span>
                  <input
                    class="lv-auth-input"
                    name="username"
                    required
                    autocomplete="username"
                    placeholder="johndoe"
                    [(ngModel)]="form.username"
                    (blur)="checkAvailability()"
                  />
                </div>
              </label>

              <label class="lv-auth-field">
                <span>Email</span>
                <div class="lv-auth-input-wrap">
                  <span class="material-symbols-outlined">mail</span>
                  <input
                    class="lv-auth-input"
                    name="email"
                    required
                    type="email"
                    autocomplete="email"
                    placeholder="you@gmail.com"
                    [(ngModel)]="form.email"
                    (blur)="checkAvailability()"
                  />
                </div>
              </label>

              <label class="lv-auth-field">
                <span>Display name</span>
                <div class="lv-auth-input-wrap">
                  <span class="material-symbols-outlined">badge</span>
                  <input
                    class="lv-auth-input"
                    name="displayName"
                    required
                    autocomplete="name"
                    placeholder="Nguyễn Văn A"
                    [(ngModel)]="form.displayName"
                  />
                </div>
              </label>

              <div *ngIf="availabilityMessage" class="lv-auth-hint" [class.success]="availabilityOk" [class.error]="!availabilityOk">
                {{ availabilityMessage }}
              </div>

              <div class="lv-auth-grid-2">
                <label class="lv-auth-field">
                  <span>Password</span>
                  <div class="lv-auth-input-wrap">
                    <span class="material-symbols-outlined">lock</span>
                    <input
                      class="lv-auth-input"
                      name="password"
                      required
                      type="password"
                      minlength="6"
                      autocomplete="new-password"
                      placeholder="••••••••"
                      [(ngModel)]="form.password"
                    />
                  </div>
                </label>

                <label class="lv-auth-field">
                  <span>Confirm password</span>
                  <div class="lv-auth-input-wrap">
                    <span class="material-symbols-outlined">verified_user</span>
                    <input
                      class="lv-auth-input"
                      name="confirmPassword"
                      required
                      type="password"
                      autocomplete="new-password"
                      placeholder="••••••••"
                      [(ngModel)]="confirmPassword"
                    />
                  </div>
                </label>
              </div>

              <button class="lv-auth-submit" type="submit" [disabled]="loading || !passwordsMatch">
                <span *ngIf="loading" class="lv-auth-spinner" aria-hidden="true"></span>
                <span>{{ loading ? 'Creating account...' : 'Create account' }}</span>
              </button>
            </form>

            <p class="lv-auth-switch">
              Already have an account?
              <a routerLink="/login">Sign in</a>
            </p>
          </div>
        </section>
      </section>
    </main>
  `
})
export class RegisterComponent {
  protected form = {
    username: '',
    email: '',
    displayName: '',
    password: ''
  };
  protected confirmPassword = '';
  protected loading = false;
  protected error = '';
  protected availabilityMessage = '';
  protected availabilityOk = false;

  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  get passwordsMatch(): boolean {
    return Boolean(this.form.password && this.form.password === this.confirmPassword);
  }

  protected checkAvailability(): void {
    if (!this.form.username || !this.form.email) {
      return;
    }

    this.auth.checkAvailability(this.form.username.trim(), this.form.email.trim()).subscribe({
      next: (result) => {
        const usernameOk = result.usernameAvailable ?? (result.usernameExists !== undefined ? !result.usernameExists : true);
        const emailOk = result.emailAvailable ?? (result.emailExists !== undefined ? !result.emailExists : true);
        this.availabilityOk = usernameOk && emailOk;
        this.availabilityMessage = this.availabilityOk
          ? 'Username and email are available'
          : 'Username or email is already used';
      },
      error: () => {
        this.availabilityMessage = '';
      }
    });
  }

  protected register(): void {
    if (!this.passwordsMatch) {
      this.error = 'Password confirmation does not match';
      return;
    }

    this.error = '';
    this.loading = true;
    this.auth.register({
      username: this.form.username.trim(),
      email: this.form.email.trim(),
      displayName: this.form.displayName.trim(),
      password: this.form.password
    }).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/dashboard']);
      },
      error: (error) => {
        this.loading = false;
        this.error = error instanceof Error ? error.message : 'Could not create account';
      }
    });
  }
}
