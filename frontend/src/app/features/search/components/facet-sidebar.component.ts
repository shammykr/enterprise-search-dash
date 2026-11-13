import { Component, computed, effect, input, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { filter } from 'rxjs';
import { FacetBucket, SearchQuery, SearchResult } from '../../../core/models';
import { fromIsoDate, toIsoDate } from '../search-query';

@Component({
  selector: 'app-facet-sidebar',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatDatepickerModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  template: `
    <mat-card appearance="outlined">
      <mat-card-header>
        <mat-card-title>Filters</mat-card-title>
        @if (hasFilters()) {
          <button mat-button class="clear" (click)="clear.emit()">Clear all</button>
        }
      </mat-card-header>
      <mat-card-content>
        <h3>Category</h3>
        @for (bucket of categoryBuckets(); track bucket.value) {
          <mat-checkbox [checked]="query().categories.includes(bucket.value)" (change)="toggleCategory(bucket.value)">
            {{ bucket.value }} <span class="count">({{ bucket.count }})</span>
          </mat-checkbox>
        } @empty {
          <p class="none">No categories</p>
        }

        <h3>Date range</h3>
        <mat-form-field appearance="outline" class="dates" subscriptSizing="dynamic">
          <mat-label>Created between</mat-label>
          <mat-date-range-input [formGroup]="range" [rangePicker]="picker">
            <input matStartDate formControlName="start" placeholder="Start" />
            <input matEndDate formControlName="end" placeholder="End" />
          </mat-date-range-input>
          <mat-datepicker-toggle matIconSuffix [for]="picker" />
          <mat-date-range-picker #picker />
        </mat-form-field>

        <h3>Tags</h3>
        <div class="tags">
          @for (bucket of tagBuckets(); track bucket.value) {
            <mat-checkbox [checked]="query().tags.includes(bucket.value)" (change)="toggleTag(bucket.value)">
              {{ bucket.value }} <span class="count">({{ bucket.count }})</span>
            </mat-checkbox>
          } @empty {
            <p class="none">No tags</p>
          }
        </div>
      </mat-card-content>
    </mat-card>
  `,
  styles: `
    mat-card-header { align-items: center; justify-content: space-between; }
    .clear { margin-left: auto; }
    h3 { margin: 16px 0 4px; font-size: 14px; font-weight: 500; }
    mat-checkbox { display: block; }
    .count { color: var(--mat-sys-on-surface-variant); }
    .dates { width: 100%; }
    .tags { max-height: 320px; overflow-y: auto; }
    .none { color: var(--mat-sys-on-surface-variant); margin: 4px 0; }
  `,
})
export class FacetSidebarComponent {
  readonly query = input.required<SearchQuery>();
  readonly facets = input<SearchResult['facets'] | null>(null);

  readonly categoriesChange = output<string[]>();
  readonly tagsChange = output<string[]>();
  readonly dateRangeChange = output<{ from: string | null; to: string | null }>();
  readonly clear = output<void>();

  readonly range = new FormGroup({
    start: new FormControl<Date | null>(null),
    end: new FormControl<Date | null>(null),
  });

  // Keep selected values visible even if the current result set has no hits for them.
  readonly categoryBuckets = computed(() => withSelected(this.facets()?.category ?? [], this.query().categories));
  readonly tagBuckets = computed(() => withSelected(this.facets()?.tags ?? [], this.query().tags));
  readonly hasFilters = computed(() => {
    const q = this.query();
    return q.categories.length > 0 || q.tags.length > 0 || !!q.from || !!q.to;
  });

  constructor() {
    effect(() => {
      const q = this.query();
      this.range.setValue({ start: fromIsoDate(q.from), end: fromIsoDate(q.to) }, { emitEvent: false });
    });

    // Emit once the range is complete (or fully cleared), not on the first click.
    this.range.valueChanges
      .pipe(
        filter(({ start, end }) => !!end || (!start && !end)),
        takeUntilDestroyed(),
      )
      .subscribe(({ start, end }) =>
        this.dateRangeChange.emit({ from: toIsoDate(start ?? null), to: toIsoDate(end ?? null) }),
      );
  }

  toggleCategory(value: string): void {
    this.categoriesChange.emit(toggle(this.query().categories, value));
  }

  toggleTag(value: string): void {
    this.tagsChange.emit(toggle(this.query().tags, value));
  }
}

function toggle(values: string[], value: string): string[] {
  return values.includes(value) ? values.filter((v) => v !== value) : [...values, value];
}

function withSelected(buckets: FacetBucket[], selected: string[]): FacetBucket[] {
  const missing = selected.filter((s) => !buckets.some((b) => b.value === s)).map((value) => ({ value, count: 0 }));
  return [...buckets, ...missing];
}
