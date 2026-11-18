import { Component, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App {
  readonly menuOpen = signal(false);

  readonly links = [
    { path: '/', label: 'Home', exact: true },
    { path: '/cv', label: 'Upload CV', exact: false },
    { path: '/jobs', label: 'Jobs', exact: false },
    { path: '/recommendations', label: 'Recommendations', exact: false },
  ];

  toggleMenu(): void { this.menuOpen.update((v) => !v); }
  closeMenu(): void { this.menuOpen.set(false); }
}