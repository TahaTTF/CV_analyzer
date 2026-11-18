import { Component, input } from '@angular/core';

@Component({
  selector: 'app-skill-chips',
  template: `
    @if (skills().length) {
      <ul class="chips" [attr.aria-label]="label()">
        @for (s of skills(); track $index) {
          <li class="chip"
              [class.matched]="variant() === 'matched'"
              [class.missing]="variant() === 'missing'">{{ s }}</li>
        }
      </ul>
    } @else {
      <span class="empty">{{ emptyText() }}</span>
    }
  `,
  styles: `
    .chips { list-style: none; margin: 0; padding: 0; display: flex; flex-wrap: wrap; gap: 8px; }
    .chip.matched { color: #6ee7b7; background: rgba(16, 185, 129, 0.12); border-color: rgba(16, 185, 129, 0.3); }
    .chip.missing { color: #fcd34d; background: rgba(245, 158, 11, 0.1); border-color: rgba(245, 158, 11, 0.3); }
    .empty { color: var(--text-muted); font-style: italic; }
  `,
})
export class SkillChips {
  readonly skills = input<string[]>([]);
  readonly variant = input<'default' | 'matched' | 'missing'>('default');
  readonly label = input('Skills');
  readonly emptyText = input('Not detected');
}