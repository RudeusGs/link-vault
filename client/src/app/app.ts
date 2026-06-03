import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { ApiService } from './core/services/api.service';
import { AuthService } from './core/services/auth.service';

type HealthState = 'checking' | 'online' | 'offline';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit {
  protected readonly auth = inject(AuthService);
  protected readonly healthState = signal<HealthState>('checking');
  protected readonly healthMessage = signal('Checking API');

  private readonly api = inject(ApiService);
  private readonly router = inject(Router);

  ngOnInit(): void {
    this.checkHealth();

    if (this.auth.isAuthenticated()) {
      this.auth.loadMe().subscribe({ error: () => undefined });
    }
  }

  protected logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  get userInitial(): string {
    const user = this.auth.currentUser();
    return (user?.displayName?.[0] || user?.username?.[0] || 'U').toUpperCase();
  }

  protected checkHealth(): void {
    this.healthState.set('checking');
    this.api.getHealth().subscribe({
      next: (response) => {
        this.healthState.set(response.success ? 'online' : 'offline');
        this.healthMessage.set(response.message || 'Backend is online');
      },
      error: (error) => {
        this.healthState.set('offline');
        this.healthMessage.set(error instanceof Error ? error.message : 'Backend is not reachable');
      }
    });
  }
}
