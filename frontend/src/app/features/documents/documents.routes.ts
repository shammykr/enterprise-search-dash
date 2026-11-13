import { Routes } from '@angular/router';
import { DocumentViewComponent } from './document-view.component';

export const DOCUMENT_ROUTES: Routes = [{ path: ':id', component: DocumentViewComponent }];
