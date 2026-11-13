import { Component, effect, inject, input, output } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatAutocompleteModule, MatAutocompleteSelectedEvent } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { catchError, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';
import { SearchApiService } from '../../../core/search-api.service';

@Component({
  selector: 'app-search-bar',
  imports: [ReactiveFormsModule, MatAutocompleteModule, MatButtonModule, MatFormFieldModule, MatIconModule, MatInputModule],
  template: `
    <form class="bar" (ngSubmit)="submit()">
      <mat-form-field appearance="outline" class="field" subscriptSizing="dynamic">
        <mat-icon matPrefix>search</mat-icon>
        <input
          matInput
          type="search"
          placeholder="Search documents, policies, runbooks…"
          aria-label="Search"
          [formControl]="control"
          [matAutocomplete]="auto" />
        @if (control.value) {
          <button mat-icon-button matSuffix type="button" aria-label="Clear" (click)="clearInput()">
            <mat-icon>close</mat-icon>
          </button>
        }
        <mat-autocomplete #auto="matAutocomplete" (optionSelected)="selected($event)">
          @for (suggestion of suggestions; track suggestion) {
            <mat-option [value]="suggestion">{{ suggestion }}</mat-option>
          }
        </mat-autocomplete>
      </mat-form-field>
      <button mat-flat-button type="submit">Search</button>
    </form>
  `,
  styles: `
    .bar { display: flex; gap: 12px; align-items: center; }
    .field { flex: 1; }
  `,
})
export class SearchBarComponent {
  private readonly api = inject(SearchApiService);

  readonly query = input('');
  readonly search = output<string>();

  readonly control = new FormControl('', { nonNullable: true });
  suggestions: string[] = [];

  constructor() {
    // Keep the box in sync when the query changes via the URL (back/forward, links).
    effect(() => this.control.setValue(this.query(), { emitEvent: false }));

    this.control.valueChanges
      .pipe(
        debounceTime(200),
        distinctUntilChanged(),
        switchMap((text) =>
          text.trim().length < 2 ? of([]) : this.api.autocomplete(text.trim()).pipe(catchError(() => of([]))),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((s) => (this.suggestions = s));
  }

  submit(): void {
    this.suggestions = [];
    this.search.emit(this.control.value.trim());
  }

  selected(event: MatAutocompleteSelectedEvent): void {
    this.control.setValue(event.option.value, { emitEvent: false });
    this.submit();
  }

  clearInput(): void {
    this.control.setValue('');
    this.submit();
  }
}
