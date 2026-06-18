import { Component, DestroyRef, OnInit, ViewChild, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';

import { ResourceListComponent } from './resource-list.component';

@Component({
  selector: 'app-resources-page',
  standalone: true,
  imports: [ResourceListComponent],
  template: `    <section>
      <div class="lv-page-header">
        <div class="lv-page-header-copy">
          <p class="lv-page-eyebrow">Resources</p>
          <h1 class="lv-page-title">Search and manage resources</h1>
          <p class="lv-page-subtitle">A focused index for every link, file, note and snippet saved in your workspace.</p>
        </div>
      </div>
      <article class="lv-card p-4">
        <app-resource-list #list title="All resources" [initialKeyword]="keyword" />
      </article>
    </section>  `
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
