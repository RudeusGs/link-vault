import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-landing',
  standalone: true,
  imports: [CommonModule, RouterModule],
  styles: [`
    .landing-page { min-height: 100vh; color: #111827; background: #ffffff; }
    .landing-nav { height: 72px; border-bottom: 1px solid #e5e7eb; background: #ffffff; }
    .landing-hero { padding: 84px 0 56px; background: #f6f7f9; border-bottom: 1px solid #e5e7eb; }
    .landing-title { font-size: clamp(44px, 7vw, 76px); line-height: 0.96; letter-spacing: -0.06em; }
    .landing-copy { max-width: 680px; color: #4b5563; font-size: 19px; line-height: 1.7; }
    .product-frame { border: 1px solid #d1d5db; border-radius: 22px; background: #ffffff; box-shadow: 0 18px 36px #e5e7eb; overflow: hidden; }
    .product-toolbar { height: 48px; border-bottom: 1px solid #e5e7eb; background: #f9fafb; }
    .product-dot { width: 10px; height: 10px; border: 1px solid #d1d5db; border-radius: 50%; background: #ffffff; }
    .product-sidebar { border-right: 1px solid #e5e7eb; background: #f9fafb; }
    .section-block { padding: 80px 0; }
    .feature-card, .pricing-card { height: 100%; border: 1px solid #e5e7eb; border-radius: 18px; background: #ffffff; box-shadow: 0 1px 2px #e5e7eb; }
    .feature-icon { width: 44px; height: 44px; border: 1px solid #dbeafe; border-radius: 12px; color: #2563eb; background: #eff6ff; }
    .price-feature { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; color: #374151; }
  `],
  template: `
    <div class="landing-page">
      <nav class="landing-nav d-flex align-items-center">
        <div class="container d-flex align-items-center justify-content-between">
          <a class="lv-brand" routerLink="/">
            <span class="lv-brand-mark"><span class="material-symbols-outlined" style="font-size:18px">lock</span></span>
            <span>LinkVault</span>
          </a>
          <div class="d-flex align-items-center gap-2">
            <a routerLink="/login" class="btn lv-button-quiet">Log in</a>
            <a routerLink="/register" class="btn btn-primary">Get started</a>
          </div>
        </div>
      </nav>

      <header class="landing-hero">
        <div class="container">
          <div class="row align-items-center g-5">
            <div class="col-lg-6">
              <div class="lv-page-kicker">Resource workspace for teams</div>
              <h1 class="landing-title fw-bold mb-4">Save, organize, and share resources without chaos.</h1>
              <p class="landing-copy mb-4">
                LinkVault gives every workspace a clear place for vaults, folders, files, links, notes, tags, and team members.
              </p>
              <div class="d-flex flex-wrap gap-2 mb-4">
                <a routerLink="/register" class="btn btn-primary btn-lg">Start free</a>
                <a routerLink="/login" class="btn btn-lg lv-button-quiet">Open workspace</a>
              </div>
              <div class="d-flex flex-wrap gap-3 lv-muted">
                <span class="d-inline-flex align-items-center gap-1"><span class="material-symbols-outlined" style="font-size:18px">check_circle</span>Workspace roles</span>
                <span class="d-inline-flex align-items-center gap-1"><span class="material-symbols-outlined" style="font-size:18px">check_circle</span>File preview</span>
                <span class="d-inline-flex align-items-center gap-1"><span class="material-symbols-outlined" style="font-size:18px">check_circle</span>Smart link metadata</span>
              </div>
            </div>

            <div class="col-lg-6">
              <div class="product-frame">
                <div class="product-toolbar d-flex align-items-center justify-content-between px-3">
                  <div class="d-flex gap-2"><span class="product-dot"></span><span class="product-dot"></span><span class="product-dot"></span></div>
                  <span class="badge rounded-pill lv-badge-soft">Workspace: Product Team</span>
                </div>
                <div class="row g-0" style="min-height: 360px;">
                  <aside class="col-4 product-sidebar p-3">
                    <div class="small fw-bold text-uppercase lv-muted mb-3">Vaults</div>
                    <div class="d-grid gap-2">
                      <div class="lv-soft-panel p-2 fw-semibold">Research</div>
                      <div class="lv-soft-panel p-2 fw-semibold">Design System</div>
                      <div class="lv-soft-panel p-2 fw-semibold">Engineering</div>
                    </div>
                  </aside>
                  <main class="col-8 p-3">
                    <div class="d-flex align-items-center justify-content-between mb-3">
                      <strong>Recent resources</strong>
                      <button class="btn btn-sm btn-primary">Add</button>
                    </div>
                    <div class="d-grid gap-2">
                      <div class="lv-soft-panel p-3 d-flex align-items-center gap-3">
                        <span class="lv-icon-box"><span class="material-symbols-outlined">link</span></span>
                        <div><strong>Pricing reference</strong><div class="small lv-muted">link · SaaS research</div></div>
                      </div>
                      <div class="lv-soft-panel p-3 d-flex align-items-center gap-3">
                        <span class="lv-icon-box"><span class="material-symbols-outlined">draft</span></span>
                        <div><strong>Product requirements</strong><div class="small lv-muted">pdf · workspace docs</div></div>
                      </div>
                      <div class="lv-soft-panel p-3 d-flex align-items-center gap-3">
                        <span class="lv-icon-box"><span class="material-symbols-outlined">code</span></span>
                        <div><strong>Auth snippet</strong><div class="small lv-muted">typescript · reusable</div></div>
                      </div>
                    </div>
                  </main>
                </div>
              </div>
            </div>
          </div>
        </div>
      </header>

      <section class="section-block">
        <div class="container">
          <div class="text-center mb-5">
            <div class="lv-page-kicker">Features</div>
            <h2 class="display-5 fw-bold mb-3">Simple enough to use daily. Structured enough for a team.</h2>
            <p class="lv-muted fs-5 mb-0">The product focuses on clear workspace context instead of hidden settings.</p>
          </div>
          <div class="row g-4">
            <div class="col-md-4" *ngFor="let feature of features">
              <article class="feature-card p-4">
                <span class="feature-icon d-inline-flex align-items-center justify-content-center mb-4">
                  <span class="material-symbols-outlined">{{ feature.icon }}</span>
                </span>
                <h3 class="h5 fw-bold mb-2">{{ feature.title }}</h3>
                <p class="lv-muted mb-0">{{ feature.description }}</p>
              </article>
            </div>
          </div>
        </div>
      </section>

      <section class="section-block border-top bg-light">
        <div class="container">
          <div class="row g-4 align-items-stretch justify-content-center">
            <div class="col-lg-4" *ngFor="let plan of plans">
              <article class="pricing-card p-4 d-flex flex-column">
                <h3 class="fw-bold mb-1">{{ plan.name }}</h3>
                <p class="lv-muted mb-4">{{ plan.description }}</p>
                <div class="display-5 fw-bold mb-4">{{ plan.price }}</div>
                <div class="flex-grow-1">
                  <div class="price-feature" *ngFor="let item of plan.features">
                    <span class="material-symbols-outlined lv-primary" style="font-size:18px">check_circle</span>
                    <span>{{ item }}</span>
                  </div>
                </div>
                <a routerLink="/register" class="btn mt-4" [class.btn-primary]="plan.primary" [class.lv-button-quiet]="!plan.primary">Choose {{ plan.name }}</a>
              </article>
            </div>
          </div>
        </div>
      </section>

      <footer class="border-top py-4 bg-white">
        <div class="container d-flex flex-wrap align-items-center justify-content-between gap-3">
          <span class="fw-bold">LinkVault</span>
          <span class="lv-muted">A focused resource workspace for modern teams.</span>
        </div>
      </footer>
    </div>
  `
})
export class LandingComponent {
  protected features = [
    { icon: 'corporate_fare', title: 'Visible workspaces', description: 'Switch context clearly and manage members without digging through hidden settings.' },
    { icon: 'folder_special', title: 'Vault structure', description: 'Keep resources inside vaults and folders that match real project organization.' },
    { icon: 'notifications', title: 'Invitation center', description: 'Workspace invites appear in the product so teammates can accept them directly.' }
  ];

  protected plans = [
    { name: 'Free', price: '$0', description: 'For personal resource saving.', primary: false, features: ['5 vaults', '3 members', '100 MB storage'] },
    { name: 'Pro', price: '$8', description: 'For serious personal workflows.', primary: true, features: ['50 vaults', '10 members', '5 GB storage'] },
    { name: 'Team', price: '$16', description: 'For collaborative workspaces.', primary: false, features: ['500 vaults', '100 members', '100 GB storage'] }
  ];
}
