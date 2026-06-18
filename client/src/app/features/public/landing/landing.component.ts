import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterModule],
  styles: [`
    .landing-page {
      background: #0f172a;
      min-height: 100vh;
      font-family: 'Inter', sans-serif;
    }
    .text-gradient {
      background: linear-gradient(135deg, #38bdf8, #818cf8);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    .hero-section {
      padding: 120px 0 60px;
    }
    .hero-title {
      line-height: 1.1;
      letter-spacing: -0.02em;
    }
    .glow-blob {
      position: absolute;
      width: 500px;
      height: 500px;
      background: linear-gradient(135deg, rgba(56, 189, 248, 0.3), rgba(129, 140, 248, 0.3));
      filter: blur(100px);
      border-radius: 50%;
      z-index: 0;
      opacity: 0.6;
    }
    .glow-blob.top-left {
      top: -100px;
      left: -100px;
    }
    .glow-blob.bottom-right {
      bottom: -100px;
      right: -100px;
    }
    .glass-panel {
      background: rgba(30, 41, 59, 0.7);
      backdrop-filter: blur(16px);
      border: 1px solid rgba(255, 255, 255, 0.1);
    }
    .mockup-panel {
      border-top: 1px solid rgba(255,255,255,0.2);
    }
    .mockup-dot {
      width: 12px;
      height: 12px;
      border-radius: 50%;
    }
    .hover-lift {
      transition: transform 0.3s cubic-bezier(0.4, 0, 0.2, 1), box-shadow 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    }
    .hover-lift:hover {
      transform: translateY(-8px);
      box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
    }
    .pricing-card {
      transition: all 0.3s ease;
    }
    .btn-ghost {
      background: transparent;
      border: 1px solid transparent;
    }
    .btn-ghost:hover {
      background: rgba(255, 255, 255, 0.1);
    }
    .py-6 {
      padding-top: 5rem;
      padding-bottom: 5rem;
    }
    .bg-darker {
      background: #0b1120;
    }
    .btn-outline-light {
      border-color: rgba(255, 255, 255, 0.2);
    }
    .btn-outline-light:hover {
      background: rgba(255, 255, 255, 0.1);
      border-color: rgba(255, 255, 255, 0.3);
      color: white;
    }
  `],
  template: `
    <div class="landing-page">
      <!-- Navbar -->
      <nav class="navbar navbar-expand-lg navbar-dark bg-transparent pt-4 pb-4">
        <div class="container">
          <a class="navbar-brand d-flex align-items-center fw-bold fs-4" routerLink="/">
            <span class="material-symbols-outlined text-primary me-2 fs-3">lock</span>
            LinkVault
          </a>
          <div class="d-flex ms-auto">
            <a routerLink="/login" class="btn btn-ghost text-white me-3 fw-medium">Log In</a>
            <a routerLink="/register" class="btn btn-primary px-4 fw-medium shadow-sm">Get Started</a>
          </div>
        </div>
      </nav>

      <!-- Hero Section -->
      <section class="hero-section text-center position-relative overflow-hidden">
        <div class="glow-blob top-left"></div>
        <div class="glow-blob bottom-right"></div>
        
        <div class="container position-relative z-1">
          <div class="row justify-content-center">
            <div class="col-lg-8">
              <span class="badge bg-primary bg-opacity-10 text-primary mb-3 px-3 py-2 rounded-pill fw-medium border border-primary border-opacity-25">
                ✨ The Ultimate Digital Vault
              </span>
              <h1 class="display-3 fw-bold text-white mb-4 hero-title">
                Organize Your Digital Life<br>
                <span class="text-gradient">In One Secure Place</span>
              </h1>
              <p class="lead text-light text-opacity-75 mb-5 px-md-5 fw-light">
                LinkVault is a premium workspace for all your links, files, notes, and code snippets.
                Stop losing important resources and start finding them instantly.
              </p>
              <div class="d-flex justify-content-center gap-3">
                <a routerLink="/register" class="btn btn-primary btn-lg px-5 py-3 fw-medium shadow-lg hover-lift">
                  Start for Free
                </a>
                <a href="#features" class="btn btn-outline-light btn-lg px-5 py-3 fw-medium hover-lift">
                  Explore Features
                </a>
              </div>
            </div>
          </div>
          
          <!-- Mockup Image / Illustration -->
          <div class="row justify-content-center mt-5 pt-4">
            <div class="col-lg-10">
              <div class="glass-panel mockup-panel shadow-2xl p-2 rounded-4">
                <div class="mockup-header d-flex gap-2 p-3 pb-2">
                  <div class="mockup-dot bg-danger"></div>
                  <div class="mockup-dot bg-warning"></div>
                  <div class="mockup-dot bg-success"></div>
                </div>
                <div class="mockup-body bg-dark rounded-3 overflow-hidden d-flex align-items-center justify-content-center" style="height: 400px;">
                   <div class="text-center text-muted">
                     <span class="material-symbols-outlined fs-1 mb-2 text-primary">dashboard</span>
                     <h5>Beautiful Dashboard</h5>
                     <p>Manage your vaults, folders, and resources with ease.</p>
                   </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Features Section -->
      <section id="features" class="features-section py-6">
        <div class="container">
          <div class="text-center mb-5 pb-3">
            <h2 class="display-5 fw-bold text-white mb-3">Everything you need</h2>
            <p class="lead text-muted">Powerful features designed for maximum productivity.</p>
          </div>
          
          <div class="row g-4">
            <div class="col-md-4">
              <div class="feature-card glass-panel p-4 rounded-4 h-100 hover-lift">
                <div class="icon-box bg-primary bg-opacity-10 text-primary mb-4 rounded-3 d-inline-flex p-3">
                  <span class="material-symbols-outlined fs-2">folder_special</span>
                </div>
                <h4 class="text-white fw-semibold mb-3">Nested Vaults</h4>
                <p class="text-muted mb-0">Organize your resources into infinite nested folders. Keep your workspace clean and structural.</p>
              </div>
            </div>
            <div class="col-md-4">
              <div class="feature-card glass-panel p-4 rounded-4 h-100 hover-lift">
                <div class="icon-box bg-success bg-opacity-10 text-success mb-4 rounded-3 d-inline-flex p-3">
                  <span class="material-symbols-outlined fs-2">preview</span>
                </div>
                <h4 class="text-white fw-semibold mb-3">Smart Previews</h4>
                <p class="text-muted mb-0">Automatically fetch metadata, Open Graph images, and descriptions for every link you save.</p>
              </div>
            </div>
            <div class="col-md-4">
              <div class="feature-card glass-panel p-4 rounded-4 h-100 hover-lift">
                <div class="icon-box bg-warning bg-opacity-10 text-warning mb-4 rounded-3 d-inline-flex p-3">
                  <span class="material-symbols-outlined fs-2">group</span>
                </div>
                <h4 class="text-white fw-semibold mb-3">Team Collaboration</h4>
                <p class="text-muted mb-0">Invite your team to workspaces. Share resources securely with role-based access control.</p>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Pricing Section -->
      <section class="pricing-section py-6 bg-darker">
        <div class="container">
          <div class="text-center mb-5 pb-3">
            <h2 class="display-5 fw-bold text-white mb-3">Simple, transparent pricing</h2>
            <p class="lead text-muted">Start for free, upgrade when you need more power.</p>
          </div>
          
          <div class="row justify-content-center g-4">
            <!-- Free Plan -->
            <div class="col-lg-4 col-md-6">
              <div class="pricing-card glass-panel p-5 rounded-4 h-100 d-flex flex-column hover-lift">
                <h3 class="text-white fw-semibold mb-2">Free</h3>
                <p class="text-muted mb-4">Perfect for getting started.</p>
                <div class="price-display mb-4">
                  <span class="display-4 fw-bold text-white">$0</span>
                  <span class="text-muted">/mo</span>
                </div>
                <ul class="list-unstyled mb-5 flex-grow-1">
                  <li class="d-flex align-items-center mb-3 text-light">
                    <span class="material-symbols-outlined text-primary me-2">check_circle</span>
                    1 Vault
                  </li>
                  <li class="d-flex align-items-center mb-3 text-light">
                    <span class="material-symbols-outlined text-primary me-2">check_circle</span>
                    Up to 50 Resources
                  </li>
                  <li class="d-flex align-items-center mb-3 text-light">
                    <span class="material-symbols-outlined text-primary me-2">check_circle</span>
                    Standard Support
                  </li>
                </ul>
                <a routerLink="/register" class="btn btn-outline-primary btn-lg w-100 fw-medium">Get Started</a>
              </div>
            </div>

            <!-- Pro Plan -->
            <div class="col-lg-4 col-md-6">
              <div class="pricing-card glass-panel p-5 rounded-4 h-100 d-flex flex-column hover-lift border-primary border-opacity-50 position-relative">
                <div class="position-absolute top-0 start-50 translate-middle badge rounded-pill bg-primary px-3 py-2">
                  Most Popular
                </div>
                <h3 class="text-white fw-semibold mb-2">Pro</h3>
                <p class="text-muted mb-4">For power users.</p>
                <div class="price-display mb-4">
                  <span class="display-4 fw-bold text-white">$9</span>
                  <span class="text-muted">/mo</span>
                </div>
                <ul class="list-unstyled mb-5 flex-grow-1">
                  <li class="d-flex align-items-center mb-3 text-light">
                    <span class="material-symbols-outlined text-primary me-2">check_circle</span>
                    Unlimited Vaults
                  </li>
                  <li class="d-flex align-items-center mb-3 text-light">
                    <span class="material-symbols-outlined text-primary me-2">check_circle</span>
                    Unlimited Resources
                  </li>
                  <li class="d-flex align-items-center mb-3 text-light">
                    <span class="material-symbols-outlined text-primary me-2">check_circle</span>
                    Priority Support
                  </li>
                </ul>
                <a routerLink="/register" class="btn btn-primary btn-lg w-100 fw-medium shadow-sm">Upgrade to Pro</a>
              </div>
            </div>

            <!-- Team Plan -->
            <div class="col-lg-4 col-md-6">
              <div class="pricing-card glass-panel p-5 rounded-4 h-100 d-flex flex-column hover-lift">
                <h3 class="text-white fw-semibold mb-2">Team</h3>
                <p class="text-muted mb-4">For growing businesses.</p>
                <div class="price-display mb-4">
                  <span class="display-4 fw-bold text-white">$29</span>
                  <span class="text-muted">/mo</span>
                </div>
                <ul class="list-unstyled mb-5 flex-grow-1">
                  <li class="d-flex align-items-center mb-3 text-light">
                    <span class="material-symbols-outlined text-primary me-2">check_circle</span>
                    Everything in Pro
                  </li>
                  <li class="d-flex align-items-center mb-3 text-light">
                    <span class="material-symbols-outlined text-primary me-2">check_circle</span>
                    Up to 10 Team Members
                  </li>
                  <li class="d-flex align-items-center mb-3 text-light">
                    <span class="material-symbols-outlined text-primary me-2">check_circle</span>
                    Advanced Analytics
                  </li>
                </ul>
                <a routerLink="/register" class="btn btn-outline-light btn-lg w-100 fw-medium">Start Free Trial</a>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- Footer -->
      <footer class="footer py-5 border-top border-secondary border-opacity-25">
        <div class="container text-center">
          <a class="navbar-brand d-inline-flex align-items-center fw-bold fs-4 text-white mb-4" routerLink="/">
            <span class="material-symbols-outlined text-primary me-2 fs-3">lock</span>
            LinkVault
          </a>
          <p class="text-muted mb-0">&copy; 2026 LinkVault Inc. All rights reserved.</p>
        </div>
      </footer>
    </div>
  `
})
export class LandingComponent {
}
