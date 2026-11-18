import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Subscription } from 'rxjs';
import { RecommendationApiService } from '../../../core/services/recommendation-api.service';
import { ErrorPanel } from '../../../shared/components/error-panel/error-panel';
import { ApiError } from '../../../shared/models/api-error.model';
import { warningLabel } from '../../../shared/utils/warning-labels';
import { CvAnalysisStore } from '../../cv-analysis/cv-analysis.store';
import { RecommendationCard } from '../recommendation-card/recommendation-card';

type Status = 'idle' | 'loading' | 'success' | 'error';

@Component({
  selector: 'app-recommendation-page',
  imports: [RouterLink, ErrorPanel, RecommendationCard],
  templateUrl: './recommendation-page.html',
  styleUrl: './recommendation-page.scss',
})
export class RecommendationPage implements OnInit, OnDestroy {
  private readonly api = inject(RecommendationApiService);
  private readonly store = inject(CvAnalysisStore);
  private sub?: Subscription;

  readonly status = signal<Status>('idle');
  readonly error = signal<ApiError | null>(null);
  readonly topK = signal(10);

  readonly response = this.store.recommendationResponse;
  readonly fileName = computed(() => this.store.file()?.name ?? '');
  readonly warnings = computed(() =>
    (this.response()?.parsingWarnings ?? []).map((code) => warningLabel(code)),
  );

  ngOnInit(): void {
    if (this.response()) this.status.set('success');
    else this.load();
  }

  onTopK(event: Event): void {
    const n = Number((event.target as HTMLInputElement).value);
    this.topK.set(Number.isFinite(n) ? Math.max(1, Math.min(50, Math.round(n))) : 10);
  }

  load(): void {
    const file = this.store.file();
    if (!file || this.status() === 'loading') return; // no duplicate requests
    this.status.set('loading');
    this.error.set(null);

    this.sub = this.api.recommend(file, this.topK()).subscribe({
      next: (r) => {
        this.store.recommendationResponse.set(r);
        this.status.set('success');
      },
      error: (e: ApiError) => {
        this.error.set(e);
        this.status.set('error');
      },
    });
  }

  cancel(): void {
    this.sub?.unsubscribe();
    this.status.set(this.response() ? 'success' : 'idle');
  }

  ngOnDestroy(): void {
    this.sub?.unsubscribe();
  }
}