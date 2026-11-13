import { DatePipe, Location } from '@angular/common';
import { Component, inject, input } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { catchError, of, switchMap } from 'rxjs';
import { EnterpriseDocument } from '../../core/models';
import { SearchApiService } from '../../core/search-api.service';

/** Full view of a single document, loaded from MongoDB via the CRUD API. */
@Component({
  selector: 'app-document-view',
  imports: [DatePipe, MatButtonModule, MatCardModule, MatChipsModule, MatIconModule],
  template: `
    <button mat-button (click)="back()"><mat-icon>arrow_back</mat-icon> Back to results</button>
    @if (doc(); as d) {
      <mat-card appearance="outlined">
        <mat-card-header>
          <mat-card-title>{{ d.title }}</mat-card-title>
          <mat-card-subtitle>
            {{ d.category }} · {{ d.author }} · created {{ d.createdAt | date: 'medium' }}
          </mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <p class="content">{{ d.content }}</p>
          <mat-chip-set>
            @for (tag of d.tags; track tag) {
              <mat-chip>{{ tag }}</mat-chip>
            }
          </mat-chip-set>
        </mat-card-content>
      </mat-card>
    } @else if (doc() === null) {
      <p>Document not found.</p>
    }
  `,
  styles: `
    mat-card { margin-top: 8px; }
    .content { white-space: pre-wrap; line-height: 1.6; margin: 16px 0; }
  `,
})
export class DocumentViewComponent {
  private readonly api = inject(SearchApiService);
  private readonly location = inject(Location);

  /** Bound from the :id route param (withComponentInputBinding). */
  readonly id = input.required<string>();

  /** undefined = loading, null = not found. */
  readonly doc = toSignal<EnterpriseDocument | null | undefined>(
    toObservable(this.id).pipe(
      switchMap((id) => this.api.getDocument(id).pipe(catchError(() => of(null)))),
    ),
    { initialValue: undefined },
  );

  back(): void {
    this.location.back();
  }
}
