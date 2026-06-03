import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import {
  AuthResponse,
  AuthUser,
  AvailabilityResponse,
  LoginRequest,
  RegisterRequest
} from '../models/auth.model';
import { ApiService } from './api.service';

const TOKEN_KEY = 'linkvault.access_token';
const USER_KEY = 'linkvault.user';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly tokenState = signal<string | null>(this.readToken());
  private readonly userState = signal<AuthUser | null>(this.readUser());

  readonly currentUser = this.userState.asReadonly();
  readonly token = this.tokenState.asReadonly();
  readonly isLoggedIn = computed(() => Boolean(this.tokenState()));

  isAuthenticated(): boolean {
    return Boolean(this.tokenState());
  }

  login(request: LoginRequest): Observable<AuthResponse> {
    return this.api.post<AuthResponse>('/auth/login', request).pipe(
      tap((response) => this.applySession(response))
    );
  }

  register(request: RegisterRequest): Observable<AuthResponse> {
    return this.api.post<AuthResponse>('/auth/register', request).pipe(
      tap((response) => this.applySession(response))
    );
  }

  loadMe(): Observable<AuthUser> {
    return this.api.get<AuthUser>('/auth/me').pipe(tap((user) => this.setUser(user)));
  }

  checkAvailability(username: string, email: string): Observable<AvailabilityResponse> {
    return this.api.get<AvailabilityResponse>('/auth/availability', { username, email });
  }

  logout(): void {
    this.tokenState.set(null);
    this.userState.set(null);
    this.safeStorageRemove(TOKEN_KEY);
    this.safeStorageRemove(USER_KEY);
  }

  private applySession(response: AuthResponse): void {
    const rawToken = response.accessToken ?? response.token ?? '';
    const token = rawToken.startsWith('Bearer ') ? rawToken.slice(7) : rawToken;

    if (!token) {
      throw new Error('Backend không trả access token');
    }

    this.tokenState.set(token);
    this.safeStorageSet(TOKEN_KEY, token);

    if (response.user) {
      this.setUser(response.user);
      return;
    }

    this.loadMe().subscribe({ error: () => undefined });
  }

  private setUser(user: AuthUser): void {
    this.userState.set(user);
    this.safeStorageSet(USER_KEY, JSON.stringify(user));
  }

  private readToken(): string | null {
    return this.safeStorageGet(TOKEN_KEY);
  }

  private readUser(): AuthUser | null {
    const raw = this.safeStorageGet(USER_KEY);
    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      return null;
    }
  }

  private safeStorageGet(key: string): string | null {
    try {
      return localStorage.getItem(key);
    } catch {
      return null;
    }
  }

  private safeStorageSet(key: string, value: string): void {
    try {
      localStorage.setItem(key, value);
    } catch {
      // ignore storage errors
    }
  }

  private safeStorageRemove(key: string): void {
    try {
      localStorage.removeItem(key);
    } catch {
      // ignore storage errors
    }
  }
}
