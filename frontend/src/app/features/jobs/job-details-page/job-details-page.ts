import { DatePipe, Location } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { BehaviorSubject, EMPTY, catchError, combineLatest, switchMap, tap } from 'rxjs';
import { JobApiService } from '../../../core/services/job-api.service';
import { ErrorPanel } from '../../../shared/components/error-panel/error-panel';
import { SkillChips } from '../../../shared/components/skill-chips/skill-chips';
import { ApiError } from '../../../shared/models/api-error.model';
import { JobDetails } from '../../../shared/models/job.model';
import { isSafeHttpUrl } from '../../../shared/utils/file-validation';
import { humanize, sourceLabel, workModeLabel } from '../../../shared/utils/job-labels';
import { toPlainText } from '../../../shared/utils/text';

@Component({
  selector: 'app-job-details-page',
  imports: [DatePipe, ErrorPanel, SkillChips],
  templateUrl: './job-details-page.html',
  styleUrl: './job-details-page.scss',
})
export class JobDetailsPage {
  private readonly api = inject(JobApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly location = inject(Location);
  private readonly refresh$ = new BehaviorSubject(0);

  readonly sourceLabel = sourceLabel;

  readonly status = signal<'loading' | 'success' | 'error'>('loading');
  readonly job = signal<JobDetails | null>(null);
  readonly error = signal<ApiError | null>(null);

  // Descriptions are rendered as plain text only, never as HTML.
  readonly description = computed(() => {
    const j = this.job();
    return toPlainText(j?.descriptionText ?? j?.description);
  });
  readonly applyUrl = computed(() => {
    const url = this.job()?.url;
    return isSafeHttpUrl(url) ? url : null;
  });
  readonly workMode = computed(() => {
    const j = this.job();
    return workModeLabel(j?.normalizedLocation?.workMode) || (j?.remote ? 'Remote' : '');
  });
  readonly employmentTypes = computed(() => (this.job()?.employmentTypes ?? []).map(humanize));
  readonly requiredSkills = computed(() => (this.job()?.extractedSkills ?? []).map((s) => s.name));
  readonly learnedSkills = computed(() => (this.job()?.toBeAcquiredSkills ?? []).map((s) => s.name));

  constructor() {
    combineLatest([this.route.paramMap, this.refresh$])
      .pipe(
        tap(() => {
          this.status.set('loading');
          this.error.set(null);
        }),
        switchMap(([params]) => {
          const id = params.get('jobId') ?? '';
          if (id.length < 1 || id.length > 100) {
            this.fail({
              timestamp: new Date().toISOString(), status: 404, code: 'RESOURCE_NOT_FOUND',
              message: 'Invalid job identifier.', path: this.router.url, fieldErrors: [], traceId: null,
            });
            return EMPTY;
          }
          return this.api.getById(id).pipe(
            catchError((e: ApiError) => {
              this.fail(e);
              return EMPTY;
            }),
          );
        }),
        takeUntilDestroyed(),
      )
      .subscribe((job) => {
        this.job.set(job);
        this.status.set('success');
      });
  }

  private fail(e: ApiError): void {
    this.error.set(e);
    this.status.set('error');
  }

  back(): void {
    if (window.history.length > 1) this.location.back();
    else this.router.navigate(['/jobs']);
  }

  retry(): void {
    this.refresh$.next(this.refresh$.value + 1);
  }
}