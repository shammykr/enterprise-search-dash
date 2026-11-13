import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatToolbarModule, MatButtonModule, MatIconModule],
  template: `
    <mat-toolbar color="primary" class="toolbar">
      <mat-icon>travel_explore</mat-icon>
      <span class="title">Enterprise Search</span>
      <span class="spacer"></span>
      <a mat-button routerLink="/search" routerLinkActive="active">Search</a>
      <a mat-button routerLink="/documents" routerLinkActive="active">Documents</a>
    </mat-toolbar>
    <main class="content">
      <router-outlet />
    </main>
  `,
  styles: `
    .toolbar { position: sticky; top: 0; z-index: 10; gap: 8px; }
    .title { font-weight: 500; }
    .spacer { flex: 1; }
    .active { text-decoration: underline; }
    .content { max-width: 1280px; margin: 0 auto; padding: 16px; }
  `,
})
export class AppComponent {}
