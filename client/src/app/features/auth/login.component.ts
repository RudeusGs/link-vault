import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { LoginRequest } from '../../core/models/auth.model';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="auth-page">
      <article class="auth-card">
        <div class="auth-brand">
          <span class="brand-mark">LV</span>
          <div>
            <p class="eyebrow">Welcome back</p>
            <h1>Login to LinkVault</h1>
          </div>
        </div>

        <div *ngIf="error" class="error">{{ error }}</div>

        <form class="auth-form" (ngSubmit)="login()">
          <label>
            Username
            <input
              name="username"
              autocomplete="username"
              required
              minlength="3"
              [(ngModel)]="form.username"
            />
          </label>

          <label>
            Password
            <input
              name="password"
              type="password"
              autocomplete="current-password"
              required
              [(ngModel)]="form.password"
            />
          </label>

          <button class="btn primary" type="submit" [disabled]="loading">
            {{ loading ? 'Logging in...' : 'Login' }}
          </button>
        </form>

        <p class="auth-footer">
          New here?
          <a routerLink="/register">Create an account</a>
        </p>
      </article>
    </section>
  `,
  styles: [
    `
      .auth-page {
        display: grid;
        min-height: 100dvh;
        place-items: center;
        padding: 24px;
        background: linear-gradient(135deg, #183c36, #f4f7f6);
      }

      .auth-card {
        display: grid;
        width: min(100%, 430px);
        gap: 18px;
        padding: 28px;
        border: 1px solid #d8e4df;
        border-radius: 14px;
        background: #ffffff;
        box-shadow: 0 18px 50px rgba(31, 41, 51, 0.16);
      }

      .auth-brand {
        display: flex;
        align-items: center;
        gap: 12px;
      }

      .brand-mark {
        display: inline-grid;
        width: 44px;
        height: 44px;
        place-items: center;
        border-radius: 10px;
        color: #183c36;
        background: #f6c66d;
        font-weight: 900;
      }

      .auth-form {
        display: grid;
        gap: 14px;
      }

      .auth-footer {
        text-align: center;
      }
    `
  ]
})
export class LoginComponent {
  protected form: LoginRequest = { username: '', password: '' };
  protected loading = false;
  protected error = '';

  private readonly authService = inject(AuthService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected login(): void {
    this.error = '';
    this.loading = true;

    this.authService.login({
      username: this.form.username.trim(),
      password: this.form.password
    }).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/dashboard';
        this.router.navigateByUrl(returnUrl);
      },
      error: (error) => {
        this.loading = false;
        this.error = error instanceof Error ? error.message : 'Could not login';
      }
    });
  }
}
