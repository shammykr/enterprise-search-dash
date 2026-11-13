import { Routes } from '@angular/router';
import { provideNativeDateAdapter } from '@angular/material/core';
import { SearchPageComponent } from './search-page.component';

export const SEARCH_ROUTES: Routes = [
  { path: '', component: SearchPageComponent, providers: [provideNativeDateAdapter()] },
];
