import { Component, ViewChild } from '@angular/core';
import { ResourceListComponent } from './resource-list.component';

@Component({
  selector: 'app-resources-page',
  standalone: true,
  imports: [ResourceListComponent],
  template: `
    <section>
      <div class="d-flex flex-column flex-md-row align-items-md-center justify-content-between gap-3 mb-4">
        <div>
          <h1 class="lv-page-title">Resources</h1>
          <p class="lv-muted fs-6 mb-0">Search resources across all vaults.</p>
        </div>
      </div>
      <article class="lv-card p-4">
        <app-resource-list #list title="All resources" />
      </article>
    </section>
  `
})
export class ResourcesPageComponent {
  @ViewChild('list') list!: ResourceListComponent;
}
