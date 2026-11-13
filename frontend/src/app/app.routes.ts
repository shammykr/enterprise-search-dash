import { Routes } from '@angular/router';

// Each feature is a separately bundled, lazily loaded route module.
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'search' },
  { path: 'search', loadChildren: () => import('./features/search/search.routes').then((m) => m.SEARCH_ROUTES) },
  {
    path: 'documents',
    loadChildren: () => import('./features/documents/documents.routes').then((m) => m.DOCUMENT_ROUTES),
  },
  { path: '**', redirectTo: 'search' },
];
