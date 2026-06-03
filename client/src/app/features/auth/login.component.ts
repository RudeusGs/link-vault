import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <main class="lv-auth-bg d-flex align-items-center justify-content-center p-3">
      <section class="lv-auth-card p-4 p-md-5">
        <div class="text-center mb-4">
          <div class="d-inline-flex align-items-center justify-content-center lv-primary-bg rounded-3 shadow-sm mb-3" style="width:52px;height:52px">
            <span class="material-symbols-outlined" style="font-size:30px">shield</span>
          </div>
          <h1 class="lv-section-title lv-primary mb-1">LinkVault</h1>
          <p class="lv-muted mb-0">Your secure digital library</p>
        </div>

        <form class="d-grid gap-3" (ngSubmit)="login()">
          <div *ngIf="error" class="alert alert-danger py-2 mb-0">{{ error }}</div>

          <div>
            <label class="form-label fw-semibold">Username</label>
            <div class="position-relative">
              <span class="material-symbols-outlined lv-input-icon">person</span>
              <input class="form-control lv-input-with-icon" name="username" required autocomplete="username" placeholder="quan1908" [(ngModel)]="username" />
            </div>
          </div>

          <div>
            <label class="form-label fw-semibold">Password</label>
            <div class="position-relative">
              <span class="material-symbols-outlined lv-input-icon">lock</span>
              <input class="form-control lv-input-with-icon" name="password" required autocomplete="current-password" placeholder="••••••••" [type]="showPassword ? 'text' : 'password'" [(ngModel)]="password" />
              <button class="btn btn-sm position-absolute top-50 end-0 translate-middle-y me-2 border-0" type="button" (click)="showPassword = !showPassword">
                <span class="material-symbols-outlined" style="font-size:20px">{{ showPassword ? 'visibility_off' : 'visibility' }}</span>
              </button>
            </div>
          </div>

          <div class="d-flex align-items-center justify-content-between small">
            <label class="form-check-label d-flex align-items-center gap-2">
              <input class="form-check-input m-0" type="checkbox" />
              Remember me
            </label>
            <a class="lv-primary fw-semibold" href="javascript:void(0)">Forgot password?</a>
          </div>

          <button class="btn btn-primary py-2 fw-semibold" type="submit" [disabled]="loading">
            <span *ngIf="loading" class="spinner-border spinner-border-sm me-2"></span>
            Sign in
          </button>
        </form>

        <div class="text-center mt-4 pt-3 border-top">
          <span class="lv-muted">New to LinkVault?</span>
          <a class="lv-primary fw-semibold ms-1" routerLink="/register">Create account</a>
        </div>
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
