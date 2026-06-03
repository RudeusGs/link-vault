import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { ApiService } from './core/services/api.service';
import { environment } from '../environments/environment';

type HealthState = 'checking' | 'online' | 'offline';

@Component({
  selector: 'app-root',
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App implements OnInit {
  protected readonly apiUrl = environment.apiUrl;
  protected readonly healthState = signal<HealthState>('checking');
  protected readonly healthMessage = signal('Checking backend health');

  private readonly api = inject(ApiService);

  ngOnInit(): void {
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
  }
}
