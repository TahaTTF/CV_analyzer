import { Component, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, ParamMap, Router, RouterLink } from '@angular/router';
import {
  BehaviorSubject, EMPTY, catchError, combineLatest, debounceTime, merge, switchMap, tap,
} from 'rxjs';
import { JobApiService } from '../../../core/services/job-api.service';
import { ErrorPanel } from '../../../shared/components/error-panel/error-panel';
import { SkillChips } from '../../../shared/components/skill-chips/skill-chips';
import { ApiError } from '../../../shared/models/api-error.model';
import { JobFilters, JobPage, JobSource } from '../../../shared/models/job.model';
import { sourceLabel } from '../../../shared/utils/job-labels';

const SOURCES: JobSource[] = ['ARBEITNOW', 'FRANCE_TRAVAIL', 'ADZUNA', 'LA_BONNE_ALTERNANCE'];
const PAGE_SIZE = 20;

// The URL is the single source of truth for filters and page number.
function filtersFrom(params: ParamMap): JobFilters {
  const page = Number(params.get('page'));
  const source = params.get('source');
  return {
    page: Number.isInteger(page) && page >= 0 ? page : 0,
    size: PAGE_SIZE,
    query: (params.get('query') ?? '').slice(0, 200),
    location: (params.get('location') ?? '').slice(0, 120),
    source: SOURCES.includes(source as JobSource) ? (source as JobSource) : '',
    relevance: 'DEFAULT',
  };
}

@Component({
  selector: 'app-job-search-page',
  imports: [ReactiveFormsModule, RouterLink, ErrorPanel, SkillChips],
  templateUrl: './job-search-page.html',
  styleUrl: './job-search-page.scss',
})
export class JobSearchPage {
  private readonly api = inject(JobApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly refresh$ = new BehaviorSubject(0);

  readonly sources = SOURCES;
  readonly sourceLabel = sourceLabel;

  readonly queryCtrl = new FormControl('', { nonNullable: true });
  readonly locationCtrl = new FormControl('', { nonNullable: true });
  readonly sourceCtrl = new FormControl<JobSource | ''>('', { nonNullable: true });

  readonly status = signal<'loading' | 'success' | 'error'>('loading');
  readonly result = signal<JobPage | null>(null);
  readonly error = signal<ApiError | null>(null);

  constructor() {
    // URL -> form + request. switchMap cancels superseded requests.
    combineLatest([this.route.queryParamMap, this.refresh$])
      .pipe(
        tap(([params]) => {
          const f = filtersFrom(params);
          if (this.queryCtrl.value !== f.query) this.queryCtrl.setValue(f.query ?? '', { emitEvent: false });
          if (this.locationCtrl.value !== f.location) this.locationCtrl.setValue(f.location ?? '', { emitEvent: false });
          if (this.sourceCtrl.value !== f.source) this.sourceCtrl.setValue(f.source ?? '', { emitEvent: false });
          this.status.set('loading');
          this.error.set(null);
        }),
        switchMap(([params]) =>
          this.api.search(filtersFrom(params)).pipe(
            catchError((e: ApiError) => {
              this.error.set(e);
              this.status.set('error');
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(),
      )
      .subscribe((page) => {
        this.result.set(page);
        this.status.set('success');
      });

    // Form -> URL. Text inputs are debounced, the select applies immediately.
    merge(this.queryCtrl.valueChanges, this.locationCtrl.valueChanges)
      .pipe(debounceTime(350), takeUntilDestroyed())
      .subscribe(() => this.applyFilters());
    this.sourceCtrl.valueChanges.pipe(takeUntilDestroyed()).subscribe(() => this.applyFilters());
  }

  private applyFilters(page = 0): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: {
        query: this.queryCtrl.value.trim() || null,
        location: this.locationCtrl.value.trim() || null,
        source: this.sourceCtrl.value || null,
        page: page > 0 ? page : null,
      },
    });
  }

  goTo(page: number): void {
    this.applyFilters(page);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  reset(): void {
    this.router.navigate([], { relativeTo: this.route, queryParams: {} });
  }

  retry(): void {
    this.refresh$.next(this.refresh$.value + 1);
  }
}