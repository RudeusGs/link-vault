import { Component, DestroyRef, OnInit, ViewChild, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';

import { ResourceListComponent } from './resource-list.component';

@Component({
  selector: 'app-resources-page',
  standalone: true,
  imports: [ResourceListComponent],
  template: `
    <section>
      <div class="lv-page-header mb-4">
        <div class="lv-page-header-copy">
          <h1 class="lv-page-title">Resources</h1>
          <p class="lv-muted fs-6 mb-0">Search resources across all vaults.</p>
        </div>
      </div>
      <article class="lv-card p-4">
        <app-resource-list #list title="All resources" [initialKeyword]="keyword" />
      </article>
    </section>
  `
})
export class ResourcesPageComponent implements OnInit {
  @ViewChild('list') list?: ResourceListComponent;

  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);

  protected keyword = (this.route.snapshot.queryParamMap.get('q') ?? '').trim();

  ngOnInit(): void {
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (params) => {
        const keyword = (params.get('q') ?? '').trim();
        if (keyword === this.keyword) {
          return;
        }

        this.keyword = keyword;
        this.list?.setKeyword(keyword);
      }
    });
  }
}
