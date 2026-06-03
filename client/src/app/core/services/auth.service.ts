import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  AuthAvailability,
  AuthResponse,
  AuthUser,
  LoginRequest,
  RegisterRequest
} from '../models/auth.model';
import { ApiService } from './api.service';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly accessTokenKey = 'linkvault.accessToken';
  private readonly userKey = 'linkvault.user';
  private readonly api = inject(ApiService);

  private readonly accessToken = signal<string | null>(this.readStoredToken());
  readonly currentUser = signal<AuthUser | null>(this.readStoredUser());
  readonly isAuthenticated = computed(() => Boolean(this.accessToken()));

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.api.post<AuthResponse>('/auth/login', request).pipe(
      tap((response) => this.storeSession(response))
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.api.post<AuthResponse>('/auth/register', request).pipe(
      tap((response) => this.storeSession(response))
    );
  }

  loadMe(): Observable<AuthUser> {
    return this.api.get<AuthUser>('/auth/me').pipe(tap((user) => this.storeUser(user)));
  }

  checkAvailability(email?: string, username?: string): Observable<AuthAvailability> {
    return this.api.get<AuthAvailability>('/auth/availability', { email, username });
  }

  getAccessToken(): string | null {
    return this.accessToken();
  }

  hasToken(): boolean {
    return Boolean(this.accessToken());
  }

  logout(): void {
    this.clearSession();
  }

  clearSession(): void {
    try {
      localStorage.removeItem(this.accessTokenKey);
      localStorage.removeItem(this.userKey);
    } catch {
      // localStorage can fail in restricted browser modes. Keep app state consistent anyway.
    }

    this.accessToken.set(null);
    this.currentUser.set(null);
  }

  private storeSession(response: AuthResponse): void {
    try {
      localStorage.setItem(this.accessTokenKey, response.accessToken);
    } catch {
      // Ignore storage failures; the current tab still keeps the user signal.
    }

    this.accessToken.set(response.accessToken);
    this.storeUser(response.user);
  }

  private storeUser(user: AuthUser): void {
    try {
      localStorage.setItem(this.userKey, JSON.stringify(user));
    } catch {
      // Ignore storage failures; the current tab still keeps the user signal.
    }

    this.currentUser.set(user);
  }

  private readStoredToken(): string | null {
    try {
      return localStorage.getItem(this.accessTokenKey);
    } catch {
      return null;
    }
  }

  private readStoredUser(): AuthUser | null {
    try {
      const raw = localStorage.getItem(this.userKey);
      return raw ? (JSON.parse(raw) as AuthUser) : null;
    } catch {
      return null;
    }
  }
}
