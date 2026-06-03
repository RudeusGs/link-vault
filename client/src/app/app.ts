import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';

import { ApiService } from './core/services/api.service';
import { AuthService } from './core/services/auth.service';

type HealthState = 'checking' | 'online' | 'offline';

@Component({
  selector: 'app-root',
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit {
  protected readonly auth = inject(AuthService);
  protected readonly healthState = signal<HealthState>('checking');
  protected readonly healthMessage = signal('Checking backend health');
  private readonly currentUrl = signal('');

  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.currentUrl.set(this.router.url);
    this.router.events.pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd)).subscribe({
      next: (event) => this.currentUrl.set(event.urlAfterRedirects)
    });

    this.api.getHealth().subscribe({
      next: (response) => {
        this.healthState.set(response.success ? 'online' : 'offline');
        this.healthMessage.set(response.message);
      },
      error: () => {
        this.healthState.set('offline');
        this.healthMessage.set('Backend is not reachable');
      }
    });

    if (this.auth.hasToken()) {
      this.auth.loadMe().subscribe({
        error: () => this.auth.clearSession()
      });
    }
  }

  protected isAuthPage(): boolean {
    return this.currentUrl().startsWith('/login') || this.currentUrl().startsWith('/register');
  }

  protected initials(value: string): string {
    return value
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase() ?? '')
      .join('');
  }

  protected logout(): void {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}
