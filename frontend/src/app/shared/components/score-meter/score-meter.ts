import { Component, computed, input } from '@angular/core';

@Component({
  selector: 'app-score-meter',
  template: `
    <div class="meter" role="meter" aria-valuemin="0" aria-valuemax="100"
         [attr.aria-valuenow]="rounded()" [attr.aria-label]="label()">
      <div class="fill" [style.width.%]="clamped()"></div>
    </div>
  `,
  styles: `
    .meter { height: 8px; border-radius: 999px; background: var(--surface); border: 1px solid var(--border); overflow: hidden; }
    .fill { height: 100%; border-radius: 999px; background: var(--gradient-primary);
            box-shadow: 0 0 12px rgba(124, 58, 237, 0.6); transition: width .4s ease; }
  `,
})
export class ScoreMeter {
  readonly value = input.required<number>();
  readonly label = input('Score');

  readonly clamped = computed(() => Math.max(0, Math.min(100, this.value())));
  readonly rounded = computed(() => Math.round(this.clamped()));
}