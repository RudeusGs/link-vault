import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { RegisterRequest } from '../../core/models/auth.model';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <section class="auth-page">
      <article class="auth-card">
        <div class="auth-brand">
          <span class="brand-mark">LV</span>
          <div>
            <p class="eyebrow">Start clean</p>
            <h1>Create account</h1>
          </div>
        </div>

        <div *ngIf="error" class="error">{{ error }}</div>

        <form class="auth-form" (ngSubmit)="register()">
          <label>
            Username
            <input
              name="username"
              autocomplete="username"
              required
              minlength="3"
              [(ngModel)]="form.username"
              (blur)="checkAvailability()"
            />
            <small *ngIf="usernameMessage" [class.bad]="usernameTaken">{{ usernameMessage }}</small>
          </label>

          <label>
            Email / Gmail
            <input
              name="email"
              type="email"
              autocomplete="email"
              required
              [(ngModel)]="form.email"
              (blur)="checkAvailability()"
            />
            <small *ngIf="emailMessage" [class.bad]="emailTaken">{{ emailMessage }}</small>
          </label>

          <label>
            Display name
            <input name="displayName" autocomplete="name" [(ngModel)]="form.displayName" />
          </label>

          <label>
            Password
            <input
              name="password"
              type="password"
              autocomplete="new-password"
              required
              minlength="6"
              [(ngModel)]="form.password"
            />
          </label>

          <button class="btn primary" type="submit" [disabled]="loading || emailTaken || usernameTaken">
            {{ loading ? 'Creating...' : 'Create account' }}
          </button>
        </form>

        <p class="auth-footer">
          Already have an account?
          <a routerLink="/login">Login</a>
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
        width: min(100%, 470px);
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

      small {
        color: #2f7d6d;
        font-weight: 700;
      }

      small.bad {
        color: #8f2f28;
      }

      .auth-footer {
        text-align: center;
      }
    `
  ]
})
export class RegisterComponent {
  protected form: RegisterRequest = {
    email: '',
    username: '',
    displayName: '',
    password: ''
  };
  protected loading = false;
  protected error = '';
  protected emailTaken = false;
  protected usernameTaken = false;
  protected emailMessage = '';
  protected usernameMessage = '';

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  protected checkAvailability(): void {
    const email = this.form.email.trim();
    const username = this.form.username.trim();

    if (!email && !username) {
      return;
    }

    this.authService.checkAvailability(email, username).subscribe({
      next: (result) => {
        this.emailTaken = Boolean(email) && !result.emailAvailable;
        this.usernameTaken = Boolean(username) && !result.usernameAvailable;
        this.emailMessage = email ? (this.emailTaken ? 'Email is already registered' : 'Email is available') : '';
        this.usernameMessage = username
          ? this.usernameTaken
            ? 'Username is already taken'
            : 'Username is available'
          : '';
      },
      error: () => {
        this.emailMessage = '';
        this.usernameMessage = '';
      }
    });
  }

  protected register(): void {
    this.error = '';
    this.loading = true;

    this.authService.register({
      email: this.form.email.trim(),
      username: this.form.username.trim(),
      password: this.form.password,
      displayName: this.form.displayName?.trim() || undefined
    }).subscribe({
      next: () => this.router.navigateByUrl('/dashboard'),
      error: (error) => {
        this.loading = false;
        this.error = error instanceof Error ? error.message : 'Could not register';
      }
    });
  }
}
