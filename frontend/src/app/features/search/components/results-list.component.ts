import { DatePipe } from '@angular/common';
import { Component, input, output } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { SearchResult } from '../../../core/models';

@Component({
  selector: 'app-results-list',
  imports: [DatePipe, RouterLink, MatCardModule, MatChipsModule, MatPaginatorModule],
  template: `
    @if (result(); as r) {
      <p class="summary">
        {{ r.total }} result{{ r.total === 1 ? '' : 's' }}
        @if (query()) { for <strong>“{{ query() }}”</strong> }
        <span class="took">({{ r.tookMs }} ms)</span>
      </p>

      @for (hit of r.hits; track hit.id) {
        <mat-card appearance="outlined" class="hit">
          <mat-card-header>
            <mat-card-title>
              <a [routerLink]="['/documents', hit.id]" [innerHTML]="hit.title"></a>
            </mat-card-title>
            <mat-card-subtitle>
              {{ hit.category }} · {{ hit.author }} · {{ hit.createdAt | date: 'mediumDate' }}
            </mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <p class="snippet" [innerHTML]="hit.snippet"></p>
            <mat-chip-set aria-label="Tags">
              @for (tag of hit.tags; track tag) {
                <mat-chip>{{ tag }}</mat-chip>
              }
            </mat-chip-set>
          </mat-card-content>
        </mat-card>
      } @empty {
        <p class="empty">No documents match your search. Try fewer filters or a different term.</p>
      }

      @if (r.total > r.size) {
        <mat-paginator
          [length]="r.total"
          [pageIndex]="r.page"
          [pageSize]="r.size"
          [hidePageSize]="true"
          (page)="onPage($event)" />
      }
    }
  `,
  styles: `
    .summary { margin: 0 0 12px; color: var(--mat-sys-on-surface-variant); }
    .took { font-size: 12px; }
    .hit { margin-bottom: 12px; }
    .hit a { color: var(--mat-sys-primary); text-decoration: none; }
    .hit a:hover { text-decoration: underline; }
    .snippet { margin: 8px 0; line-height: 1.5; }
    :host ::ng-deep mark { background: #fff2a8; padding: 0 1px; }
    .empty { padding: 32px 0; text-align: center; color: var(--mat-sys-on-surface-variant); }
  `,
})
export class ResultsListComponent {
  readonly result = input<SearchResult | null>(null);
  readonly query = input('');
  readonly pageChange = output<number>();

  onPage(event: PageEvent): void {
    this.pageChange.emit(event.pageIndex);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
}
