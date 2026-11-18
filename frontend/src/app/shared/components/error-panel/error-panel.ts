import { Component, computed, input, output } from '@angular/core';
import { ApiError } from '../../models/api-error.model';
import { friendlyMessage } from '../../utils/error-messages';

@Component({
  selector: 'app-error-panel',
  template: `
    <div class="panel" role="alert">
      <span class="badge badge-danger">{{ error().code }}</span>
      <p class="msg">{{ message() }}</p>
      @if (error().fieldErrors.length) {
        <ul class="fields">
          @for (f of error().fieldErrors; track f.field) {
            <li><strong>{{ f.field }}</strong>: {{ f.message }}</li>
          }
        </ul>
      }
      @if (error().traceId) {
        <p class="trace">Trace ID: {{ error().traceId }}</p>
      }
      @if (retryable()) {
        <button class="btn btn-ghost" type="button" (click)="retry.emit()">Try again</button>
      }
    </div>
  `,
  styles: `
    .panel {
      display: flex; flex-direction: column; align-items: flex-start; gap: 10px;
      padding: 20px; border-radius: var(--radius);
      background: rgba(248, 113, 113, 0.07);
      border: 1px solid rgba(248, 113, 113, 0.3);
    }
    .msg { color: var(--text); }
    .fields { margin: 0; padding-left: 18px; }
    .trace { font-size: 12px; color: var(--text-muted); }
  `,
})
export class ErrorPanel {
  readonly error = input.required<ApiError>();
  readonly retryable = input(false);
  readonly retry = output<void>();

  readonly message = computed(() => friendlyMessage(this.error()));
}