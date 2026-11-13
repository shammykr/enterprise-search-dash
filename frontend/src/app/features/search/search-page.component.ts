import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { catchError, map, of, switchMap, tap } from 'rxjs';
import { SearchApiService } from '../../core/search-api.service';
import { SearchQuery, SearchResult } from '../../core/models';
import { parseQuery, toParams } from './search-query';
import { SearchBarComponent } from './components/search-bar.component';
import { FacetSidebarComponent } from './components/facet-sidebar.component';
import { ResultsListComponent } from './components/results-list.component';

@Component({
  selector: 'app-search-page',
  imports: [MatProgressBarModule, SearchBarComponent, FacetSidebarComponent, ResultsListComponent],
  template: `
    <app-search-bar [query]="query().q" (search)="update({ q: $event })" />
    @if (loading()) {
      <mat-progress-bar mode="indeterminate" />
    }
    @if (error()) {
      <p class="error">{{ error() }}</p>
    }
    <div class="layout">
      <app-facet-sidebar
        [query]="query()"
        [facets]="result()?.facets ?? null"
        (categoriesChange)="update({ categories: $event })"
        (tagsChange)="update({ tags: $event })"
        (dateRangeChange)="update({ from: $event.from, to: $event.to })"
        (clear)="update({ categories: [], tags: [], from: null, to: null })" />
      <app-results-list [result]="result()" [query]="query().q" (pageChange)="update({ page: $event }, false)" />
    </div>
  `,
  styles: `
    .layout {
      display: grid;
      grid-template-columns: 280px minmax(0, 1fr);
      gap: 16px;
      margin-top: 16px;
      align-items: start;
    }
    /* Stack the filters above the results on tablets and phones. */
    @media (max-width: 900px) {
      .layout { grid-template-columns: minmax(0, 1fr); }
    }
    .error { color: var(--mat-sys-error); }
  `,
})
export class SearchPageComponent {
  private readonly api = inject(SearchApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly query = signal<SearchQuery>(parseQuery(this.route.snapshot.queryParamMap));
  readonly result = signal<SearchResult | null>(null);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  constructor() {
    this.route.queryParamMap
      .pipe(
        map(parseQuery),
        tap((q) => {
          this.query.set(q);
          this.loading.set(true);
          this.error.set(null);
        }),
        switchMap((q) =>
          this.api.search(q).pipe(
            catchError(() => {
              this.error.set('Search is unavailable right now. Is the API running?');
              return of(null);
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((result) => {
        this.result.set(result);
        this.loading.set(false);
      });
  }

  /** Any change other than paging resets to the first page. */
  update(change: Partial<SearchQuery>, resetPage = true): void {
    const next: SearchQuery = { ...this.query(), ...change };
    if (resetPage) next.page = 0;
    this.router.navigate([], { relativeTo: this.route, queryParams: toParams(next) });
  }
}
