import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, MatToolbarModule, MatIconModule],
  template: `
    <mat-toolbar color="primary" class="toolbar">
      <mat-icon>travel_explore</mat-icon>
      <a class="title" routerLink="/search">Enterprise Search</a>
    </mat-toolbar>
    <main class="content">
      <router-outlet />
    </main>
  `,
  styles: `
    .toolbar { position: sticky; top: 0; z-index: 10; gap: 8px; }
    .title { font-weight: 500; color: inherit; text-decoration: none; }
    .content { max-width: 1280px; margin: 0 auto; padding: 16px; }
  `,
})
export class AppComponent {}
