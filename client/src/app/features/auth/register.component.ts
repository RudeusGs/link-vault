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
    <main class="lv-auth-bg d-flex align-items-center justify-content-center p-3">
      <section class="lv-auth-card p-4 p-md-5">
        <div class="text-center mb-4">
          <div class="d-inline-flex align-items-center justify-content-center lv-primary-bg rounded-3 shadow-sm mb-3" style="width:52px;height:52px">
            <span class="material-symbols-outlined" style="font-size:30px">inventory_2</span>
          </div>
          <h1 class="lv-section-title lv-primary mb-1">LinkVault</h1>
          <p class="lv-muted mb-0">Create your professional resource hub</p>
        </div>

        <form class="d-grid gap-3" (ngSubmit)="register()">
          <div *ngIf="error" class="alert alert-danger py-2 mb-0">{{ error }}</div>

          <div>
            <label class="form-label fw-semibold">Username</label>
            <div class="position-relative">
              <span class="material-symbols-outlined lv-input-icon">alternate_email</span>
              <input class="form-control lv-input-with-icon" name="username" required placeholder="johndoe" [(ngModel)]="form.username" (blur)="checkAvailability()" />
            </div>
            <small *ngIf="availabilityMessage" [class.text-success]="availabilityOk" [class.text-danger]="!availabilityOk">{{ availabilityMessage }}</small>
          </div>

          <div>
            <label class="form-label fw-semibold">Email</label>
            <div class="position-relative">
              <span class="material-symbols-outlined lv-input-icon">mail</span>
              <input class="form-control lv-input-with-icon" name="email" required type="email" placeholder="you@gmail.com" [(ngModel)]="form.email" (blur)="checkAvailability()" />
            </div>
          </div>

          <div>
            <label class="form-label fw-semibold">Display name</label>
            <div class="position-relative">
              <span class="material-symbols-outlined lv-input-icon">badge</span>
              <input class="form-control lv-input-with-icon" name="displayName" required placeholder="Nguyễn Văn A" [(ngModel)]="form.displayName" />
            </div>
          </div>

          <div class="row g-3">
            <div class="col-md-6">
              <label class="form-label fw-semibold">Password</label>
              <input class="form-control" name="password" required type="password" minlength="6" placeholder="••••••••" [(ngModel)]="form.password" />
            </div>
            <div class="col-md-6">
              <label class="form-label fw-semibold">Confirm</label>
              <input class="form-control" name="confirmPassword" required type="password" placeholder="••••••••" [(ngModel)]="confirmPassword" />
            </div>
          </div>

          <button class="btn btn-primary py-2 fw-semibold mt-2" type="submit" [disabled]="loading || !passwordsMatch">
            <span *ngIf="loading" class="spinner-border spinner-border-sm me-2"></span>
            Create account
          </button>
        </form>

        <div class="text-center mt-4 pt-3 border-top">
          <span class="lv-muted">Already have an account?</span>
          <a class="lv-primary fw-semibold ms-1" routerLink="/login">Sign in</a>
        </div>
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
